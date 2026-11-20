package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.OssService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.annotation.SeatOperation;
import com.ruoyi.system.domain.ActivityLockRequest;
import com.ruoyi.system.domain.ActivitySeatAssignment;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectSignup;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.bo.PromotionActivityBo;
import com.ruoyi.system.domain.enums.ProjectMemberRole;
import com.ruoyi.system.domain.enums.SeatOperationType;
import com.ruoyi.system.domain.vo.MyActivityVo;
import com.ruoyi.system.domain.vo.PromotionActivityVo;
import com.ruoyi.system.mapper.ActivitySeatAssignmentMapper;
import com.ruoyi.system.mapper.ActivityLockRequestMapper;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.mapper.ProjectSignupMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.ServiceCategoryMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IProjectSignupService;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.IMicroSelfBookingService;
import com.ruoyi.system.service.IStaffPricingService;
import com.ruoyi.system.service.IUserTenantRoleService;
import com.ruoyi.system.service.IVoiceComplianceService;
import com.ruoyi.system.service.strategy.TeamContext;
import com.ruoyi.system.service.strategy.TeamResult;
import com.ruoyi.system.service.strategy.TeamStrategyDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 活动/预告/海报服务实现（T15.28，小微版 lite 档活动浏览页数据源）
 * <p>promotion_activity 为租户级表，多租户插件自动隔离；老板 PC 管理走登录上下文，
 * 活动浏览页匿名走 {@link TenantHelper#supply} 显式限定 tenantId，仅返回 status=PUBLISHED 的公开活动。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionActivityServiceImpl implements IPromotionActivityService {

    private final PromotionActivityMapper activityMapper;

    /** ★M28 T28.1 活动锁定/解锁时全量刷 activity_seat_assignment.locked */
    private final ActivitySeatAssignmentMapper seatAssignmentMapper;

    /** ★M33 §九 员工申请锁定 → 详情回显 lockRequestPending（直查 Mapper 避免与 IActivityLockRequestService 循环依赖） */
    private final ActivityLockRequestMapper lockRequestMapper;

    /** OSS 服务：ossId → 可渲染 URL 翻译（框架既有能力，列表/详情/匿名浏览统一在服务端翻译） */
    private final OssService ossService;

    /** M20 T18 增强：语音合规校验服务（推广发布页 insert/update 触发） */
    private final IVoiceComplianceService voiceComplianceService;

    /** ★报名详情聚合：关联服务项目/报名/成员/用户（只读） */
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceCategoryMapper serviceCategoryMapper;
    private final ProjectSignupMapper signupMapper;
    private final ProjectMemberMapper memberMapper;
    private final SysUserMapper sysUserMapper;

    /** ★报名详情页指定房间名解析（selected_room_ids → service_item） */

    /** ★成团目标实时判定（业态策略分发）与顾客自助报名委派 */
    private final TeamStrategyDispatcher teamStrategyDispatcher;
    private final IProjectSignupService projectSignupService;

    /** ★活动结束/取消时释放关联的房间和助教资源 */
    private final IMicroSelfBookingService selfBookingService;

    /** ★M22 助教小时价格查询（自动计算活动总价用） */
    private final IStaffPricingService staffPricingService;

    /** ★M22 顾客/助教活动合并列表（booking_order 查询） */
    private final BookingOrderMapper bookingOrderMapper;

    /** ★员工身份标签：租户角色映射动态显示名（司机/导游/助教），替代硬编码「助教」 */
    private final IUserTenantRoleService userTenantRoleService;

    /** ★V3.9 §一.3（T26.7 补遗）：房间级背景图防御式查询（room_seat_layout 表可能未落地） */
    private final JdbcTemplate jdbcTemplate;

    /** 活动状态：草稿 */
    private static final String STATUS_DRAFT = "DRAFT";
    /** 活动状态：已发布 */
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    /** 活动状态：已结束 */
    private static final String STATUS_ENDED = "ENDED";

    @Override
    public TableDataInfo<PromotionActivityVo> queryPageList(PromotionActivityBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionActivity> wrapper = Wrappers.<PromotionActivity>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getTitle()), PromotionActivity::getTitle, bo.getTitle())
            .eq(StrUtil.isNotBlank(bo.getActivityType()), PromotionActivity::getActivityType, bo.getActivityType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PromotionActivity::getStatus, bo.getStatus())
            .orderByDesc(PromotionActivity::getCreateTime);
        Page<PromotionActivityVo> page = activityMapper.selectVoPage(pageQuery.build(), wrapper);
        // ★coverUrl/posterUrl/logoUrl 库内存储 ossId：服务端统一翻译为 URL，前端 image-preview/小程序 image 可直接渲染
        page.getRecords().forEach(this::resolveImageUrls);
        return TableDataInfo.build(page);
    }

    @Override
    public PromotionActivityVo queryById(Long id) {
        PromotionActivityVo vo = activityMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("活动不存在");
        }
        resolveImageUrls(vo);
        return vo;
    }

    @Override
    public List<PromotionActivityVo> listPublished(Long tenantId, String activityType, String title, String beginTime, String endTime) {
        if (tenantId == null) {
            // 匿名浏览必须显式指定门店（租户），无 tenantId 直接返回空，避免跨租户泄露
            return Collections.emptyList();
        }
        // ★匿名请求线程无租户上下文：用 supply 显式限定 tenantId，多租户插件据此过滤，保证跨租户隔离
        // ★过滤已结束活动：status=ENDED / endTime 已过 / endTime 缺失时 startTime 已过均不展示
        //   （浏览页仅显示进行中/未开始的活动；已结束活动归入历史页与「我的活动 → 已结束」）
        List<PromotionActivityVo> list = TenantHelper.supply(tenantId, () -> activityMapper.selectVoList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getStatus, STATUS_PUBLISHED)
                .eq(StrUtil.isNotBlank(activityType), PromotionActivity::getActivityType, activityType)
                .like(StrUtil.isNotBlank(title), PromotionActivity::getTitle, title)
                .ge(StrUtil.isNotBlank(beginTime), PromotionActivity::getStartTime, beginTime)
                .le(StrUtil.isNotBlank(endTime), PromotionActivity::getStartTime, endTime + " 23:59:59")
                .and(w -> w
                    .and(x -> x.isNotNull(PromotionActivity::getEndTime)
                        .ge(PromotionActivity::getEndTime, new Date()))
                    .or(x -> x.isNull(PromotionActivity::getEndTime)
                        .and(y -> y.isNull(PromotionActivity::getStartTime)
                            .or().ge(PromotionActivity::getStartTime, new Date()))))
                .orderByDesc(PromotionActivity::getPublishTime)
                .orderByDesc(PromotionActivity::getCreateTime)));
        // ★匿名浏览页（小程序）无法自行调 /system/oss/listByIds：ossId 翻译必须服务端完成
        list.forEach(this::resolveImageUrls);
        return list;
    }

    @Override
    public TableDataInfo<PromotionActivityVo> queryHistoryPage(String title, String beginTime, String endTime, PageQuery pageQuery) {
        // ★历史活动 = 已取消（DRAFT 下架）+ 已完成（ENDED，含 endTime 已过的 PUBLISHED）
        LambdaQueryWrapper<PromotionActivity> wrapper = Wrappers.<PromotionActivity>lambdaQuery()
            .like(StrUtil.isNotBlank(title), PromotionActivity::getTitle, title)
            .ge(StrUtil.isNotBlank(beginTime), PromotionActivity::getStartTime, beginTime)
            .le(StrUtil.isNotBlank(endTime), PromotionActivity::getStartTime, endTime + " 23:59:59")
            .and(w -> w
                .in(PromotionActivity::getStatus, STATUS_DRAFT, STATUS_ENDED)
                .or(x -> x.eq(PromotionActivity::getStatus, STATUS_PUBLISHED)
                    .isNotNull(PromotionActivity::getEndTime)
                    .lt(PromotionActivity::getEndTime, new Date()))
                // ★endTime 缺失的时点型活动：startTime 已过即视为已完成，与浏览页过滤互补
                .or(x -> x.eq(PromotionActivity::getStatus, STATUS_PUBLISHED)
                    .isNull(PromotionActivity::getEndTime)
                    .isNotNull(PromotionActivity::getStartTime)
                    .lt(PromotionActivity::getStartTime, new Date())))
            .orderByDesc(PromotionActivity::getCreateTime);
        Page<PromotionActivityVo> page = activityMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(this::resolveImageUrls);
        return TableDataInfo.build(page);
    }

    /**
     * 翻译活动 VO 的图片字段（ossId → URL）
     */
    private void resolveImageUrls(PromotionActivityVo vo) {
        if (vo == null) {
            return;
        }
        vo.setCoverUrl(resolveOssUrls(vo.getCoverUrl()));
        vo.setPosterUrl(resolveOssUrls(vo.getPosterUrl()));
        vo.setLogoUrl(resolveOssUrls(vo.getLogoUrl()));
        // M19 新增字段：背景图/语音/二维码也需翻译
        vo.setBgImageUrl(resolveOssUrls(vo.getBgImageUrl()));
        vo.setVoiceUrl(resolveOssUrls(vo.getVoiceUrl()));
        vo.setQrcodeUrl(resolveOssUrls(vo.getQrcodeUrl()));
    }

    /**
     * 逗号分隔 ossId 串翻译为完整 URL 串。
     * <p>仅当全部 token 为纯数字（雪花 ossId）才翻译；已是 URL 或混合内容原样返回，避免二次翻译。
     * <p>翻译失败 fail-soft 返回原值：图片展示降级但不阻断主流程。
     */
    private String resolveOssUrls(String ossIds) {
        if (StrUtil.isBlank(ossIds)) {
            return ossIds;
        }
        for (String token : ossIds.split(",")) {
            if (!token.trim().matches("\\d+")) {
                return ossIds;
            }
        }
        try {
            return ossService.selectUrlByIds(ossIds);
        } catch (Exception e) {
            log.warn("OSS url 翻译失败，ossIds={}", ossIds, e);
            return ossIds;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(PromotionActivityBo bo) {
        // ★fail-fast：入口判空
        if (bo == null) {
            throw new ServiceException("活动参数不能为空");
        }

        // M20 T18 增强：语音上传后合规校验（insert 场景 activityId 尚未落库，传 null）
        if (StrUtil.isNotBlank(bo.getVoiceUrl())) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId == null) {
                throw new ServiceException("租户ID不能为空");
            }
            var checkResult = voiceComplianceService.checkVoiceCompliance(bo.getVoiceUrl(), tenantId, null);
            if (checkResult != null && !checkResult.isPass()) {
                log.warn("[insert] tenantId={} 语音合规检测失败 → 拒绝创建活动", tenantId);
                throw new ServiceException("语音内容包含敏感词，请重新录制");
            }
        }

        PromotionActivity entity = BeanCopyUtils.copy(bo, PromotionActivity.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getActivityType())) {
            entity.setActivityType("activity");
        }
        // 新建默认草稿；发布走 changeStatus 记 publish_time
        entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), STATUS_DRAFT));
        if (STATUS_PUBLISHED.equals(entity.getStatus())) {
            entity.setPublishTime(new Date());
        }
        // ★M22 自动计算活动费用（房间单价×时长 + 助教单价×时长）
        calculateAndSetAutoPrice(entity);
        // ★REQ-02 发布时立即锁定应收=活动总价（COALESCE(customPrice, autoCalculatedPrice)）
        if (STATUS_PUBLISHED.equals(entity.getStatus()) && entity.getReceivableAmount() == null) {
            BigDecimal effectivePrice = entity.getCustomPrice() != null
                ? entity.getCustomPrice() : entity.getAutoCalculatedPrice();
            if (effectivePrice != null) {
                entity.setReceivableAmount(effectivePrice);
            }
        }
        activityMapper.insert(entity);
        // ★BUG-02 若创建时直接以 PUBLISHED 状态入库，同步占用关联房间/大巴/导游/司机（→ in_use/working）
        if (STATUS_PUBLISHED.equals(entity.getStatus())) {
            selfBookingService.occupyActivityResources(entity.getId());
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(PromotionActivityBo bo) {
        // ★fail-fast：入口判空
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity exist = activityMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("活动不存在");
        }

        // M20 T18 增强：语音上传后合规校验（update 场景 activityId 已存在）
        // ★voiceUrl 与库中一致时跳过校验：同一内容在上次保存时已转写检测过，
        //   避免每次编辑都重复同步 ASR（最坏约 60s）导致接口长时间等待
        if (StrUtil.isNotBlank(bo.getVoiceUrl()) && !bo.getVoiceUrl().equals(exist.getVoiceUrl())) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId == null) {
                throw new ServiceException("租户ID不能为空");
            }
            var checkResult = voiceComplianceService.checkVoiceCompliance(bo.getVoiceUrl(), tenantId, bo.getId());
            if (checkResult != null && !checkResult.isPass()) {
                log.warn("[update] tenantId={}, activityId={} 语音合规检测失败 → 拒绝更新", tenantId, bo.getId());
                throw new ServiceException("语音内容包含敏感词，请重新录制");
            }
        }

        PromotionActivity update = BeanCopyUtils.copy(bo, PromotionActivity.class);
        // 状态变更统一走 changeStatus，避免编辑接口漏记 publish_time
        update.setStatus(null);
        // ★M22 重新计算自动价格（房间/助教/时长可能变更）
        PromotionActivity merged = new PromotionActivity();
        // 以库中已有值为底，合并本次编辑字段
        merged.setSelectedRoomIds(update.getSelectedRoomIds() != null ? update.getSelectedRoomIds() : exist.getSelectedRoomIds());
        merged.setSelectedStaffIds(update.getSelectedStaffIds() != null ? update.getSelectedStaffIds() : exist.getSelectedStaffIds());
        merged.setStartTime(update.getStartTime() != null ? update.getStartTime() : exist.getStartTime());
        merged.setEndTime(update.getEndTime() != null ? update.getEndTime() : exist.getEndTime());
        calculateAndSetAutoPrice(merged);
        update.setAutoCalculatedPrice(merged.getAutoCalculatedPrice());
        int rows = activityMapper.updateById(update);
        // ★BUG-02 已发布活动编辑关联房间/员工时，重新同步占用（先释放旧集合，再按新集合占用）
        if (rows > 0 && STATUS_PUBLISHED.equals(exist.getStatus())) {
            boolean roomsChanged = !Objects.equals(update.getSelectedRoomIds(), exist.getSelectedRoomIds());
            boolean staffChanged = !Objects.equals(update.getSelectedStaffIds(), exist.getSelectedStaffIds());
            if (roomsChanged || staffChanged) {
                selfBookingService.releaseActivityResources(Collections.singletonList(exist.getId()));
                selfBookingService.occupyActivityResources(exist.getId());
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(Long id, String status) {
        if (id == null || StrUtil.isBlank(status)) {
            throw new ServiceException("活动ID与目标状态不能为空");
        }
        PromotionActivity exist = activityMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("活动不存在");
        }
        String oldStatus = exist.getStatus();
        PromotionActivity update = new PromotionActivity();
        update.setId(id);
        update.setStatus(status);
        // ★手动结束活动时清除 isLocked（前端状态列优先显示 isLocked，不清除则已结束活动仍显示「已锁定」）
        if (STATUS_ENDED.equals(status)) {
            update.setIsLocked(false);
        }
        // 首次发布记录发布时间（重复发布不覆盖原始 publish_time）
        if (STATUS_PUBLISHED.equals(status) && exist.getPublishTime() == null) {
            update.setPublishTime(new Date());
        }
        // ★REQ-02 活动发布时立即锁定应收=活动总价（COALESCE(customPrice, autoCalculatedPrice)），
        //   不再等首次报名才锁定；仅 receivable_amount IS NULL 时写入，幂等。
        if (STATUS_PUBLISHED.equals(status) && exist.getReceivableAmount() == null) {
            BigDecimal effectivePrice = exist.getCustomPrice() != null
                ? exist.getCustomPrice() : exist.getAutoCalculatedPrice();
            if (effectivePrice != null) {
                update.setReceivableAmount(effectivePrice);
            }
        }
        int rows = activityMapper.updateById(update);
        // ★BUG-02 活动发布（非 PUBLISHED → PUBLISHED）时占用关联房间/大巴/导游/司机（→ in_use/working）
        if (rows > 0 && STATUS_PUBLISHED.equals(status) && !STATUS_PUBLISHED.equals(oldStatus)) {
            selfBookingService.occupyActivityResources(id);
        }
        // ★活动取消（PUBLISHED→DRAFT）或手动结束（PUBLISHED→ENDED）时释放关联房间和助教资源
        if (rows > 0 && STATUS_PUBLISHED.equals(oldStatus)
            && (STATUS_DRAFT.equals(status) || STATUS_ENDED.equals(status))) {
            selfBookingService.releaseActivityResources(Collections.singletonList(id));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        // ★删除前释放关联资源（避免已发布活动删除后资源仍被占用）
        selfBookingService.releaseActivityResources(new ArrayList<>(ids));
        // promotion_activity 含 del_flag，逻辑删除
        return activityMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> autoEndActivities() {
        // ★先查询即将结束的活动（用于后续释放关联资源）
        List<PromotionActivity> endingActivities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getStatus, STATUS_PUBLISHED)
                .isNotNull(PromotionActivity::getEndTime)
                .lt(PromotionActivity::getEndTime, new Date()));
        List<Long> endedIds = endingActivities.stream()
            .map(PromotionActivity::getId).collect(Collectors.toList());
        if (endedIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量置为 ENDED + 清除 isLocked（前端状态列优先显示 isLocked，不清除则已结束活动仍显示「已锁定」）
        activityMapper.update(null, Wrappers.<PromotionActivity>lambdaUpdate()
            .set(PromotionActivity::getStatus, STATUS_ENDED)
            .set(PromotionActivity::getIsLocked, false)
            .set(PromotionActivity::getUpdateTime, new Date())
            .set(PromotionActivity::getUpdateBy, "activityEndJob")
            .eq(PromotionActivity::getStatus, STATUS_PUBLISHED)
            .isNotNull(PromotionActivity::getEndTime)
            .lt(PromotionActivity::getEndTime, new Date()));
        // ★释放已结束活动的关联资源（房间/助教 → idle）
        selfBookingService.releaseActivityResources(endedIds);
        // ★REQ-02 活动结束回写实收金额（收款确认口径：已支付报名人数 × 人均应付）
        Date confirmTime = new Date();
        for (PromotionActivity act : endingActivities) {
            writeBackReceivedAmount(act, confirmTime);
        }
        return endedIds;
    }

    @Override
    public Map<String, Object> getSignupDetail(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // ★活动详情单页化：附带活动基础信息（ossId 已翻译），小程序详情页仅凭 activityId 即可自足渲染，
        //   避免列表页透传薄数据（如 myActivities 的 MyActivityVo 仅 bizId/标题）时渲染降级（缺介绍/语音/封面/状态）
        PromotionActivityVo activityVo = BeanCopyUtils.copy(activity, PromotionActivityVo.class);
        resolveImageUrls(activityVo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activity", activityVo);
        // 主持：活动设置选定员工（selected_staff_ids JSON 数组）
        List<Map<String, Object>> hosts = new ArrayList<>();
        for (Long staffId : parseIdJson(activity.getSelectedStaffIds())) {
            SysUser user = sysUserMapper.selectUserById(staffId);
            if (user != null) {
                Map<String, Object> cell = rosterCell(user.getUserId(), user.getNickName(), user.getAvatar(), false, null);
                // ★M34 名片弹层需要手机号才能拨号
                cell.put("phone", user.getPhonenumber());
                hosts.add(cell);
            }
        }
        result.put("hosts", hosts);
        // 指定房间：活动设置选定房间（selected_room_ids JSON 数组 → service_item 名称）
        List<String> roomNames = new ArrayList<>();
        List<Long> roomIdList = parseIdJson(activity.getSelectedRoomIds());
        List<ServiceItem> roomItems = roomIdList.isEmpty()
            ? Collections.emptyList() : serviceItemMapper.selectBatchIds(roomIdList);
        if (!roomItems.isEmpty()) {
            Map<Long, String> roomNameMap = roomItems.stream()
                .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName, (a, b) -> a));
            for (Long roomId : roomIdList) {
                String roomName = roomNameMap.get(roomId);
                if (roomName != null) {
                    roomNames.add(roomName);
                }
            }
        }
        result.put("roomNames", roomNames);
        // ★V3.9 §一.2/§一.3（T26.7 补遗）：座位布局活动标志 + 房间级背景图，
        // 供顾客端详情「选座报名」入口（选座即报名 CUSTOMER_PICK）与头图兜底渲染
        // ★增强检测：优先走 service_item.has_seat_layout 标志位，若未置位则回落查询 room_seat_layout 表
        ServiceItem seatRoom = roomItems.stream()
            .filter(r -> r.getHasSeatLayout() != null && r.getHasSeatLayout() == 1)
            .findFirst().orElse(null);
        if (seatRoom == null && !roomIdList.isEmpty()) {
            // 回落：直接查询 room_seat_layout 表确认是否存在布局记录（标志位可能未同步）
            seatRoom = detectSeatRoomByLayoutTable(roomIdList, roomItems);
        }
        result.put("hasSeatLayout", seatRoom != null);
        result.put("seatRoomId", seatRoom == null ? null : seatRoom.getId());
        result.put("roomBgImageUrl", seatRoom == null ? null : queryRoomBgImageUrl(seatRoom.getId()));
        // 报名名单：关联服务项目 project_signup（无关联容器时仅返回主持）
        List<Map<String, Object>> members = new ArrayList<>();
        int signedCount = 0;
        int waitlistCount = 0;
        Integer quota = null;
        ServiceItem itemRef = null;
        if (activity.getSelectedProjectId() != null) {
            Long projectId = activity.getSelectedProjectId();
            // ★M22 修复：selected_project_id 指向 service_item.id，不是 project.id
            itemRef = serviceItemMapper.selectById(projectId);
            if (itemRef != null && StrUtil.isNotBlank(itemRef.getExtraConfig())) {
                quota = JSONUtil.parseObj(itemRef.getExtraConfig()).getInt("quota");
            }
            // 容器成员角色（userId → role）用于名单角色标签
            Map<Long, String> roleMap = new LinkedHashMap<>();
            for (ProjectMember m : memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId))) {
                roleMap.putIfAbsent(m.getUserId(), m.getRole());
            }
            List<ProjectSignup> signups = signupMapper.selectList(Wrappers.<ProjectSignup>lambdaQuery()
                .eq(ProjectSignup::getProjectId, projectId)
                .orderByAsc(ProjectSignup::getSignupTime));
            List<Long> userIds = signups.stream().map(ProjectSignup::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
            for (ProjectSignup signup : signups) {
                boolean waitlist = signup.getIsWaitlist() != null && signup.getIsWaitlist() == 1;
                if (waitlist) {
                    waitlistCount++;
                } else {
                    signedCount += signup.getPeopleCount() == null ? 1 : signup.getPeopleCount();
                }
                SysUser user = signup.getUserId() == null ? null : userMap.get(signup.getUserId());
                String roleLabel = user == null ? null : ProjectMemberRole.of(roleMap.get(user.getUserId())).getLabel();
                // ★M34 T34.3：传手机号用于前端脱敏显示与拨号（游客报名无手机号）
                members.add(rosterCell(signup.getUserId(),
                    user != null ? user.getNickName() : "游客报名",
                    user != null ? user.getAvatar() : null,
                    user != null ? user.getPhonenumber() : null,
                    waitlist, roleLabel));
            }
        }
        // ★座位布局活动报名聚合（修复「已报名人数恒为 0」的反复缺陷）：
        //   座位布局活动顾客通过选座（CUSTOMER_PICK）/ Boss 加人（BOSS_ASSIGN）参与，
        //   只写 activity_seat_assignment、不产生 project_signup 记录；仅聚合 project_signup
        //   会导致 members 恒空、signedCount 恒 0（列表口径 calcActualParticipantCount 统计座位表，两处不一致）。
        //   此处按座位占用记录补全参与人并回传座位号；PRESET（导游/司机）归入主持块，不计入名额。
        if (seatRoom != null) {
            signedCount += mergeSeatAssignments(activityId, hosts, members);
        }
        result.put("members", members);
        result.put("signedCount", signedCount);
        result.put("waitlistCount", waitlistCount);
        result.put("quota", quota);
        result.put("remaining", quota == null ? null : Math.max(quota - signedCount, 0));
        result.put("projectId", activity.getSelectedProjectId());
        // 成团目标（最少成团人数）：业态策略实时判定（桌数×每桌/成团阈值，缺失回落 quota），-1 表示不限
        if (itemRef != null) {
            String bizCode = resolveBizCode(itemRef.getCategoryId());
            TeamContext ctx = new TeamContext();
            ctx.setProjectId(itemRef.getId());
            ctx.setTotalPeople(signedCount);
            ctx.setQuota(quota);
            ctx.setExtra(StrUtil.isNotBlank(itemRef.getExtraConfig()) ? JSONUtil.parseObj(itemRef.getExtraConfig()) : new cn.hutool.json.JSONObject());
            TeamResult team = teamStrategyDispatcher.evaluate(bizCode, ctx);
            result.put("teamTarget", team.getTarget());
            result.put("teamFormed", team.isFormed());
        } else {
            result.put("teamTarget", null);
            result.put("teamFormed", false);
        }
        // ★当前登录用户报名状态（小程序详情页按钮态：已报名/候补中）
        // ★M31 兼容匿名访问（扫码进入详情页）：未登录时跳过用户相关查询
        Long currentUserId = null;
        try {
            currentUserId = LoginHelper.getUserId();
        } catch (Exception e) {
            // 匿名访问时 LoginHelper 可能抛异常，降级为 null
            log.debug("[SignupDetail] 匿名访问，跳过当前用户状态查询");
        }
        boolean mySigned = false;
        boolean myWaitlist = false;
        for (Map<String, Object> cell : members) {
            if (Objects.equals(cell.get("userId"), currentUserId)) {
                if (Boolean.TRUE.equals(cell.get("isWaitlist"))) {
                    myWaitlist = true;
                } else {
                    mySigned = true;
                }
            }
        }
        result.put("mySigned", mySigned);
        result.put("myWaitlist", myWaitlist);
        // ★V3.9 §一.2：当前登录顾客在本活动的自选座位号（详情按钮态「已选座·查看座位」）
        String mySeatNo = null;
        if (currentUserId != null) {
            List<ActivitySeatAssignment> mineSeats = seatAssignmentMapper.selectList(
                Wrappers.<ActivitySeatAssignment>lambdaQuery()
                    .eq(ActivitySeatAssignment::getActivityId, activityId)
                    .eq(ActivitySeatAssignment::getUserId, currentUserId)
                    .orderByAsc(ActivitySeatAssignment::getId));
            if (!mineSeats.isEmpty()) {
                mySeatNo = mineSeats.get(0).getSeatNo();
            }
        }
        result.put("mySeatNo", mySeatNo);
        // 可取消标志：活动开始前 1 小时以上且已报名
        boolean canCancel = false;
        if ((mySigned || myWaitlist) && activity.getStartTime() != null) {
            long oneHourBefore = activity.getStartTime().getTime() - 3600_000L;
            canCancel = System.currentTimeMillis() < oneHourBefore;
        }
        result.put("canCancel", canCancel);
        // ★M22 费用信息回填（perPersonPrice / participantCount / isLocked）
        BigDecimal effectivePrice = activity.getCustomPrice() != null
            ? activity.getCustomPrice() : activity.getAutoCalculatedPrice();
        result.put("effectivePrice", effectivePrice);
        result.put("participantCount", activity.getParticipantCount());
        result.put("isLocked", activity.getIsLocked());
        // ★M33 §九 员工「申请锁定」后回显待审批状态（前端 staffBook/confirm.vue lockState=PENDING）
        result.put("lockRequestPending", lockRequestMapper.selectCount(
            Wrappers.<ActivityLockRequest>lambdaQuery()
                .eq(ActivityLockRequest::getActivityId, activityId)
                .eq(ActivityLockRequest::getStatus, ActivityLockRequest.STATUS_PENDING)) > 0);
        if (effectivePrice != null && activity.getParticipantCount() != null && activity.getParticipantCount() > 0) {
            result.put("perPersonPrice", effectivePrice.divide(
                BigDecimal.valueOf(activity.getParticipantCount()), 2, RoundingMode.HALF_UP));
        } else {
            result.put("perPersonPrice", null);
        }
        return result;
    }

    /**
     * ★V3.9 §一.3：查询房间级背景图（room_seat_layout.bg_image_url，T26.3 上传端点存 OSS URL）。
     * <p>防御式降级：T26.1 表可能未落地，查询失败返回 null（详情头图回落主题渐变），不阻断主流程。
     */
    private String queryRoomBgImageUrl(Long roomId) {
        if (roomId == null) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT bg_image_url FROM room_seat_layout WHERE room_id = ? LIMIT 1", roomId);
            if (rows.isEmpty()) {
                return null;
            }
            Object url = rows.get(0).get("bg_image_url");
            return url == null ? null : String.valueOf(url);
        } catch (Exception e) {
            log.warn("[SignupDetail] 查询 room_seat_layout 失败，房间背景图降级为空: roomId={}", roomId, e);
            return null;
        }
    }

    /**
     * ★回落检测：通过直接查询 room_seat_layout 表确认房间是否存在座位布局。
     * <p>场景：service_item.has_seat_layout 标志位未同步（布局通过 SQL 脚本/直接操作创建，未回写标志），
     * 导致前端详情页误判为“未开放线上报名”。防御式降级：表不存在或查询异常返回 null。
     *
     * @param roomIdList 活动关联的房间ID列表
     * @param roomItems  房间 ServiceItem 对象列表（用于返回匹配项）
     * @return 存在布局的房间 ServiceItem，或 null
     */
    private ServiceItem detectSeatRoomByLayoutTable(List<Long> roomIdList, List<ServiceItem> roomItems) {
        try {
            // 拼接 IN 占位符
            String placeholders = roomIdList.stream().map(id -> "?").collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT room_id FROM room_seat_layout WHERE room_id IN (" + placeholders + ") LIMIT 1",
                roomIdList.toArray());
            if (rows.isEmpty()) {
                return null;
            }
            Object layoutRoomId = rows.get(0).get("room_id");
            if (layoutRoomId == null) {
                return null;
            }
            Long matchedId = Long.valueOf(String.valueOf(layoutRoomId));
            // 从 roomItems 中找到对应的 ServiceItem
            return roomItems.stream()
                .filter(r -> matchedId.equals(r.getId()))
                .findFirst().orElse(null);
        } catch (Exception e) {
            log.debug("[SignupDetail] 回落检测 room_seat_layout 失败（表可能未落地）: roomIds={}", roomIdList, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> customerSignup(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (!STATUS_PUBLISHED.equals(activity.getStatus())) {
            throw new ServiceException("活动未发布，不可报名");
        }
        // ★M22 锁定校验：活动已锁定不可报名
        if (Boolean.TRUE.equals(activity.getIsLocked())) {
            throw new ServiceException("活动已锁定，不可报名");
        }
        if (activity.getEndTime() != null && activity.getEndTime().before(new Date())) {
            throw new ServiceException("活动已结束，不可报名");
        }
        Long projectId = activity.getSelectedProjectId();
        if (projectId == null) {
            // ★兜底：检查活动是否有座位布局，若有则引导用户走选座流程
            List<Long> roomIdList = parseIdJson(activity.getSelectedRoomIds());
            if (!roomIdList.isEmpty()) {
                List<ServiceItem> roomItems = serviceItemMapper.selectBatchIds(roomIdList);
                ServiceItem seatRoom = detectSeatRoomByLayoutTable(roomIdList, roomItems);
                if (seatRoom != null) {
                    throw new ServiceException("该活动需通过选座报名，请在活动详情页点击「选座报名」");
                }
            }
            throw new ServiceException("该活动未开放线上报名");
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后再报名");
        }
        // 防重复报名：同用户同容器已有报名记录即拦截（候补与已接受区分文案）
        List<ProjectSignup> mine = signupMapper.selectList(Wrappers.<ProjectSignup>lambdaQuery()
            .eq(ProjectSignup::getProjectId, projectId)
            .eq(ProjectSignup::getUserId, userId));
        if (!mine.isEmpty()) {
            boolean signed = mine.stream().anyMatch(s -> s.getIsWaitlist() == null || s.getIsWaitlist() == 0);
            throw new ServiceException(signed ? "您已报名该活动，请勿重复报名" : "您已在候补队列中，请耐心等待");
        }
        ProjectSignupBo bo = new ProjectSignupBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setPeopleCount(1);
        Map<String, Object> result = projectSignupService.signup(bo);
        // ★BUG-04 报名成功后同步刷新活动 participant_count（否则活动详情页参与人数长期为 0）
        refreshParticipantCount(activityId);
        // ★REQ-02 首次报名一次锁定应收金额（活动总价），后续报名不再变动
        lockReceivableAmount(activity);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long signupId, Long activityId) {
        // ★M22 锁定校验：活动已锁定不可取消
        if (activityId != null) {
            PromotionActivity activity = activityMapper.selectById(activityId);
            if (activity != null && Boolean.TRUE.equals(activity.getIsLocked())) {
                throw new ServiceException("活动已锁定，不可取消报名");
            }
        }
        projectSignupService.cancelByActivityId(signupId, activityId);
        // ★BUG-04 取消报名后同步刷新活动 participant_count（-peopleCount）
        refreshParticipantCount(activityId);
    }

    /**
     * ★BUG-04 从 project_signup 重新汇总活动 participant_count（仅统计非候补记录的 people_count）。
     * <p>报名 / 取消 / 座位编排后调用，避免活动详情页显示 0 人参与；
     * 无 selected_project_id（座位布局活动）时不改写，保留既有值。
     */
    private void refreshParticipantCount(Long activityId) {
        if (activityId == null) {
            return;
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getSelectedProjectId() == null) {
            return;
        }
        List<ProjectSignup> signups = signupMapper.selectList(
            Wrappers.<ProjectSignup>lambdaQuery()
                .eq(ProjectSignup::getProjectId, activity.getSelectedProjectId())
                .and(w -> w.isNull(ProjectSignup::getIsWaitlist).or().eq(ProjectSignup::getIsWaitlist, 0)));
        int total = signups.stream()
            .mapToInt(s -> s.getPeopleCount() == null ? 1 : s.getPeopleCount())
            .sum();
        PromotionActivity update = new PromotionActivity();
        update.setId(activityId);
        update.setParticipantCount(total);
        activityMapper.updateById(update);
    }

    /**
     * ★REQ-02 安全网：兜底锁定应收金额
     * <p>应收 = 活动总价 COALESCE(customPrice, autoCalculatedPrice)；仅在 receivable_amount IS NULL 时写入，
     *   用条件 UPDATE 保证并发幂等。
     * <p>★主路径已在 changeStatus/insert 发布时锁定，此方法作为安全网处理异常场景（如直接操作数据库跳过了发布流程）。
     */
    private void lockReceivableAmount(PromotionActivity activity) {
        if (activity == null || activity.getId() == null || activity.getReceivableAmount() != null) {
            return;
        }
        BigDecimal effectivePrice = activity.getCustomPrice() != null
            ? activity.getCustomPrice() : activity.getAutoCalculatedPrice();
        if (effectivePrice == null) {
            return;
        }
        activityMapper.update(null, Wrappers.<PromotionActivity>lambdaUpdate()
            .set(PromotionActivity::getReceivableAmount, effectivePrice)
            .eq(PromotionActivity::getId, activity.getId())
            .isNull(PromotionActivity::getReceivableAmount));
    }

    /**
     * ★REQ-02 活动结束回写实收金额
     * <p>活动报名走 project_signup（不产生 cash_flow），故实收按「收款确认」口径计算：
     *   人均应付(应收/设定人数) × 已支付(PAID)报名人数；实收不超过应收。
     *   历史活动若应收仍为空但已有报名，兜底补锁一次。
     */
    private void writeBackReceivedAmount(PromotionActivity act, Date confirmTime) {
        if (act == null || act.getId() == null) {
            return;
        }
        BigDecimal effectivePrice = act.getCustomPrice() != null
            ? act.getCustomPrice() : act.getAutoCalculatedPrice();
        BigDecimal receivable = act.getReceivableAmount() != null ? act.getReceivableAmount() : effectivePrice;
        Integer participantCount = act.getParticipantCount();
        BigDecimal received = BigDecimal.ZERO;
        if (receivable != null && act.getSelectedProjectId() != null
            && participantCount != null && participantCount > 0) {
            BigDecimal perPerson = receivable.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
            int paidPeople = sumPaidPeople(act.getSelectedProjectId());
            received = perPerson.multiply(BigDecimal.valueOf(paidPeople)).setScale(2, RoundingMode.HALF_UP);
            if (received.compareTo(receivable) > 0) {
                received = receivable;
            }
        }
        PromotionActivity update = new PromotionActivity();
        update.setId(act.getId());
        update.setReceivedAmount(received);
        update.setReceivedConfirmedTime(confirmTime);
        // 兜底：历史活动应收为空但已有报名，结束回写时一并补锁
        if (act.getReceivableAmount() == null && receivable != null) {
            update.setReceivableAmount(receivable);
        }
        activityMapper.updateById(update);
    }

    /**
     * ★REQ-02 汇总某项目已支付(PAID)的报名人数（非候补）
     */
    private int sumPaidPeople(Long projectId) {
        if (projectId == null) {
            return 0;
        }
        List<ProjectSignup> list = signupMapper.selectList(Wrappers.<ProjectSignup>lambdaQuery()
            .eq(ProjectSignup::getProjectId, projectId)
            .eq(ProjectSignup::getPayStatus, "PAID")
            .and(w -> w.isNull(ProjectSignup::getIsWaitlist).or().eq(ProjectSignup::getIsWaitlist, 0)));
        return list.stream().mapToInt(s -> s.getPeopleCount() == null ? 1 : s.getPeopleCount()).sum();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockActivity(Long activityId, boolean locked) {
        // ★兼容旧端点 /micro/promo/lock/{id}/{locked}；委派至核心逻辑（无操作日志切面）
        applyLockState(activityId, locked);
    }

    @SeatOperation(SeatOperationType.LOCK)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockActivity(Long activityId) {
        applyLockState(activityId, true);
    }

    @SeatOperation(SeatOperationType.UNLOCK)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockActivity(Long activityId) {
        applyLockState(activityId, false);
    }

    /**
     * ★M28 T28.1 锁定/解锁核心逻辑：
     * <ol>
     *   <li>更新 promotion_activity.is_locked（运行时锁定权威标志；status 维持不变，避免顾客浏览页丢失）</li>
     *   <li>全量刷 activity_seat_assignment.locked（锁定=1 座位只读 / 解锁=0 可再编排）</li>
     * </ol>
     */
    private void applyLockState(Long activityId, boolean locked) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity exist = activityMapper.selectById(activityId);
        if (exist == null) {
            throw new ServiceException("活动不存在");
        }
        // ① 更新活动 is_locked
        PromotionActivity update = new PromotionActivity();
        update.setId(activityId);
        update.setIsLocked(locked);
        activityMapper.updateById(update);
        // ② 全量刷座位占用表 locked 标志（多租户插件自动隔离，仅作用于本活动）
        int rows = seatAssignmentMapper.update(null,
            Wrappers.<ActivitySeatAssignment>lambdaUpdate()
                .set(ActivitySeatAssignment::getLocked, locked ? 1 : 0)
                .eq(ActivitySeatAssignment::getActivityId, activityId));
        log.info("[lockActivity] activityId={} locked={} seatAssignmentUpdated={}", activityId, locked, rows);
    }

    /**
     * ★活动是否已结束（前后端统一口径，前端见 ruoyi-app utils/format.js#isActivityEnded）：
     * <ol>
     *   <li>status=ENDED；</li>
     *   <li>endTime 已过；</li>
     *   <li>endTime 缺失时回落 startTime 已过（时点型活动，如按发车时间的大巴游）。</li>
     * </ol>
     * <p>已结束活动不得出现在顾客首页/浏览页「最新活动」，归入「我的活动 → 已结束」Tab 与历史页。
     */
    private boolean isActivityEnded(String status, Date startTime, Date endTime) {
        if (STATUS_ENDED.equals(status)) {
            return true;
        }
        Date now = new Date();
        if (endTime != null) {
            return now.after(endTime);
        }
        return startTime != null && now.after(startTime);
    }

    /** 从 service_category.industry_type 推导 bizCode（TeamStrategyDispatcher 对 null/未知 bizCode 回落默认策略） */
    private String resolveBizCode(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ServiceCategory category = serviceCategoryMapper.selectById(categoryId);
        return category != null ? category.getIndustryType() : null;
    }

    // ===== M22 顾客/助教「我的活动」合并列表 =====

    @Override
    public List<MyActivityVo> listCustomerActivities(String statusFilter) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        List<MyActivityVo> result = new ArrayList<>();
        // ★已添加的活动ID集合（用于去重：同一活动可能同时存在报名记录和选座记录）
        java.util.Set<Long> addedActivityIds = new java.util.HashSet<>();
        // ① 自助预约订单（booking_order.customer_user_id = 当前用户）
        List<BookingOrder> orders = bookingOrderMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .eq(BookingOrder::getCustomerUserId, userId)
                .ne(BookingOrder::getOrderStatus, "CANCELLED")
                .orderByDesc(BookingOrder::getBookingTime));
        for (BookingOrder order : orders) {
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("BOOKING");
            vo.setBizId(order.getId());
            vo.setTitle(order.getServiceName());
            vo.setStartTime(order.getBookingTime());
            vo.setStatus(order.getOrderStatus());
            vo.setServiceName(order.getServiceName());
            vo.setAmount(order.getAmount());
            vo.setPayStatus(order.getPayStatus());
            vo.setCreateTime(order.getCreateTime());
            // 解析助教名
            if (order.getStaffUserId() != null) {
                SysUser staff = sysUserMapper.selectUserById(order.getStaffUserId());
                vo.setStaffName(staff != null ? staff.getNickName() : null);
            }
            result.add(vo);
        }
        // ② 活动报名（project_signup.user_id = 当前用户 → promotion_activity）
        List<ProjectSignup> signups = signupMapper.selectList(
            Wrappers.<ProjectSignup>lambdaQuery()
                .eq(ProjectSignup::getUserId, userId));
        for (ProjectSignup signup : signups) {
            // 查找关联活动（selected_project_id = signup.projectId）
            List<PromotionActivity> activities = activityMapper.selectList(
                Wrappers.<PromotionActivity>lambdaQuery()
                    .eq(PromotionActivity::getSelectedProjectId, signup.getProjectId())
                    .last("LIMIT 1"));
            if (activities.isEmpty()) {
                continue;
            }
            PromotionActivity act = activities.get(0);
            addedActivityIds.add(act.getId());
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("ACTIVITY_SIGNUP");
            vo.setBizId(act.getId());
            vo.setTitle(act.getTitle());
            vo.setStartTime(act.getStartTime());
            vo.setEndTime(act.getEndTime());
            // ★ossId 翻译：cover_url 库内存储 ossId，返回原值会导致小程序详情头图加载失败
            vo.setCoverUrl(resolveOssUrls(act.getCoverUrl()));
            vo.setStatus(act.getStatus());
            vo.setIsLocked(act.getIsLocked());
            vo.setParticipantCount(calcActualParticipantCount(act));
            vo.setPayStatus(signup.getPayStatus());
            vo.setCreateTime(signup.getCreateTime());
            // 计算人均费用
            BigDecimal effectivePrice = act.getCustomPrice() != null
                ? act.getCustomPrice() : act.getAutoCalculatedPrice();
            vo.setAmount(effectivePrice);
            if (effectivePrice != null && act.getParticipantCount() != null && act.getParticipantCount() > 0) {
                vo.setPerPersonPrice(effectivePrice.divide(
                    BigDecimal.valueOf(act.getParticipantCount()), 2, RoundingMode.HALF_UP));
            }
            // ★填充主持人名称（从 selectedStaffIds 解析）
            vo.setHostNames(resolveHostNames(act.getSelectedStaffIds()));
            vo.setHostAvatars(resolveHostAvatars(act.getSelectedStaffIds()));
            // ★填充座位信息（从关联房间的座位布局查询）
            fillSeatInfo(vo, act.getSelectedRoomIds());
            result.add(vo);
        }
        // ③ ★选座参与的活动（activity_seat_assignment.user_id = 当前用户）
        //    座位布局活动无 selected_project_id，顾客通过选座参与，选座流程不创建 project_signup 记录，
        //    需单独查询 activity_seat_assignment 表补全此类活动。
        List<ActivitySeatAssignment> assignments = seatAssignmentMapper.selectList(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getUserId, userId));
        for (ActivitySeatAssignment assignment : assignments) {
            Long actId = assignment.getActivityId();
            if (actId == null || addedActivityIds.contains(actId)) {
                continue;
            }
            PromotionActivity act = activityMapper.selectById(actId);
            if (act == null) {
                continue;
            }
            addedActivityIds.add(actId);
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("ACTIVITY_SIGNUP");
            vo.setBizId(act.getId());
            vo.setTitle(act.getTitle());
            vo.setStartTime(act.getStartTime());
            vo.setEndTime(act.getEndTime());
            // ★ossId 翻译：cover_url 库内存储 ossId，返回原值会导致小程序详情头图加载失败
            vo.setCoverUrl(resolveOssUrls(act.getCoverUrl()));
            vo.setStatus(act.getStatus());
            vo.setIsLocked(act.getIsLocked());
            vo.setParticipantCount(calcActualParticipantCount(act));
            vo.setCreateTime(assignment.getAssignTime());
            // 计算人均费用
            BigDecimal effectivePrice = act.getCustomPrice() != null
                ? act.getCustomPrice() : act.getAutoCalculatedPrice();
            vo.setAmount(effectivePrice);
            if (effectivePrice != null && act.getParticipantCount() != null && act.getParticipantCount() > 0) {
                vo.setPerPersonPrice(effectivePrice.divide(
                    BigDecimal.valueOf(act.getParticipantCount()), 2, RoundingMode.HALF_UP));
            }
            // ★填充主持人名称
            vo.setHostNames(resolveHostNames(act.getSelectedStaffIds()));
            vo.setHostAvatars(resolveHostAvatars(act.getSelectedStaffIds()));
            // ★填充座位信息
            fillSeatInfo(vo, act.getSelectedRoomIds());
            result.add(vo);
        }
        // 按开始时间倒序
        result.sort((a, b) -> {
            if (a.getStartTime() == null && b.getStartTime() == null) return 0;
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return b.getStartTime().compareTo(a.getStartTime());
        });
        // ★状态过滤：null/空=全部，ONGOING=进行中，ENDED=已结束
        if (StrUtil.isNotBlank(statusFilter)) {
            result = result.stream().filter(vo -> {
                if ("ONGOING".equals(statusFilter)) {
                    if ("BOOKING".equals(vo.getSourceType())) {
                        return !"COMPLETED".equals(vo.getStatus()) && !"CANCELLED".equals(vo.getStatus());
                    }
                    return !isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                } else if ("ENDED".equals(statusFilter)) {
                    if ("BOOKING".equals(vo.getSourceType())) {
                        return "COMPLETED".equals(vo.getStatus());
                    }
                    return isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                }
                return true;
            }).collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public List<MyActivityVo> listBossActivities(String statusFilter) {
        // Boss 查看本店全部活动（不过滤用户）
        List<PromotionActivity> activities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .orderByDesc(PromotionActivity::getStartTime));
        List<MyActivityVo> result = new ArrayList<>();
        for (PromotionActivity act : activities) {
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("ACTIVITY_SIGNUP");
            vo.setBizId(act.getId());
            vo.setTitle(act.getTitle());
            vo.setStartTime(act.getStartTime());
            vo.setEndTime(act.getEndTime());
            vo.setCoverUrl(resolveOssUrls(act.getCoverUrl()));
            vo.setStatus(act.getStatus());
            vo.setIsLocked(act.getIsLocked());
            vo.setParticipantCount(calcActualParticipantCount(act));
            vo.setCreateTime(act.getCreateTime());
            BigDecimal effectivePrice = act.getCustomPrice() != null
                ? act.getCustomPrice() : act.getAutoCalculatedPrice();
            vo.setAmount(effectivePrice);
            if (effectivePrice != null && act.getParticipantCount() != null && act.getParticipantCount() > 0) {
                vo.setPerPersonPrice(effectivePrice.divide(
                    BigDecimal.valueOf(act.getParticipantCount()), 2, RoundingMode.HALF_UP));
            }
            vo.setHostNames(resolveHostNames(act.getSelectedStaffIds()));
            vo.setHostAvatars(resolveHostAvatars(act.getSelectedStaffIds()));
            fillSeatInfo(vo, act.getSelectedRoomIds());
            result.add(vo);
        }
        // ★状态过滤：null/空=全部，ONGOING=进行中，ENDED=已结束
        if (StrUtil.isNotBlank(statusFilter)) {
            result = result.stream().filter(vo -> {
                if ("ONGOING".equals(statusFilter)) {
                    return !isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                } else if ("ENDED".equals(statusFilter)) {
                    return isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                }
                return true;
            }).collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public List<MyActivityVo> listStaffActivities(String statusFilter) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        List<MyActivityVo> result = new ArrayList<>();
        // ★身份标签：当前登录员工的主角色显示名（租户角色映射动态名，如司机），无映射时 null 前端回退「主持」
        String myRoleLabel = userTenantRoleService.resolveRoleDisplayName(userId);
        // ① 作为服务员工的预约订单
        List<BookingOrder> orders = bookingOrderMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .eq(BookingOrder::getStaffUserId, userId)
                .ne(BookingOrder::getOrderStatus, "CANCELLED")
                .orderByDesc(BookingOrder::getBookingTime));
        for (BookingOrder order : orders) {
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("BOOKING");
            vo.setBizId(order.getId());
            vo.setTitle(order.getServiceName());
            vo.setStartTime(order.getBookingTime());
            vo.setStatus(order.getOrderStatus());
            vo.setServiceName(order.getServiceName());
            vo.setAmount(order.getAmount());
            vo.setRoleLabel(myRoleLabel);
            vo.setCreateTime(order.getCreateTime());
            result.add(vo);
        }
        // ② 被选中的活动（selected_staff_ids JSON 包含当前员工）
        // ★BUG-01：兼容 selected_staff_ids 存数字/字符串两种 JSON 格式
        //   PC 端 el-select 的 :value="item.id" 若 id 序列化为字符串则存 ["5"]，反之为 [5]；
        //   JSON_CONTAINS 严格类型匹配，需用 OR 同时兼容两种存储格式，避免员工/导游/司机登录 H5 看不到被指派活动。
        List<PromotionActivity> activities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .isNotNull(PromotionActivity::getSelectedStaffIds)
                .and(w -> w
                    .apply("JSON_CONTAINS(selected_staff_ids, CAST({0} AS JSON))", String.valueOf(userId))
                    .or()
                    .apply("JSON_CONTAINS(selected_staff_ids, CAST({0} AS JSON))", "\"" + userId + "\""))
                .orderByDesc(PromotionActivity::getStartTime));
        for (PromotionActivity act : activities) {
            MyActivityVo vo = new MyActivityVo();
            vo.setSourceType("ACTIVITY_SIGNUP");
            vo.setBizId(act.getId());
            vo.setTitle(act.getTitle());
            vo.setStartTime(act.getStartTime());
            vo.setEndTime(act.getEndTime());
            // ★ossId 翻译：cover_url 库内存储 ossId，返回原值会导致小程序详情头图加载失败
            vo.setCoverUrl(resolveOssUrls(act.getCoverUrl()));
            vo.setStatus(act.getStatus());
            vo.setIsLocked(act.getIsLocked());
            vo.setParticipantCount(calcActualParticipantCount(act));
            // ★主持人类角色标签：按主持人逐个解析角色显示名（去重保序拼接，如「导游、司机」）
            vo.setRoleLabel(resolveHostRoleLabels(act.getSelectedStaffIds()));
            vo.setCreateTime(act.getCreateTime());
            BigDecimal effectivePrice = act.getCustomPrice() != null
                ? act.getCustomPrice() : act.getAutoCalculatedPrice();
            vo.setAmount(effectivePrice);
            if (effectivePrice != null && act.getParticipantCount() != null && act.getParticipantCount() > 0) {
                vo.setPerPersonPrice(effectivePrice.divide(
                    BigDecimal.valueOf(act.getParticipantCount()), 2, RoundingMode.HALF_UP));
            }
            // ★填充主持人名称（从 selectedStaffIds 解析）
            vo.setHostNames(resolveHostNames(act.getSelectedStaffIds()));
            vo.setHostAvatars(resolveHostAvatars(act.getSelectedStaffIds()));
            // ★填充座位信息（从关联房间的座位布局查询）
            fillSeatInfo(vo, act.getSelectedRoomIds());
            result.add(vo);
        }
        // 按开始时间倒序
        result.sort((a, b) -> {
            if (a.getStartTime() == null && b.getStartTime() == null) return 0;
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return b.getStartTime().compareTo(a.getStartTime());
        });
        // ★状态过滤：null/空=全部，ONGOING=进行中（PUBLISHED 且未过期），ENDED=已结束
        if (StrUtil.isNotBlank(statusFilter)) {
            result = result.stream().filter(vo -> {
                if ("ONGOING".equals(statusFilter)) {
                    // BOOKING 类型按订单状态判断，ACTIVITY_SIGNUP 按活动状态 + 时间判断
                    if ("BOOKING".equals(vo.getSourceType())) {
                        return !"COMPLETED".equals(vo.getStatus()) && !"CANCELLED".equals(vo.getStatus());
                    }
                    return !isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                } else if ("ENDED".equals(statusFilter)) {
                    if ("BOOKING".equals(vo.getSourceType())) {
                        return "COMPLETED".equals(vo.getStatus());
                    }
                    return isActivityEnded(vo.getStatus(), vo.getStartTime(), vo.getEndTime());
                }
                return true;
            }).collect(Collectors.toList());
        }
        return result;
    }

    /** 解析 selectedStaffIds JSON，返回主持人名称拼接串（如 "黄导游、刘司机"） */
    private String resolveHostNames(String selectedStaffIdsJson) {
        List<Long> staffIds = parseIdJson(selectedStaffIdsJson);
        if (staffIds.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long staffId : staffIds) {
            SysUser user = sysUserMapper.selectUserById(staffId);
            if (user != null && StrUtil.isNotBlank(user.getNickName())) {
                names.add(user.getNickName());
            }
        }
        return names.isEmpty() ? null : String.join("、", names);
    }

    /** 解析 selectedStaffIds JSON，返回主持人角色显示名拼接串（如「导游、司机」，去重保序；无法解析返回 null，前端回退「主持」） */
    private String resolveHostRoleLabels(String selectedStaffIdsJson) {
        List<Long> staffIds = parseIdJson(selectedStaffIdsJson);
        if (staffIds.isEmpty()) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (Long staffId : staffIds) {
            String label = userTenantRoleService.resolveRoleDisplayName(staffId);
            if (StrUtil.isNotBlank(label) && !labels.contains(label)) {
                labels.add(label);
            }
        }
        return labels.isEmpty() ? null : String.join("、", labels);
    }

    /** 解析 selectedStaffIds JSON，返回主持人头像 URL 列表（与 resolveHostNames 一一对应） */
    private List<String> resolveHostAvatars(String selectedStaffIdsJson) {
        List<Long> staffIds = parseIdJson(selectedStaffIdsJson);
        if (staffIds.isEmpty()) {
            return null;
        }
        List<String> avatars = new ArrayList<>();
        for (Long staffId : staffIds) {
            SysUser user = sysUserMapper.selectUserById(staffId);
            if (user != null && StrUtil.isNotBlank(user.getAvatar())) {
                avatars.add(resolveOssUrls(user.getAvatar()));
            } else {
                avatars.add(null);
            }
        }
        return avatars.isEmpty() ? null : avatars;
    }

    /**
     * ★从真实数据源计算活动实际报名人数（替代 promotion_activity.participant_count 的 Boss 设定值）。
     * <p>座位布局活动：从 activity_seat_assignment 统计去重用户数（排除 PRESET 预置人员）；
     * 项目报名活动：从 project_signup 汇总非候补记录的 people_count。
     * <p>两种数据源均无记录时返回 0。
     */
    private int calcActualParticipantCount(PromotionActivity act) {
        if (act == null || act.getId() == null) {
            return 0;
        }
        // ① 座位布局活动：从 activity_seat_assignment 统计去重用户数（排除 PRESET 预置人员如导游/司机）
        List<Long> seatRoomIds = parseIdJson(act.getSelectedRoomIds());
        if (!seatRoomIds.isEmpty()) {
            try {
                List<Map<String, Object>> layouts = jdbcTemplate.queryForList(
                    "SELECT id FROM room_seat_layout WHERE room_id IN (" +
                    seatRoomIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") LIMIT 1",
                    seatRoomIds.toArray());
                if (!layouts.isEmpty()) {
                    Long layoutId = ((Number) layouts.get(0).get("id")).longValue();
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT COUNT(DISTINCT asa.user_id) AS cnt FROM activity_seat_assignment asa " +
                        "INNER JOIN room_seat rs ON asa.seat_id = rs.id " +
                        "WHERE asa.activity_id = ? AND rs.layout_id = ? AND asa.assignment_type != 'PRESET' AND asa.user_id IS NOT NULL",
                        act.getId(), layoutId);
                    if (!rows.isEmpty()) {
                        return ((Number) rows.get(0).get("cnt")).intValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[calcActualParticipantCount] 座位布局统计失败: activityId={}", act.getId(), e);
            }
        }
        // ② 项目报名活动：从 project_signup 汇总非候补记录的 people_count
        if (act.getSelectedProjectId() != null) {
            List<ProjectSignup> signups = signupMapper.selectList(
                Wrappers.<ProjectSignup>lambdaQuery()
                    .eq(ProjectSignup::getProjectId, act.getSelectedProjectId())
                    .and(w -> w.isNull(ProjectSignup::getIsWaitlist).or().eq(ProjectSignup::getIsWaitlist, 0)));
            return signups.stream()
                .mapToInt(s -> s.getPeopleCount() == null ? 1 : s.getPeopleCount())
                .sum();
        }
        return 0;
    }

    /** 从活动关联房间的座位布局中查询座位总数和已选数，填充到 VO */
    private void fillSeatInfo(MyActivityVo vo, String selectedRoomIdsJson) {
        List<Long> roomIds = parseIdJson(selectedRoomIdsJson);
        if (roomIds.isEmpty()) {
            return;
        }
        try {
            // 查询第一个有座位布局的房间
            List<Map<String, Object>> layouts = jdbcTemplate.queryForList(
                "SELECT id, room_id FROM room_seat_layout WHERE room_id IN (" +
                roomIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") LIMIT 1",
                roomIds.toArray());
            if (layouts.isEmpty()) {
                return;
            }
            Long layoutId = ((Number) layouts.get(0).get("id")).longValue();
            // 总座位数
            List<Map<String, Object>> seatRows = jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS total FROM room_seat WHERE layout_id = ?", layoutId);
            int total = seatRows.isEmpty() ? 0 : ((Number) seatRows.get(0).get("total")).intValue();
            // 已选座位数（activity_seat_assignment.seat_id → room_seat.id，通过 room_seat.layout_id 关联）
            Long activityId = vo.getBizId();
            if (activityId != null) {
                List<Map<String, Object>> assignedRows = jdbcTemplate.queryForList(
                    "SELECT COUNT(*) AS cnt FROM activity_seat_assignment asa " +
                    "INNER JOIN room_seat rs ON asa.seat_id = rs.id " +
                    "WHERE asa.activity_id = ? AND rs.layout_id = ?",
                    activityId, layoutId);
                int current = assignedRows.isEmpty() ? 0 : ((Number) assignedRows.get(0).get("cnt")).intValue();
                vo.setCurrentSeats(current);
            }
            vo.setTotalSeats(total);
        } catch (Exception e) {
            log.warn("[listStaffActivities] 查询座位信息失败，降级为空: activityId={}", vo.getBizId(), e);
        }
    }

    /**
     * ★座位布局活动报名聚合：按 activity_seat_assignment 补全 getSignupDetail 的主持/顾客名单。
     * <p>顾客自选（CUSTOMER_PICK）/ Boss 指派（BOSS_ASSIGN）→ 顾客名单（计入报名人数）；
     *   预置人员（PRESET，导游/司机）→ 主持块（不计入名额）；已在名单中的用户仅回显座位号。
     *
     * @return 新增顾客人数（用于累加 signedCount）
     */
    private int mergeSeatAssignments(Long activityId, List<Map<String, Object>> hosts, List<Map<String, Object>> members) {
        List<ActivitySeatAssignment> assigns = seatAssignmentMapper.selectList(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .isNotNull(ActivitySeatAssignment::getUserId)
                .orderByAsc(ActivitySeatAssignment::getId));
        if (assigns.isEmpty()) {
            return 0;
        }
        Map<Long, Map<String, Object>> memberByUser = new LinkedHashMap<>();
        for (Map<String, Object> cell : members) {
            if (cell.get("userId") != null) {
                memberByUser.put((Long) cell.get("userId"), cell);
            }
        }
        Map<Long, Map<String, Object>> hostByUser = new LinkedHashMap<>();
        for (Map<String, Object> cell : hosts) {
            if (cell.get("userId") != null) {
                hostByUser.put((Long) cell.get("userId"), cell);
            }
        }
        List<Long> missingUserIds = assigns.stream()
            .map(ActivitySeatAssignment::getUserId)
            .filter(uid -> !memberByUser.containsKey(uid) && !hostByUser.containsKey(uid))
            .distinct().collect(Collectors.toList());
        Map<Long, SysUser> missingUserMap = missingUserIds.isEmpty() ? Collections.emptyMap()
            : sysUserMapper.selectBatchIds(missingUserIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        int added = 0;
        for (ActivitySeatAssignment assign : assigns) {
            Long uid = assign.getUserId();
            if (ActivitySeatAssignment.TYPE_PRESET.equals(assign.getAssignmentType())) {
                // 预置人员（导游/司机）：主持块回显座位号；不在主持名单时补入（不计入名额）
                Map<String, Object> hostCell = hostByUser.get(uid);
                if (hostCell == null) {
                    SysUser u = missingUserMap.get(uid);
                    hostCell = rosterCell(uid, u != null ? u.getNickName() : "预置人员",
                        u != null ? u.getAvatar() : null, u != null ? u.getPhonenumber() : null, false, null);
                    hosts.add(hostCell);
                    hostByUser.put(uid, hostCell);
                }
                hostCell.put("seatNo", assign.getSeatNo());
            } else {
                // 顾客自选 / Boss 指派 → 计入报名人数
                Map<String, Object> cell = memberByUser.get(uid);
                if (cell == null) {
                    SysUser u = missingUserMap.get(uid);
                    cell = rosterCell(uid, u != null ? u.getNickName() : "顾客报名",
                        u != null ? u.getAvatar() : null, u != null ? u.getPhonenumber() : null, false, null);
                    members.add(cell);
                    memberByUser.put(uid, cell);
                    added++;
                }
                cell.put("seatNo", assign.getSeatNo());
            }
        }
        return added;
    }

    /** 名单单元：{userId, name, avatar, isWaitlist, roleLabel} */
    /**
     * ★M34 T34.3 报名名单单元格（含手机号用于脱敏显示与拨号）
     */
    private Map<String, Object> rosterCell(Long userId, String name, String avatar, boolean waitlist, String roleLabel) {
        return rosterCell(userId, name, avatar, null, waitlist, roleLabel);
    }

    /**
     * ★M34 T34.3 报名名单单元格（含手机号用于脱敏显示与拨号）
     */
    private Map<String, Object> rosterCell(Long userId, String name, String avatar, String phone, boolean waitlist, String roleLabel) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("userId", userId);
        cell.put("name", name);
        cell.put("avatar", avatar);
        cell.put("phone", phone);  // ★完整手机号（前端脱敏后展示，拨号时临时拼 tel:）
        cell.put("isWaitlist", waitlist);
        cell.put("roleLabel", roleLabel);
        return cell;
    }

    /** JSON 数组串解析为 ID 列表（脏数据 fail-soft 返回空列表，不阻断详情展示） */
    private List<Long> parseIdJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, Long.class);
        } catch (Exception e) {
            log.warn("ID JSON 解析失败：{}", json);
            return Collections.emptyList();
        }
    }

    /**
     * ★M22 自动计算活动费用：房间单价×时长 + 助教单价×时长 = auto_calculated_price
     * <p>时长 = (endTime - startTime) 向上取整到整小时。
     */
    private void calculateAndSetAutoPrice(PromotionActivity entity) {
        if (entity.getStartTime() == null || entity.getEndTime() == null) {
            entity.setAutoCalculatedPrice(null);
            return;
        }
        long diffMs = entity.getEndTime().getTime() - entity.getStartTime().getTime();
        if (diffMs <= 0) {
            entity.setAutoCalculatedPrice(null);
            return;
        }
        int hours = (int) Math.ceil(diffMs / 3600000.0);
        BigDecimal totalCost = BigDecimal.ZERO;
        // ① 房间费用：SUM(service_item.price × hours)
        List<Long> roomIds = parseIdJson(entity.getSelectedRoomIds());
        if (!roomIds.isEmpty()) {
            List<ServiceItem> rooms = serviceItemMapper.selectBatchIds(roomIds);
            for (ServiceItem room : rooms) {
                BigDecimal price = room.getPrice() != null ? room.getPrice() : BigDecimal.ZERO;
                totalCost = totalCost.add(price.multiply(BigDecimal.valueOf(hours)));
            }
        }
        // ② 助教费用：SUM(staff_pricing.hourly_rate × hours)
        List<Long> staffIds = parseIdJson(entity.getSelectedStaffIds());
        if (!staffIds.isEmpty()) {
            Map<Long, BigDecimal> rates = staffPricingService.batchGetHourlyRates(staffIds);
            for (Long staffId : staffIds) {
                BigDecimal rate = rates.getOrDefault(staffId, BigDecimal.ZERO);
                totalCost = totalCost.add(rate.multiply(BigDecimal.valueOf(hours)));
            }
        }
        entity.setAutoCalculatedPrice(totalCost);
    }
}
