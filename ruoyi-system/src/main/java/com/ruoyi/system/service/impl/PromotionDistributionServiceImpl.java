package com.ruoyi.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.CouponBo;
import com.ruoyi.system.domain.bo.PromotionAttributionBo;
import com.ruoyi.system.domain.bo.PromotionCodeBo;
import com.ruoyi.system.domain.vo.*;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IPromotionDistributionService;
import com.ruoyi.system.service.wx.IWxMiniappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推广分销服务实现（T16.5/T16.6/T16.10/T16.11，小微版 M16）
 * <p>★全部接口由 Controller 层 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION") 统一校验版本。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionDistributionServiceImpl implements IPromotionDistributionService {

    private final PromotionCodeMapper codeMapper;
    private final PromotionAttributionMapper attrMapper;
    private final CouponMapper couponMapper;
    private final CouponRecordMapper couponRecordMapper;
    private final PromotionRewardRecordMapper rewardRecordMapper;
    private final SysUserMapper userMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final IWxMiniappService wxMiniappService;

    /** H5 推广码基础 URL（二维码指向的登录页地址） */
    @Value("${promo.h5-base-url:https://xiaozhangben.com.cn/h5/#/pages/login/index}")
    private String h5BaseUrl;

    // ===== T16.5 推广码 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionCodeVo generateCode(String codeType) {
        // ★fail-fast：入口判空
        if (StrUtil.isBlank(codeType)) {
            throw new ServiceException("推广码类型不能为空");
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("当前租户上下文缺失，无法生成推广码");
        }
        // 一人一码：先查是否已有
        LambdaQueryWrapper<PromotionCode> existWrapper = Wrappers.<PromotionCode>lambdaQuery()
            .eq(PromotionCode::getUserId, userId)
            .eq(PromotionCode::getCodeType, codeType);
        PromotionCode exist = codeMapper.selectOne(existWrapper);
        if (exist != null) {
            // ★存量码 scene_param 可能含 t=null 等脏数据，重新生成时强制刷新
            String roleKey = resolveRoleKey();
            String userName = LoginHelper.getUsername();
            String newScene;
            if ("store".equals(codeType)) {
                newScene = "t=" + tenantId;
            } else {
                newScene = "t=" + tenantId + "&u=" + (StrUtil.isNotBlank(userName) ? userName : userId)
                    + "&r=" + (roleKey != null ? roleKey : "u");
                if (newScene.length() > 32) {
                    newScene = "t=" + tenantId + "&u=" + userId + "&r=" + (roleKey != null ? roleKey : "u");
                }
            }
            if (newScene.length() > 32) {
                newScene = newScene.substring(0, 32);
            }
            if (!newScene.equals(exist.getSceneParam())) {
                exist.setSceneParam(newScene);
                exist.setStatus("0");
                codeMapper.updateById(exist);
                log.info("[generateCode] 存量推广码 scene 刷新: userId={}, old={}, new={}", userId, exist.getSceneParam(), newScene);
            }
            return BeanCopyUtils.copy(exist, PromotionCodeVo.class);
        }
        // 首次生成：构造 scene_param（V3.7：微信二维码含 租户+人员+角色）
        String roleKey = resolveRoleKey();
        String sceneParam;
        if ("store".equals(codeType)) {
            sceneParam = "t=" + tenantId;
        } else {
            // u=用户名（如 lijie）；scene 超 32 字符（微信 scene 上限）时回退 u=用户ID
            String userName = LoginHelper.getUsername();
            sceneParam = "t=" + tenantId + "&u=" + (StrUtil.isNotBlank(userName) ? userName : userId)
                + "&r=" + (roleKey != null ? roleKey : "u");
            if (sceneParam.length() > 32) {
                sceneParam = "t=" + tenantId + "&u=" + userId + "&r=" + (roleKey != null ? roleKey : "u");
            }
        }
        // scene_param ≤ 32 字符（微信小程序码 scene 限制）
        if (sceneParam.length() > 32) {
            sceneParam = sceneParam.substring(0, 32);
        }
        PromotionCode code = new PromotionCode();
        code.setUserId(userId);
        code.setRoleCode(resolveRoleKey());
        code.setCodeType(codeType);
        code.setSceneParam(sceneParam);
        code.setStatus("0");
        codeMapper.insert(code);
        return BeanCopyUtils.copy(code, PromotionCodeVo.class);
    }

    @Override
    public TableDataInfo<PromotionCodeVo> queryCodePage(PromotionCodeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionCode> wrapper = Wrappers.<PromotionCode>lambdaQuery()
            .eq(bo.getUserId() != null, PromotionCode::getUserId, bo.getUserId())
            .eq(StrUtil.isNotBlank(bo.getRoleCode()), PromotionCode::getRoleCode, bo.getRoleCode())
            .eq(StrUtil.isNotBlank(bo.getCodeType()), PromotionCode::getCodeType, bo.getCodeType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PromotionCode::getStatus, bo.getStatus())
            .orderByDesc(PromotionCode::getCreateTime);
        Page<PromotionCodeVo> page = codeMapper.selectVoPage(pageQuery.build(), wrapper);
        fillUserNames(page.getRecords(), true);
        return TableDataInfo.build(page);
    }

    @Override
    public int toggleCode(Long id, String status) {
        // ★fail-fast：入口判空
        if (id == null) {
            throw new ServiceException("推广码ID不能为空");
        }
        if (StrUtil.isBlank(status)) {
            throw new ServiceException("状态不能为空");
        }
        PromotionCode exist = codeMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("推广码不存在");
        }
        LambdaUpdateWrapper<PromotionCode> wrapper = Wrappers.<PromotionCode>lambdaUpdate()
            .eq(PromotionCode::getId, id)
            .set(PromotionCode::getStatus, status);
        return codeMapper.update(null, wrapper);
    }

    // ===== T16.6 扫码归因 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanAttribution(String sceneParam, Long customerUserId, String customerPhone) {
        // ★fail-fast：入口判空
        if (StrUtil.isBlank(sceneParam)) {
            return;
        }
        if (customerUserId == null) {
            log.warn("[scanAttribution] customerUserId 为空，跳过归因");
            return;
        }
        // 解析 scene_param → tenantId, roleCode, 推广人（u=用户名，兼容存量 u=用户ID）
        Map<String, String> params = parseSceneParam(sceneParam);
        String promoterRoleCode = params.get("r");
        String uParam = params.get("u");
        if (StrUtil.isBlank(uParam)) {
            // 门店码（无 u 参数）不归因到个人
            return;
        }
        Long promoterUserId = null;
        if (uParam.chars().allMatch(Character::isDigit)) {
            // 存量数字格式：u=用户ID
            try {
                promoterUserId = Long.valueOf(uParam);
            } catch (NumberFormatException e) {
                log.warn("[scanAttribution] 解析推广人用户ID失败: scene={}", sceneParam);
                return;
            }
        } else {
            // V3.7 新格式：u=用户名（sys_user 在 IGNORE_TENANT_TABLES，需显式限定租户范围）
            Long sceneTenantId = TenantHelper.getTenantId();
            try {
                sceneTenantId = params.get("t") != null ? Long.valueOf(params.get("t")) : sceneTenantId;
            } catch (NumberFormatException e) {
                log.warn("[scanAttribution] 解析 scene 租户ID失败: scene={}", sceneParam);
            }
            if (sceneTenantId == null) {
                log.warn("[scanAttribution] 无法确定租户，跳过归因: scene={}", sceneParam);
                return;
            }
            SysUser promoter = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUserName, uParam)
                .eq(SysUser::getTenantId, sceneTenantId)
                .last("LIMIT 1"));
            if (promoter == null) {
                log.warn("[scanAttribution] 用户名未命中推广人: scene={}", sceneParam);
                return;
            }
            promoterUserId = promoter.getUserId();
        }
        // 推广人用户ID缺失不归因
        if (promoterUserId == null) {
            return;
        }
        // 检查推广人状态：禁用则不更新归因
        LambdaQueryWrapper<PromotionCode> codeWrapper = Wrappers.<PromotionCode>lambdaQuery()
            .eq(PromotionCode::getUserId, promoterUserId)
            .eq(PromotionCode::getCodeType, "personal");
        PromotionCode code = codeMapper.selectOne(codeWrapper);
        if (code != null && "1".equals(code.getStatus())) {
            log.info("[scanAttribution] 推广人 {} 已禁用，不更新归因", promoterUserId);
            return;
        }
        // upsert promotion_attribution：UNIQUE(tenant_id, customer_user_id)
        LambdaQueryWrapper<PromotionAttribution> existWrapper = Wrappers.<PromotionAttribution>lambdaQuery()
            .eq(PromotionAttribution::getCustomerUserId, customerUserId);
        PromotionAttribution exist = attrMapper.selectOne(existWrapper);
        if (exist == null) {
            PromotionAttribution attr = new PromotionAttribution();
            attr.setCustomerUserId(customerUserId);
            attr.setCustomerPhone(customerPhone);
            attr.setPromoterUserId(promoterUserId);
            attr.setPromoterRoleCode(promoterRoleCode);
            attr.setLastScanTime(new Date());
            attr.setStatus("0");
            attrMapper.insert(attr);
        } else {
            // 更新归因：谁最后扫码业绩归谁（交接点）
            exist.setPromoterUserId(promoterUserId);
            exist.setPromoterRoleCode(promoterRoleCode);
            exist.setLastScanTime(new Date());
            exist.setCustomerPhone(customerPhone);
            attrMapper.updateById(exist);
        }
    }

    @Override
    public TableDataInfo<PromotionAttributionVo> queryAttributionPage(PromotionAttributionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionAttribution> wrapper = Wrappers.<PromotionAttribution>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getCustomerPhone()), PromotionAttribution::getCustomerPhone, bo.getCustomerPhone())
            .eq(bo.getCustomerUserId() != null, PromotionAttribution::getCustomerUserId, bo.getCustomerUserId())
            .eq(bo.getPromoterUserId() != null, PromotionAttribution::getPromoterUserId, bo.getPromoterUserId())
            .eq(StrUtil.isNotBlank(bo.getPromoterRoleCode()), PromotionAttribution::getPromoterRoleCode, bo.getPromoterRoleCode())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PromotionAttribution::getStatus, bo.getStatus())
            .orderByDesc(PromotionAttribution::getLastScanTime);
        Page<PromotionAttributionVo> page = attrMapper.selectVoPage(pageQuery.build(), wrapper);
        // 回填推广人昵称
        for (PromotionAttributionVo vo : page.getRecords()) {
            if (vo.getPromoterUserId() != null) {
                SysUser user = userMapper.selectById(vo.getPromoterUserId());
                if (user != null) {
                    vo.setPromoterName(user.getNickName());
                }
            }
        }
        return TableDataInfo.build(page);
    }

    // ===== T16.10 老板后台推广统计 =====

    @Override
    public List<Map<String, Object>> queryPromotionStat(String period) {
        // 按推广人汇总：带来人数/已消费/拉新成功数
        LambdaQueryWrapper<PromotionAttribution> wrapper = Wrappers.<PromotionAttribution>lambdaQuery()
            .isNotNull(PromotionAttribution::getPromoterUserId);
        List<PromotionAttribution> allAttrs = attrMapper.selectList(wrapper);
        // ★DB 返回保护
        if (allAttrs == null) {
            allAttrs = Collections.emptyList();
        }
        Map<Long, List<PromotionAttribution>> grouped = allAttrs.stream()
            .filter(a -> a.getPromoterUserId() != null)
            .collect(Collectors.groupingBy(PromotionAttribution::getPromoterUserId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<PromotionAttribution>> entry : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("promoterUserId", entry.getKey());
            SysUser user = userMapper.selectById(entry.getKey());
            row.put("promoterName", user != null ? user.getNickName() : "-");
            row.put("broughtCount", entry.getValue().size());
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> queryPerformanceStat(String period) {
        // 简化实现：返回推广归因总数、奖励发放总数等汇总
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAttribution", attrMapper.selectCount(Wrappers.<PromotionAttribution>lambdaQuery()
            .isNotNull(PromotionAttribution::getPromoterUserId)));
        summary.put("totalReward", rewardRecordMapper.selectCount(null));
        result.add(summary);
        return result;
    }

    // ===== T16.11 我的推广码 =====

    @Override
    public Map<String, Object> myPromotion() {
        Long userId = LoginHelper.getUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        // V3.7：回传用户名/角色，供小程序「我的推广码」展示 租户+人员+角色
        result.put("userName", LoginHelper.getUsername());
        result.put("roleKey", resolveRoleKey());
        // 我的推广码
        LambdaQueryWrapper<PromotionCode> codeWrapper = Wrappers.<PromotionCode>lambdaQuery()
            .eq(PromotionCode::getUserId, userId)
            .eq(PromotionCode::getCodeType, "personal");
        PromotionCode myCode = codeMapper.selectOne(codeWrapper);
        result.put("code", myCode != null ? BeanCopyUtils.copy(myCode, PromotionCodeVo.class) : null);
        // 我的推广数据（带来人数）
        Long broughtCount = attrMapper.selectCount(Wrappers.<PromotionAttribution>lambdaQuery()
            .eq(PromotionAttribution::getPromoterUserId, userId));
        result.put("broughtCount", broughtCount);

        // ★T18.16 邀请好友列表（promotion_attribution 按推广人查，回填顾客昵称 + 是否已消费）
        List<PromotionAttribution> attrList = attrMapper.selectList(
            Wrappers.<PromotionAttribution>lambdaQuery()
                .eq(PromotionAttribution::getPromoterUserId, userId)
                .orderByDesc(PromotionAttribution::getLastScanTime));
        long consumedCount = 0;
        List<Map<String, Object>> friends = new ArrayList<>();
        for (PromotionAttribution attr : attrList) {
            Map<String, Object> friend = new LinkedHashMap<>();
            friend.put("customerUserId", attr.getCustomerUserId());
            friend.put("customerPhone", attr.getCustomerPhone());
            friend.put("lastScanTime", attr.getLastScanTime());
            // 回填顾客昵称
            String nickName = null;
            if (attr.getCustomerUserId() != null) {
                SysUser customer = userMapper.selectById(attr.getCustomerUserId());
                if (customer != null) {
                    nickName = customer.getNickName();
                }
            }
            friend.put("nickName", nickName != null ? nickName : "顾客");
            // 判断是否已消费（有已完成订单即算消费）
            boolean consumed = false;
            if (attr.getCustomerUserId() != null) {
                Long orderCount = bookingOrderMapper.selectCount(
                    Wrappers.<BookingOrder>lambdaQuery()
                        .eq(BookingOrder::getCustomerUserId, attr.getCustomerUserId())
                        .eq(BookingOrder::getOrderStatus, "COMPLETED"));
                consumed = orderCount != null && orderCount > 0;
            }
            friend.put("consumed", consumed);
            if (consumed) {
                consumedCount++;
            }
            friends.add(friend);
        }
        result.put("friends", friends);
        result.put("consumedCount", consumedCount);

        // 我的奖励（现金/优惠券）
        String roleKey = resolveRoleKey();
        if ("salesman".equals(roleKey) || "boss".equals(roleKey)) {
            // 员工：现金奖励汇总
            List<PromotionRewardRecord> records = rewardRecordMapper.selectList(
                Wrappers.<PromotionRewardRecord>lambdaQuery()
                    .eq(PromotionRewardRecord::getPromoterUserId, userId));
            result.put("rewardRecords", records);
        } else {
            // 顾客：优惠券列表
            result.put("myCoupons", myCoupons());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateMyQrcode() {
        // 生成/复用个人推广码（一人一码，scene 落库持久化）
        PromotionCodeVo code = generateCode("personal");
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("当前租户上下文缺失，无法生成推广码");
        }
        String userName = LoginHelper.getUsername();
        String roleKey = resolveRoleKey();
        Long userId = LoginHelper.getUserId();
        // H5 推广链接：带租户ID + 用户ID（与微信 scene 参数一致，只是 URL 形式）
        String h5Url = h5BaseUrl + "?ref_t=" + tenantId + "&ref_u=" + userId;
        Map<String, Object> result = new LinkedHashMap<>(8);
        result.put("scene", code != null ? code.getSceneParam() : "");
        result.put("userName", userName);
        result.put("roleKey", roleKey);
        result.put("tenantId", tenantId);
        // 用 ZXing 生成 H5 URL 的二维码（base64 data URL，前端可直接渲染）
        String qrcodeUrl = generateH5Qrcode(h5Url);
        result.put("qrcodeUrl", qrcodeUrl);
        result.put("h5Url", h5Url);
        return result;
    }

    /**
     * ★H5 推广码二维码生成（ZXing，替代原微信小程序码）
     * <p>顾客手机浏览器扫码 → 打开 H5 登录页（带 ref_t/ref_u 参数）→ 登录后自动归因。
     *
     * @param content 二维码内容（H5 登录页 URL）
     * @return base64 data URL（data:image/png;base64,...）
     */
    private String generateH5Qrcode(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 430, 430, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:image/png;base64," + base64;
            log.info("[generateH5Qrcode] H5推广码生成成功 url={} size={}KB", content, bytes.length / 1024);
            return dataUrl;
        } catch (Exception e) {
            log.warn("[generateH5Qrcode] H5推广码生成失败: url={}, 原因={}", content, e.getMessage());
            return null;
        }
    }

    // ===== T16.3 优惠券管理 =====

    @Override
    public TableDataInfo<CouponVo> queryCouponPage(CouponBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getName()), Coupon::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getType()), Coupon::getType, bo.getType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), Coupon::getStatus, bo.getStatus())
            .orderByDesc(Coupon::getCreateTime);
        Page<CouponVo> page = couponMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public CouponVo queryCouponById(Long id) {
        return couponMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertCoupon(CouponBo bo) {
        // ★fail-fast：入口判空
        if (bo == null) {
            throw new ServiceException("优惠券参数不能为空");
        }
        if (StrUtil.isBlank(bo.getName())) {
            throw new ServiceException("优惠券名称不能为空");
        }
        Coupon coupon = new Coupon();
        coupon.setName(bo.getName());
        coupon.setType(bo.getType());
        coupon.setValue(bo.getValue());
        coupon.setValidDays(bo.getValidDays() != null ? bo.getValidDays() : 30);
        coupon.setTotal(bo.getTotal());
        coupon.setIssuedCount(0);
        coupon.setStatus("0");
        coupon.setRemark(bo.getRemark());
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCoupon(CouponBo bo) {
        // ★fail-fast：入口判空
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("优惠券ID不能为空");
        }
        Coupon exist = couponMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("优惠券不存在");
        }
        Coupon update = new Coupon();
        update.setId(bo.getId());
        update.setName(bo.getName());
        update.setType(bo.getType());
        update.setValue(bo.getValue());
        if (bo.getValidDays() != null) {
            update.setValidDays(bo.getValidDays());
        }
        update.setTotal(bo.getTotal());
        update.setStatus(bo.getStatus());
        update.setRemark(bo.getRemark());
        return couponMapper.updateById(update);
    }

    @Override
    public int deleteCoupon(Long[] ids) {
        return couponMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public List<CouponRecordVo> myCoupons() {
        Long userId = LoginHelper.getUserId();
        LambdaQueryWrapper<CouponRecord> wrapper = Wrappers.<CouponRecord>lambdaQuery()
            .eq(CouponRecord::getUserId, userId)
            .orderByDesc(CouponRecord::getReceiveTime);
        List<CouponRecord> records = couponRecordMapper.selectList(wrapper);
        List<CouponRecordVo> vos = couponRecordMapper.selectVoList(wrapper);
        // 回填优惠券名称
        for (CouponRecordVo vo : vos) {
            Coupon coupon = couponMapper.selectById(vo.getCouponId());
            if (coupon != null) {
                vo.setCouponName(coupon.getName());
            }
        }
        return vos;
    }

    // ===== 私有方法 =====

    private Map<String, String> parseSceneParam(String sceneParam) {
        Map<String, String> result = new HashMap<>();
        if (StrUtil.isBlank(sceneParam)) {
            return result;
        }
        String[] pairs = sceneParam.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    /**
     * 从当前登录用户 roles 列表取第一个 roleKey
     */
    private String resolveRoleKey() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null && loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
                RoleDTO first = loginUser.getRoles().get(0);
                return first.getRoleKey();
            }
        } catch (Exception e) {
            log.warn("[resolveRoleKey] 获取角色失败", e);
        }
        return null;
    }

    private void fillUserNames(List<PromotionCodeVo> records, boolean fillUserName) {
        if (!fillUserName) {
            return;
        }
        for (PromotionCodeVo vo : records) {
            if (vo.getUserId() != null) {
                SysUser user = userMapper.selectById(vo.getUserId());
                if (user != null) {
                    vo.setUserName(user.getNickName());
                }
            }
        }
    }
}
