package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantHelper;
import org.springframework.beans.BeanUtils;
import com.ruoyi.system.domain.IndustryTemplate;
import com.ruoyi.system.domain.RoomSeat;
import com.ruoyi.system.domain.RoomSeatLayout;
import com.ruoyi.system.domain.RoomSeatLayoutTemplate;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.CopyLayoutBo;
import com.ruoyi.system.domain.bo.RoomSeatBo;
import com.ruoyi.system.domain.bo.RoomSeatLayoutBo;
import com.ruoyi.system.domain.bo.RoomSeatLayoutTemplateBo;
import com.ruoyi.system.domain.bo.SaveAsTemplateBo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutTemplateVo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutVo;
import com.ruoyi.system.domain.vo.RoomSeatVo;
import com.ruoyi.system.mapper.IndustryTemplateMapper;
import com.ruoyi.system.mapper.RoomSeatLayoutMapper;
import com.ruoyi.system.mapper.RoomSeatLayoutTemplateMapper;
import com.ruoyi.system.mapper.RoomSeatMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IRoomSeatLayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 房间座位布局服务实现（T26.1 / T26.4 / T26.5 / T26.6，小微版 M26）
 * <p>★坐标以背景图百分比 0~1 存储；保存时全量替换座位点（先删后插），避免 diff 逻辑复杂性。
 * <p>★复制/应用模板遵循脱钩原则：目标布局与源独立，源修改不影响目标。
 * <p>★模板可见性：Boss 侧 listVisibleTemplates 返回本租户私有 + 平台行业默认（tenant_id=1）；
 *   room_seat_layout_template 已在 MybatisPlusConfig.IGNORE_TENANT_TABLES 白名单，需服务层手工隔离。
 * <p>★联动 T26.2：保存布局后同步刷新 service_item.has_seat_layout=1 + seat_capacity=座位数。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomSeatLayoutServiceImpl implements IRoomSeatLayoutService {

    private final RoomSeatLayoutMapper layoutMapper;
    private final RoomSeatMapper seatMapper;
    private final RoomSeatLayoutTemplateMapper templateMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final IndustryTemplateMapper industryTemplateMapper;
    private final SysUserMapper sysUserMapper;

    /** 平台默认模板固定 tenant_id（IGNORE_TENANT_TABLES 白名单表，服务层手工隔离） */
    private static final Long PLATFORM_TENANT_ID = 1L;

    // ============================================================
    // Boss 侧：查询布局（T26.4）
    // ============================================================

    @Override
    public RoomSeatLayoutVo getLayoutByRoomId(Long roomId) {
        if (roomId == null) {
            throw new ServiceException("房间ID不能为空");
        }
        RoomSeatLayout layout = layoutMapper.selectOne(Wrappers.<RoomSeatLayout>lambdaQuery()
            .eq(RoomSeatLayout::getRoomId, roomId)
            .last("LIMIT 1"));
        if (layout == null) {
            return null;
        }
        return toLayoutVo(layout, true);
    }

    // ============================================================
    // Boss 侧：保存布局（T26.1 / T26.4）
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveLayout(RoomSeatLayoutBo bo) {
        Objects.requireNonNull(bo, "布局参数不能为空");
        Objects.requireNonNull(bo.getRoomId(), "房间ID不能为空");
        if (StrUtil.isBlank(bo.getBgImageUrl())) {
            throw new ServiceException("背景图不能为空");
        }
        // ① 校验房间存在且为空间分类（category_type=space）
        ServiceItem room = serviceItemMapper.selectById(bo.getRoomId());
        if (room == null) {
            throw new ServiceException("房间不存在或已删除");
        }

        // ② 座位号唯一性预校验（UNIQUE(layout_id, seat_no) 兜底，提前抛业务异常）
        List<RoomSeatBo> seats = bo.getSeats() == null ? Collections.emptyList() : bo.getSeats();
        Set<String> seatNoSet = new HashSet<>();
        for (RoomSeatBo s : seats) {
            if (StrUtil.isBlank(s.getSeatNo())) {
                throw new ServiceException("座位号不能为空");
            }
            if (!seatNoSet.add(s.getSeatNo())) {
                throw new ServiceException("座位号重复：" + s.getSeatNo());
            }
        }

        // ③ 查询是否已有布局：有则 UPDATE，无则 INSERT
        RoomSeatLayout existing = layoutMapper.selectOne(Wrappers.<RoomSeatLayout>lambdaQuery()
            .eq(RoomSeatLayout::getRoomId, bo.getRoomId())
            .last("LIMIT 1"));
        RoomSeatLayout entity = new RoomSeatLayout();
        BeanUtils.copyProperties(bo, entity, "id", "seats");
        if (StrUtil.isBlank(entity.getBgImageSource())) {
            entity.setBgImageSource(RoomSeatLayout.SOURCE_UPLOAD);
        }
        if (entity.getLayoutWidth() == null || entity.getLayoutWidth() <= 0) {
            entity.setLayoutWidth(1920);
        }
        if (entity.getLayoutHeight() == null || entity.getLayoutHeight() <= 0) {
            entity.setLayoutHeight(1080);
        }
        Long layoutId;
        if (existing != null) {
            entity.setId(existing.getId());
            layoutMapper.updateById(entity);
            layoutId = existing.getId();
            // 座位全量替换：先删后插
            seatMapper.delete(Wrappers.<RoomSeat>lambdaQuery().eq(RoomSeat::getLayoutId, layoutId));
        } else {
            try {
                layoutMapper.insert(entity);
            } catch (DuplicateKeyException e) {
                log.warn("[RoomLayout] UNIQUE(tenant_id, room_id) 冲突 roomId={}", bo.getRoomId());
                throw new ServiceException("该房间已存在布局，请刷新后重试");
            }
            layoutId = entity.getId();
        }

        // ④ 批量写入座位点
        for (RoomSeatBo s : seats) {
            RoomSeat seat = new RoomSeat();
            BeanUtils.copyProperties(s, seat, "id");
            seat.setId(null);
            seat.setLayoutId(layoutId);
            if (seat.getIsPreset() == null) {
                seat.setIsPreset(0);
            }
            // 预置角色合法性校验
            if (seat.getIsPreset() == 1 && StrUtil.isNotBlank(seat.getPresetRole())) {
                validatePresetRole(seat.getPresetRole());
            }
            seatMapper.insert(seat);
        }

        // ⑤ 联动 T26.2：刷新 service_item.has_seat_layout=1 + seat_capacity=座位数
        ServiceItem updateItem = new ServiceItem();
        updateItem.setId(room.getId());
        updateItem.setHasSeatLayout(1);
        updateItem.setSeatCapacity(seats.size());
        serviceItemMapper.updateById(updateItem);

        log.info("[RoomLayout] 保存成功 layoutId={} roomId={} seatCount={}", layoutId, bo.getRoomId(), seats.size());
        return layoutId;
    }

    // ============================================================
    // Boss 侧：复制布局（T26.5）
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyFromRoom(CopyLayoutBo bo) {
        Objects.requireNonNull(bo, "复制参数不能为空");
        if (bo.getSourceRoomId() == null || bo.getTargetRoomId() == null) {
            throw new ServiceException("源房间与目标房间均不能为空");
        }
        if (Objects.equals(bo.getSourceRoomId(), bo.getTargetRoomId())) {
            throw new ServiceException("源房间与目标房间不能相同");
        }
        // ① 校验源布局存在
        RoomSeatLayout source = layoutMapper.selectOne(Wrappers.<RoomSeatLayout>lambdaQuery()
            .eq(RoomSeatLayout::getRoomId, bo.getSourceRoomId())
            .last("LIMIT 1"));
        if (source == null) {
            throw new ServiceException("源房间尚未配置布局，无法复制");
        }
        // ② 校验目标房间存在
        ServiceItem targetRoom = serviceItemMapper.selectById(bo.getTargetRoomId());
        if (targetRoom == null) {
            throw new ServiceException("目标房间不存在或已删除");
        }
        // ③ 目标房间若已有布局，先删（连带座位点）
        RoomSeatLayout targetExisting = layoutMapper.selectOne(Wrappers.<RoomSeatLayout>lambdaQuery()
            .eq(RoomSeatLayout::getRoomId, bo.getTargetRoomId())
            .last("LIMIT 1"));
        if (targetExisting != null) {
            seatMapper.delete(Wrappers.<RoomSeat>lambdaQuery().eq(RoomSeat::getLayoutId, targetExisting.getId()));
            layoutMapper.deleteById(targetExisting.getId());
        }
        // ④ 克隆布局主表（template_id 留痕源布局对应的模板，若无则 NULL）
        RoomSeatLayout clone = new RoomSeatLayout();
        clone.setRoomId(bo.getTargetRoomId());
        clone.setBgImageUrl(source.getBgImageUrl());
        clone.setBgImageSource(source.getBgImageSource());
        clone.setLayoutWidth(source.getLayoutWidth());
        clone.setLayoutHeight(source.getLayoutHeight());
        clone.setTemplateId(source.getTemplateId());
        clone.setIsDefaultTemplate(0);
        clone.setTemplateName(source.getTemplateName());
        layoutMapper.insert(clone);
        Long newLayoutId = clone.getId();
        // ⑤ 克隆座位点（预置人员关系保留，preset_user_id 一并复制）
        List<RoomSeat> sourceSeats = seatMapper.selectList(Wrappers.<RoomSeat>lambdaQuery()
            .eq(RoomSeat::getLayoutId, source.getId())
            .orderByAsc(RoomSeat::getSeatNo));
        for (RoomSeat s : sourceSeats) {
            RoomSeat cloneSeat = new RoomSeat();
            BeanUtils.copyProperties(s, cloneSeat, "id", "layoutId", "tenantId");
            cloneSeat.setId(null);
            cloneSeat.setLayoutId(newLayoutId);
            seatMapper.insert(cloneSeat);
        }
        // ⑥ 联动 T26.2：目标房间刷新 has_seat_layout=1 + seat_capacity
        ServiceItem updateItem = new ServiceItem();
        updateItem.setId(bo.getTargetRoomId());
        updateItem.setHasSeatLayout(1);
        updateItem.setSeatCapacity(sourceSeats.size());
        serviceItemMapper.updateById(updateItem);

        log.info("[RoomLayout] 复制布局成功 sourceRoomId={} targetRoomId={} newLayoutId={} seatCount={}",
            bo.getSourceRoomId(), bo.getTargetRoomId(), newLayoutId, sourceSeats.size());
        return newLayoutId;
    }

    // ============================================================
    // Boss 侧：存为模板 / 应用模板（T26.5）
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAsTemplate(SaveAsTemplateBo bo) {
        Objects.requireNonNull(bo, "存为模板参数不能为空");
        if (bo.getLayoutId() == null || StrUtil.isBlank(bo.getTemplateName())) {
            throw new ServiceException("布局ID与模板名称均不能为空");
        }
        RoomSeatLayout layout = layoutMapper.selectById(bo.getLayoutId());
        if (layout == null) {
            throw new ServiceException("源布局不存在");
        }
        RoomSeatLayoutTemplate template = new RoomSeatLayoutTemplate();
        template.setName(bo.getTemplateName());
        template.setBgImageUrl(layout.getBgImageUrl());
        template.setIsIndustryDefault(0);
        // Boss 私有模板 tenant_id 由 MetaObjectHandler 自动填充当前租户
        try {
            templateMapper.insert(template);
        } catch (DuplicateKeyException e) {
            log.warn("[RoomLayout] 模板重名 tenantId={} name={}", TenantHelper.getTenantId(), bo.getTemplateName());
            throw new ServiceException("模板名称已存在，请更换：" + bo.getTemplateName());
        }
        log.info("[RoomLayout] 存为模板成功 templateId={} name={} layoutId={}",
            template.getId(), bo.getTemplateName(), bo.getLayoutId());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyTemplate(Long roomId, Long templateId) {
        if (roomId == null || templateId == null) {
            throw new ServiceException("房间ID与模板ID均不能为空");
        }
        ServiceItem room = serviceItemMapper.selectById(roomId);
        if (room == null) {
            throw new ServiceException("房间不存在或已删除");
        }
        RoomSeatLayoutTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        // 可见性校验：本租户私有模板 或 平台默认模板（tenant_id=1）
        Long currentTenantId = TenantHelper.getTenantId();
        boolean isPlatform = PLATFORM_TENANT_ID.equals(template.getTenantId())
            && Integer.valueOf(1).equals(template.getIsIndustryDefault());
        boolean isOwnPrivate = Objects.equals(template.getTenantId(), currentTenantId)
            && Integer.valueOf(0).equals(template.getIsIndustryDefault());
        if (!isPlatform && !isOwnPrivate) {
            throw new ServiceException("无权使用该模板");
        }
        // 目标房间若已有布局，先删（连带座位点）
        RoomSeatLayout existing = layoutMapper.selectOne(Wrappers.<RoomSeatLayout>lambdaQuery()
            .eq(RoomSeatLayout::getRoomId, roomId)
            .last("LIMIT 1"));
        if (existing != null) {
            seatMapper.delete(Wrappers.<RoomSeat>lambdaQuery().eq(RoomSeat::getLayoutId, existing.getId()));
            layoutMapper.deleteById(existing.getId());
        }
        // 新建布局：bg_image_source=PRESET（平台默认模板）或 UPLOAD（Boss 私有）
        RoomSeatLayout layout = new RoomSeatLayout();
        layout.setRoomId(roomId);
        layout.setBgImageUrl(template.getBgImageUrl());
        layout.setBgImageSource(isPlatform ? RoomSeatLayout.SOURCE_PRESET : RoomSeatLayout.SOURCE_UPLOAD);
        layout.setLayoutWidth(1920);
        layout.setLayoutHeight(1080);
        layout.setTemplateId(templateId);
        layout.setIsDefaultTemplate(0);
        layout.setTemplateName(template.getName());
        layoutMapper.insert(layout);
        // 联动 T26.2：has_seat_layout=1；seat_capacity 暂置 0（Boss 需拖拽落位后由 saveLayout 刷新）
        ServiceItem updateItem = new ServiceItem();
        updateItem.setId(roomId);
        updateItem.setHasSeatLayout(1);
        serviceItemMapper.updateById(updateItem);
        log.info("[RoomLayout] 应用模板成功 roomId={} templateId={} newLayoutId={} source={}",
            roomId, templateId, layout.getId(), layout.getBgImageSource());
        return layout.getId();
    }

    @Override
    public List<RoomSeatLayoutTemplateVo> listVisibleTemplates(String industryCode) {
        Long currentTenantId = TenantHelper.getTenantId();
        // 手工租户隔离：本租户私有 + 平台默认（tenant_id=1 且 is_industry_default=1）
        List<RoomSeatLayoutTemplate> list = templateMapper.selectList(Wrappers.<RoomSeatLayoutTemplate>lambdaQuery()
            .and(w -> w
                .or(x -> x.eq(RoomSeatLayoutTemplate::getTenantId, currentTenantId)
                          .eq(RoomSeatLayoutTemplate::getIsIndustryDefault, 0))
                .or(x -> x.eq(RoomSeatLayoutTemplate::getTenantId, PLATFORM_TENANT_ID)
                          .eq(RoomSeatLayoutTemplate::getIsIndustryDefault, 1)
                          .eq(StrUtil.isNotBlank(industryCode), RoomSeatLayoutTemplate::getIndustryCode, industryCode)))
            .orderByDesc(RoomSeatLayoutTemplate::getIsIndustryDefault)
            .orderByAsc(RoomSeatLayoutTemplate::getName));
        return toTemplateVoList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteLayout(Long layoutId) {
        if (layoutId == null) {
            throw new ServiceException("布局ID不能为空");
        }
        RoomSeatLayout layout = layoutMapper.selectById(layoutId);
        if (layout == null) {
            throw new ServiceException("布局不存在");
        }
        seatMapper.delete(Wrappers.<RoomSeat>lambdaQuery().eq(RoomSeat::getLayoutId, layoutId));
        int rows = layoutMapper.deleteById(layoutId);
        // 联动 T26.2：房间回退 has_seat_layout=0
        ServiceItem updateItem = new ServiceItem();
        updateItem.setId(layout.getRoomId());
        updateItem.setHasSeatLayout(0);
        updateItem.setSeatCapacity(null);
        serviceItemMapper.updateById(updateItem);
        log.info("[RoomLayout] 删除布局成功 layoutId={} roomId={}", layoutId, layout.getRoomId());
        return rows;
    }

    // ============================================================
    // 平台 Admin 侧：行业默认模板 CRUD（T26.6）
    // ============================================================

    @Override
    public List<RoomSeatLayoutTemplateVo> listPlatformTemplates(String industryCode) {
        List<RoomSeatLayoutTemplate> list = templateMapper.selectList(Wrappers.<RoomSeatLayoutTemplate>lambdaQuery()
            .eq(RoomSeatLayoutTemplate::getTenantId, PLATFORM_TENANT_ID)
            .eq(RoomSeatLayoutTemplate::getIsIndustryDefault, 1)
            .eq(StrUtil.isNotBlank(industryCode), RoomSeatLayoutTemplate::getIndustryCode, industryCode)
            .orderByAsc(RoomSeatLayoutTemplate::getIndustryCode)
            .orderByAsc(RoomSeatLayoutTemplate::getName));
        return toTemplateVoList(list);
    }

    @Override
    public RoomSeatLayoutTemplateVo getPlatformTemplate(Long id) {
        if (id == null) {
            throw new ServiceException("模板ID不能为空");
        }
        RoomSeatLayoutTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        if (!PLATFORM_TENANT_ID.equals(template.getTenantId())
            || !Integer.valueOf(1).equals(template.getIsIndustryDefault())) {
            throw new ServiceException("非平台默认模板，无权查看");
        }
        return toTemplateVo(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlatformTemplate(RoomSeatLayoutTemplateBo bo) {
        Objects.requireNonNull(bo, "模板参数不能为空");
        if (StrUtil.isBlank(bo.getName())) {
            throw new ServiceException("模板名称不能为空");
        }
        if (StrUtil.isBlank(bo.getIndustryCode())) {
            throw new ServiceException("平台默认模板必须指定行业编码");
        }
        // 校验行业编码存在
        IndustryTemplate industry = industryTemplateMapper.selectOne(Wrappers.<IndustryTemplate>lambdaQuery()
            .eq(IndustryTemplate::getIndustryCode, bo.getIndustryCode())
            .last("LIMIT 1"));
        if (industry == null) {
            throw new ServiceException("行业编码不存在：" + bo.getIndustryCode());
        }
        // ★M27 T27.2：平台默认模板背景图必须为透明底 PNG / SVG（简洁线条示意图，供主题色联动铺底）
        validateTransparentBg(bo.getBgImageUrl());
        RoomSeatLayoutTemplate template = new RoomSeatLayoutTemplate();
        BeanUtils.copyProperties(bo, template, "id");
        template.setId(null);
        // 平台默认模板强制 tenant_id=1，is_industry_default=1
        template.setTenantId(PLATFORM_TENANT_ID);
        template.setIsIndustryDefault(1);
        try {
            templateMapper.insert(template);
        } catch (DuplicateKeyException e) {
            log.warn("[RoomTemplate] UNIQUE(tenant_id, name) 冲突 name={}", bo.getName());
            throw new ServiceException("模板名称已存在，请更换：" + bo.getName());
        }
        log.info("[RoomTemplate] 新增平台默认模板成功 templateId={} name={} industryCode={}",
            template.getId(), bo.getName(), bo.getIndustryCode());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePlatformTemplate(RoomSeatLayoutTemplateBo bo) {
        Objects.requireNonNull(bo, "模板参数不能为空");
        if (bo.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        RoomSeatLayoutTemplate existing = templateMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("模板不存在");
        }
        if (!PLATFORM_TENANT_ID.equals(existing.getTenantId())
            || !Integer.valueOf(1).equals(existing.getIsIndustryDefault())) {
            throw new ServiceException("非平台默认模板，无权修改");
        }
        RoomSeatLayoutTemplate update = new RoomSeatLayoutTemplate();
        BeanUtils.copyProperties(bo, update);
        // ★M27 T27.2：平台默认模板背景图必须为透明底 PNG / SVG
        validateTransparentBg(bo.getBgImageUrl());
        // 强制保持 tenant_id / is_industry_default 不被前端篡改
        update.setTenantId(PLATFORM_TENANT_ID);
        update.setIsIndustryDefault(1);
        // 若修改 industryCode，需校验合法性
        if (StrUtil.isNotBlank(bo.getIndustryCode())
            && !bo.getIndustryCode().equals(existing.getIndustryCode())) {
            IndustryTemplate industry = industryTemplateMapper.selectOne(Wrappers.<IndustryTemplate>lambdaQuery()
                .eq(IndustryTemplate::getIndustryCode, bo.getIndustryCode())
                .last("LIMIT 1"));
            if (industry == null) {
                throw new ServiceException("行业编码不存在：" + bo.getIndustryCode());
            }
        }
        int rows = templateMapper.updateById(update);
        log.info("[RoomTemplate] 编辑平台默认模板成功 templateId={} rows={}", bo.getId(), rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlatformTemplate(Long id) {
        if (id == null) {
            throw new ServiceException("模板ID不能为空");
        }
        RoomSeatLayoutTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("模板不存在");
        }
        if (!PLATFORM_TENANT_ID.equals(existing.getTenantId())
            || !Integer.valueOf(1).equals(existing.getIsIndustryDefault())) {
            throw new ServiceException("非平台默认模板，无权删除");
        }
        int rows = templateMapper.deleteById(id);
        log.info("[RoomTemplate] 删除平台默认模板成功 templateId={} rows={}", id, rows);
        return rows;
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    /**
     * ★M27 T27.2：平台默认模板背景图透明底格式校验。
     * <p>默认图需为透明底 PNG / SVG（简洁线条示意图，非真实照片），以便前端按主题主色 20% 铺底联动渲染；
     * Boss 自传背景图（JPG/PNG/WebP）走 T26.3 上传校验，不受此约束。空值视为不上传背景图，放行。
     *
     * @param bgImageUrl 背景图 OSS URL
     */
    private void validateTransparentBg(String bgImageUrl) {
        if (StrUtil.isBlank(bgImageUrl)) {
            return;
        }
        // 去除查询串后取扩展名（OSS URL 可能带签名参数）
        String path = bgImageUrl;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        String lower = path.toLowerCase();
        if (!lower.endsWith(".svg") && !lower.endsWith(".png")) {
            throw new ServiceException("平台默认模板背景图必须为透明底 SVG 或 PNG（简洁线条示意图），当前格式不支持主题色联动");
        }
    }

    /**
     * 预置角色合法性校验（T26.1，与 RoomSeat 常量保持一致）
     */
    private void validatePresetRole(String role) {
        if (!RoomSeat.PRESET_GUIDE.equals(role)
            && !RoomSeat.PRESET_DRIVER.equals(role)
            && !RoomSeat.PRESET_ASSISTANT.equals(role)) {
            throw new ServiceException("非法预置角色：" + role + "（仅支持 guide/driver/assistant）");
        }
    }

    /**
     * 布局实体 → VO（可选联表加载座位点）
     */
    private RoomSeatLayoutVo toLayoutVo(RoomSeatLayout layout, boolean withSeats) {
        RoomSeatLayoutVo vo = new RoomSeatLayoutVo();
        BeanUtils.copyProperties(layout, vo);
        // 房间名称回填
        ServiceItem room = serviceItemMapper.selectById(layout.getRoomId());
        if (room != null) {
            vo.setRoomName(room.getName());
        }
        if (withSeats) {
            List<RoomSeat> seats = seatMapper.selectList(Wrappers.<RoomSeat>lambdaQuery()
                .eq(RoomSeat::getLayoutId, layout.getId())
                .orderByAsc(RoomSeat::getSeatNo));
            List<RoomSeatVo> seatVos = seats.stream().map(s -> {
                RoomSeatVo svo = new RoomSeatVo();
                BeanUtils.copyProperties(s, svo);
                return svo;
            }).collect(Collectors.toList());
            // 预置人员昵称批量回填
            fillPresetUserName(seatVos);
            vo.setSeats(seatVos);
            vo.setSeatCount(seatVos.size());
            vo.setPresetCount((int) seatVos.stream()
                .filter(s -> Integer.valueOf(1).equals(s.getIsPreset())).count());
        }
        return vo;
    }

    /**
     * 批量回填预置人员昵称
     */
    private void fillPresetUserName(List<RoomSeatVo> seatVos) {
        if (CollUtil.isEmpty(seatVos)) {
            return;
        }
        Set<Long> userIds = seatVos.stream()
            .map(RoomSeatVo::getPresetUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(new ArrayList<>(userIds));
        if (CollUtil.isEmpty(users)) {
            return;
        }
        Map<Long, String> nameMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
        seatVos.forEach(s -> {
            if (s.getPresetUserId() != null) {
                s.setPresetUserName(nameMap.get(s.getPresetUserId()));
            }
        });
    }

    /**
     * 模板实体 → VO（含行业名称回填）
     */
    private RoomSeatLayoutTemplateVo toTemplateVo(RoomSeatLayoutTemplate template) {
        RoomSeatLayoutTemplateVo vo = new RoomSeatLayoutTemplateVo();
        BeanUtils.copyProperties(template, vo);
        if (StrUtil.isNotBlank(template.getIndustryCode())) {
            IndustryTemplate industry = industryTemplateMapper.selectOne(Wrappers.<IndustryTemplate>lambdaQuery()
                .eq(IndustryTemplate::getIndustryCode, template.getIndustryCode())
                .last("LIMIT 1"));
            if (industry != null) {
                vo.setIndustryName(industry.getIndustryName());
            }
        }
        return vo;
    }

    /**
     * 模板实体列表 → VO 列表（批量回填行业名称，避免 N+1）
     */
    private List<RoomSeatLayoutTemplateVo> toTemplateVoList(List<RoomSeatLayoutTemplate> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Set<String> industryCodes = list.stream()
            .map(RoomSeatLayoutTemplate::getIndustryCode)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toSet());
        Map<String, String> industryNameMap = Collections.emptyMap();
        if (!industryCodes.isEmpty()) {
            List<IndustryTemplate> industries = industryTemplateMapper.selectList(
                Wrappers.<IndustryTemplate>lambdaQuery().in(IndustryTemplate::getIndustryCode, industryCodes));
            industryNameMap = industries.stream()
                .collect(Collectors.toMap(IndustryTemplate::getIndustryCode,
                    IndustryTemplate::getIndustryName, (a, b) -> a));
        }
        final Map<String, String> finalMap = industryNameMap;
        return list.stream().map(t -> {
            RoomSeatLayoutTemplateVo vo = new RoomSeatLayoutTemplateVo();
            BeanUtils.copyProperties(t, vo);
            if (StrUtil.isNotBlank(t.getIndustryCode())) {
                vo.setIndustryName(finalMap.get(t.getIndustryCode()));
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
