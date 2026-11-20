package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.CheckInConfig;
import com.ruoyi.system.domain.CheckInRecord;
import com.ruoyi.system.domain.IndustryTemplate;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.CheckInBo;
import com.ruoyi.system.domain.bo.CheckInSupplementBo;
import com.ruoyi.system.domain.vo.CheckInConfigVo;
import com.ruoyi.system.domain.vo.CheckInRecordVo;
import com.ruoyi.system.mapper.CheckInConfigMapper;
import com.ruoyi.system.mapper.CheckInRecordMapper;
import com.ruoyi.system.mapper.IndustryTemplateMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ICheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工打卡服务实现（T24.1，小微版 M24）
 * <p>★三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled
 * <p>★打卡不受活动锁定影响，锁定后仍可打卡。
 * <p>★source 字段区分 NORMAL=正常打卡 / SUPPLEMENT=Boss 补录。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements ICheckInService {

    private final CheckInRecordMapper recordMapper;
    private final CheckInConfigMapper configMapper;
    private final PromotionActivityMapper activityMapper;
    private final SysTenantMapper tenantMapper;
    private final IndustryTemplateMapper industryTemplateMapper;
    private final SysUserMapper userMapper;

    /** 来源：正常打卡 */
    private static final String SOURCE_NORMAL = "NORMAL";
    /** 来源：Boss 补录 */
    private static final String SOURCE_SUPPLEMENT = "SUPPLEMENT";

    // ============================================================
    // 员工小程序
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitCheckIn(CheckInBo bo) {
        // ① 校验三级开关
        if (!isCheckInEnabled()) {
            throw new ServiceException("打卡功能未开启，请联系管理员");
        }
        // ② 校验拍照要求
        CheckInConfig config = getOrCreateConfig();
        if (config.getPhotoRequired() != null && config.getPhotoRequired() == 1
            && StrUtil.isBlank(bo.getPhotoUrl())) {
            throw new ServiceException("打卡必须拍照");
        }
        // ③ 查询活动（用于迟到判定）
        PromotionActivity activity = activityMapper.selectById(bo.getActivityId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // ④ 判断迟到
        Date now = new Date();
        boolean isLate = calcIsLate(now, activity.getStartTime(), config.getLateThresholdMinutes());
        // ⑤ 生成水印文字
        String watermarkText = bo.getWatermarkText();
        if (StrUtil.isBlank(watermarkText)) {
            watermarkText = generateWatermarkText(bo.getLocationName());
        }
        // ⑥ 写入记录
        CheckInRecord record = new CheckInRecord();
        record.setActivityId(bo.getActivityId());
        record.setStaffUserId(LoginHelper.getUserId());
        record.setCheckInTime(now);
        record.setLocationName(bo.getLocationName());
        record.setLocationLat(bo.getLocationLat());
        record.setLocationLng(bo.getLocationLng());
        record.setPhotoUrl(bo.getPhotoUrl());
        record.setIsLate(isLate ? 1 : 0);
        record.setWatermarkText(watermarkText);
        record.setSource(SOURCE_NORMAL);
        record.setRemark(bo.getRemark());
        recordMapper.insert(record);
        return record.getId();
    }

    @Override
    public List<CheckInRecordVo> myRecords(Long activityId, Date startDate, Date endDate) {
        Long userId = LoginHelper.getUserId();
        LambdaQueryWrapper<CheckInRecord> wrapper = Wrappers.<CheckInRecord>lambdaQuery()
            .eq(CheckInRecord::getStaffUserId, userId);
        if (activityId != null) {
            wrapper.eq(CheckInRecord::getActivityId, activityId);
        }
        if (startDate != null) {
            wrapper.ge(CheckInRecord::getCheckInTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(CheckInRecord::getCheckInTime, endDate);
        }
        wrapper.orderByDesc(CheckInRecord::getCheckInTime);
        return toVoList(recordMapper.selectList(wrapper));
    }

    // ============================================================
    // Boss PC 管理端
    // ============================================================

    @Override
    public List<CheckInRecordVo> activityRecords(Long activityId, Long staffUserId) {
        LambdaQueryWrapper<CheckInRecord> wrapper = Wrappers.<CheckInRecord>lambdaQuery();
        if (activityId != null) {
            wrapper.eq(CheckInRecord::getActivityId, activityId);
        }
        if (staffUserId != null) {
            wrapper.eq(CheckInRecord::getStaffUserId, staffUserId);
        }
        wrapper.orderByDesc(CheckInRecord::getCheckInTime);
        return toVoList(recordMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long supplementCheckIn(CheckInSupplementBo bo) {
        // ① 校验三级开关
        if (!isCheckInEnabled()) {
            throw new ServiceException("打卡功能未开启，无法补录");
        }
        // ② 查询活动
        PromotionActivity activity = activityMapper.selectById(bo.getActivityId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // ③ 判断迟到
        CheckInConfig config = getOrCreateConfig();
        boolean isLate = calcIsLate(bo.getCheckInTime(), activity.getStartTime(), config.getLateThresholdMinutes());
        // ④ 写入补录记录
        CheckInRecord record = new CheckInRecord();
        record.setActivityId(bo.getActivityId());
        record.setStaffUserId(bo.getStaffUserId());
        record.setCheckInTime(bo.getCheckInTime());
        record.setLocationName(bo.getLocationName());
        record.setIsLate(isLate ? 1 : 0);
        record.setWatermarkText(generateWatermarkText(bo.getLocationName()));
        record.setSource(SOURCE_SUPPLEMENT);
        record.setSupplementReason(bo.getSupplementReason());
        recordMapper.insert(record);
        return record.getId();
    }

    @Override
    public List<CheckInRecordVo> exportRecords(Long activityId, Long staffUserId, Date startDate, Date endDate) {
        LambdaQueryWrapper<CheckInRecord> wrapper = Wrappers.<CheckInRecord>lambdaQuery();
        if (activityId != null) {
            wrapper.eq(CheckInRecord::getActivityId, activityId);
        }
        if (staffUserId != null) {
            wrapper.eq(CheckInRecord::getStaffUserId, staffUserId);
        }
        if (startDate != null) {
            wrapper.ge(CheckInRecord::getCheckInTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(CheckInRecord::getCheckInTime, endDate);
        }
        wrapper.orderByDesc(CheckInRecord::getCheckInTime);
        List<CheckInRecord> list = recordMapper.selectList(wrapper);
        return toExportVoList(list);
    }

    // ============================================================
    // 打卡配置
    // ============================================================

    @Override
    public CheckInConfigVo getConfig() {
        CheckInConfig config = getOrCreateConfig();
        CheckInConfigVo vo = new CheckInConfigVo();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(CheckInConfigVo configVo) {
        CheckInConfig config = getOrCreateConfig();
        CheckInConfig update = new CheckInConfig();
        update.setId(config.getId());
        if (configVo.getEnabled() != null) {
            update.setEnabled(configVo.getEnabled());
        }
        if (configVo.getPhotoRequired() != null) {
            update.setPhotoRequired(configVo.getPhotoRequired());
        }
        if (configVo.getLateThresholdMinutes() != null) {
            update.setLateThresholdMinutes(configVo.getLateThresholdMinutes());
        }
        configMapper.updateById(update);
    }

    // ============================================================
    // 三级开关校验
    // ============================================================

    @Override
    public boolean isCheckInEnabled() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return false;
        }
        // 第一级：行业级开关
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return false;
        }
        if (StrUtil.isNotBlank(tenant.getIndustry())) {
            IndustryTemplate template = industryTemplateMapper.selectOne(
                Wrappers.<IndustryTemplate>lambdaQuery()
                    .eq(IndustryTemplate::getIndustryCode, tenant.getIndustry())
                    .eq(IndustryTemplate::getStatus, "0"));
            if (template != null && template.getCheckInEnabled() != null
                && template.getCheckInEnabled() == 0) {
                log.debug("[checkIn] 行业级打卡开关关闭：industry={}", tenant.getIndustry());
                return false;
            }
        }
        // 第二级：租户级开关
        if (tenant.getCheckInEnabled() != null && tenant.getCheckInEnabled() == 0) {
            log.debug("[checkIn] 租户级打卡开关关闭：tenantId={}", tenantId);
            return false;
        }
        // 第三级：Boss 配置级开关
        CheckInConfig config = configMapper.selectOne(
            Wrappers.<CheckInConfig>lambdaQuery()
                .eq(CheckInConfig::getTenantId, tenantId));
        if (config != null && config.getEnabled() != null && config.getEnabled() == 0) {
            log.debug("[checkIn] Boss 级打卡开关关闭：tenantId={}", tenantId);
            return false;
        }
        return true;
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    /**
     * 获取或初始化打卡配置（UNIQUE(tenant_id)，首次查询自动创建默认配置）
     */
    private CheckInConfig getOrCreateConfig() {
        Long tenantId = LoginHelper.getTenantId();
        CheckInConfig config = configMapper.selectOne(
            Wrappers.<CheckInConfig>lambdaQuery()
                .eq(CheckInConfig::getTenantId, tenantId));
        if (config == null) {
            // 自动创建默认配置
            config = new CheckInConfig();
            config.setEnabled(0);
            config.setPhotoRequired(1);
            config.setLateThresholdMinutes(15);
            configMapper.insert(config);
        }
        return config;
    }

    /**
     * 判断是否迟到：打卡时间 > 活动开始时间 + N 分钟
     */
    private boolean calcIsLate(Date checkInTime, Date activityStartTime, Integer thresholdMinutes) {
        if (activityStartTime == null || checkInTime == null) {
            return false;
        }
        int threshold = (thresholdMinutes != null && thresholdMinutes > 0) ? thresholdMinutes : 15;
        long deadlineMs = activityStartTime.getTime() + (long) threshold * 60 * 1000;
        return checkInTime.getTime() > deadlineMs;
    }

    /**
     * 生成水印文字（时间 + 位置 + 用户名）
     */
    private String generateWatermarkText(String locationName) {
        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (StrUtil.isNotBlank(locationName)) {
            sb.append(" | ").append(locationName);
        }
        String nickName = LoginHelper.getNickName();
        if (StrUtil.isNotBlank(nickName)) {
            sb.append(" | ").append(nickName);
        }
        return sb.toString();
    }

    /**
     * 实体列表 → VO 列表（含员工昵称/活动名称回填）
     */
    private List<CheckInRecordVo> toVoList(List<CheckInRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量加载用户
        Set<Long> userIds = records.stream().map(CheckInRecord::getStaffUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = loadUserMap(new ArrayList<>(userIds));
        // 批量加载活动
        Set<Long> activityIds = records.stream().map(CheckInRecord::getActivityId).collect(Collectors.toSet());
        Map<Long, String> activityTitleMap = loadActivityTitle(new ArrayList<>(activityIds));

        return records.stream().map(entity -> {
            CheckInRecordVo vo = new CheckInRecordVo();
            BeanUtils.copyProperties(entity, vo);
            // 回填员工昵称
            SysUser user = userMap.get(entity.getStaffUserId());
            if (user != null) {
                vo.setStaffName(user.getNickName());
            }
            // 回填活动名称
            vo.setActivityTitle(activityTitleMap.get(entity.getActivityId()));
            // 迟到转文字
            vo.setIsLateStr(entity.getIsLate() != null && entity.getIsLate() == 1 ? "是" : "否");
            // 来源转文字
            vo.setSource(SOURCE_SUPPLEMENT.equals(entity.getSource()) ? "补录" : "正常");
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 实体列表 → 导出 VO 列表（含 Excel 格式化）
     */
    private List<CheckInRecordVo> toExportVoList(List<CheckInRecord> records) {
        return toVoList(records);
    }

    /**
     * 批量加载用户信息
     */
    private Map<Long, SysUser> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
    }

    /**
     * 批量加载活动名称
     */
    private Map<Long, String> loadActivityTitle(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PromotionActivity> activities = activityMapper.selectBatchIds(activityIds);
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyMap();
        }
        return activities.stream().collect(Collectors.toMap(PromotionActivity::getId, PromotionActivity::getTitle, (a, b) -> a));
    }
}
