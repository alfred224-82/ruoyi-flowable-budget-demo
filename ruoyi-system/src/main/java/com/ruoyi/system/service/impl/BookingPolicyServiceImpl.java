package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.BookingPolicy;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.BookingPolicyBo;
import com.ruoyi.system.domain.vo.BookingPolicyVo;
import com.ruoyi.system.mapper.BookingPolicyMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IBookingPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 预约策略配置服务实现（T18.10，小微版 M18）
 * <p>★fail-fast：入口判空 + 值域校验。
 * <p>★upsert 模式：每租户一条策略。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPolicyServiceImpl implements IBookingPolicyService {

    private final BookingPolicyMapper policyMapper;
    private final SysTenantMapper tenantMapper;

    /** 合法取消策略白名单 */
    private static final Set<String> VALID_CANCEL_POLICIES = new HashSet<>(
        Arrays.asList("FREE", "FEE", "FORBID"));

    /** REQ-01：项目锁死必选行业（旅游/按摩/足浴）——项目强制开启且必填，前端置灰只读 */
    private static final Set<String> PROJECT_LOCKED_INDUSTRIES = new HashSet<>(
        Arrays.asList("travel", "massage", "foot_bath"));

    /** REQ-01：读取当前登录租户行业编码（无登录上下文或租户不存在时返回 null） */
    private String currentIndustryCode() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return null;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null ? tenant.getIndustry() : null;
    }

    /** REQ-01：行业是否为项目锁死型 */
    private boolean isProjectLockedIndustry(String industry) {
        return industry != null && PROJECT_LOCKED_INDUSTRIES.contains(industry);
    }

    /** REQ-01：为 Vo 补齐三类资源开关默认值（enabled=1/required=0），锁死行业强制项目开启且必填 */
    private void applyResourceSwitchDefaults(BookingPolicyVo vo, String industry) {
        if (vo.getProjectBookingEnabled() == null) vo.setProjectBookingEnabled(1);
        if (vo.getProjectBookingRequired() == null) vo.setProjectBookingRequired(0);
        if (vo.getRoomBookingEnabled() == null) vo.setRoomBookingEnabled(1);
        if (vo.getRoomBookingRequired() == null) vo.setRoomBookingRequired(0);
        if (vo.getStaffBookingEnabled() == null) vo.setStaffBookingEnabled(1);
        if (vo.getStaffBookingRequired() == null) vo.setStaffBookingRequired(0);
        vo.setIndustryCode(industry);
        if (isProjectLockedIndustry(industry)) {
            vo.setProjectBookingEnabled(1);
            vo.setProjectBookingRequired(1);
        }
    }

    @Override
    public BookingPolicyVo queryPolicy() {
        String industry = currentIndustryCode();
        LambdaQueryWrapper<BookingPolicy> wrapper = Wrappers.<BookingPolicy>lambdaQuery()
            .last("LIMIT 1");
        BookingPolicyVo vo = policyMapper.selectVoOne(wrapper);
        if (vo == null) {
            // 返回默认策略
            BookingPolicyVo defaultVo = new BookingPolicyVo();
            defaultVo.setCustomerSelfBooking(1);
            defaultVo.setAdvanceBookingDays(7);
            defaultVo.setCancelPolicy("FREE");
            defaultVo.setCancelFreeHours(2);
            defaultVo.setNoShowThreshold(3);
            defaultVo.setAutoConfirm(0);
            applyResourceSwitchDefaults(defaultVo, industry);
            return defaultVo;
        }
        applyResourceSwitchDefaults(vo, industry);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePolicy(BookingPolicyBo bo) {
        // ★fail-fast
        if (bo == null) {
            throw new ServiceException("策略参数不能为空");
        }
        // 值域校验
        if (bo.getAdvanceBookingDays() != null && bo.getAdvanceBookingDays() < 0) {
            throw new ServiceException("提前预约天数不能为负数");
        }
        if (bo.getAdvanceBookingDays() != null && bo.getAdvanceBookingDays() > 365) {
            throw new ServiceException("提前预约天数不能超过365天");
        }
        if (bo.getCancelPolicy() != null && !VALID_CANCEL_POLICIES.contains(bo.getCancelPolicy())) {
            throw new ServiceException("取消策略不合法：" + bo.getCancelPolicy());
        }
        if (bo.getCancelFreeHours() != null && bo.getCancelFreeHours() < 0) {
            throw new ServiceException("免费取消截止小时数不能为负数");
        }
        if (bo.getNoShowThreshold() != null && bo.getNoShowThreshold() < 1) {
            throw new ServiceException("爽约次数阈值至少为1");
        }
        if (bo.getMaxDailyBookings() != null && bo.getMaxDailyBookings() < 1) {
            throw new ServiceException("每日最大预约数至少为1");
        }
        // REQ-01：三类资源开关值域校验（0/1）
        validateSwitch(bo.getProjectBookingEnabled(), "项目开放开关");
        validateSwitch(bo.getProjectBookingRequired(), "项目必填开关");
        validateSwitch(bo.getRoomBookingEnabled(), "场地开放开关");
        validateSwitch(bo.getRoomBookingRequired(), "场地必填开关");
        validateSwitch(bo.getStaffBookingEnabled(), "员工开放开关");
        validateSwitch(bo.getStaffBookingRequired(), "员工必填开关");
        // REQ-01：锁死行业（旅游/按摩/足浴）强制项目开启且必填，覆盖用户输入
        boolean projectLocked = isProjectLockedIndustry(currentIndustryCode());
        Integer projectEnabled = bo.getProjectBookingEnabled();
        Integer projectRequired = bo.getProjectBookingRequired();
        if (projectLocked) {
            projectEnabled = 1;
            projectRequired = 1;
        }

        // upsert
        LambdaQueryWrapper<BookingPolicy> wrapper = Wrappers.<BookingPolicy>lambdaQuery()
            .last("LIMIT 1");
        BookingPolicy exist = policyMapper.selectOne(wrapper);
        if (exist != null) {
            BookingPolicy entity = new BookingPolicy();
            entity.setId(exist.getId());
            if (bo.getCustomerSelfBooking() != null) entity.setCustomerSelfBooking(bo.getCustomerSelfBooking());
            if (bo.getAdvanceBookingDays() != null) entity.setAdvanceBookingDays(bo.getAdvanceBookingDays());
            if (bo.getCancelPolicy() != null) entity.setCancelPolicy(bo.getCancelPolicy());
            if (bo.getCancelFreeHours() != null) entity.setCancelFreeHours(bo.getCancelFreeHours());
            if (bo.getNoShowThreshold() != null) entity.setNoShowThreshold(bo.getNoShowThreshold());
            if (bo.getAutoConfirm() != null) entity.setAutoConfirm(bo.getAutoConfirm());
            entity.setMaxDailyBookings(bo.getMaxDailyBookings());
            if (bo.getProjectBookingEnabled() != null || projectLocked) entity.setProjectBookingEnabled(projectEnabled);
            if (bo.getProjectBookingRequired() != null || projectLocked) entity.setProjectBookingRequired(projectRequired);
            if (bo.getRoomBookingEnabled() != null) entity.setRoomBookingEnabled(bo.getRoomBookingEnabled());
            if (bo.getRoomBookingRequired() != null) entity.setRoomBookingRequired(bo.getRoomBookingRequired());
            if (bo.getStaffBookingEnabled() != null) entity.setStaffBookingEnabled(bo.getStaffBookingEnabled());
            if (bo.getStaffBookingRequired() != null) entity.setStaffBookingRequired(bo.getStaffBookingRequired());
            entity.setRemark(bo.getRemark());
            policyMapper.updateById(entity);
            log.info("[savePolicy] UPDATE id={}", exist.getId());
            return exist.getId();
        } else {
            BookingPolicy entity = new BookingPolicy();
            entity.setCustomerSelfBooking(bo.getCustomerSelfBooking() != null ? bo.getCustomerSelfBooking() : 1);
            entity.setAdvanceBookingDays(bo.getAdvanceBookingDays() != null ? bo.getAdvanceBookingDays() : 7);
            entity.setCancelPolicy(bo.getCancelPolicy() != null ? bo.getCancelPolicy() : "FREE");
            entity.setCancelFreeHours(bo.getCancelFreeHours() != null ? bo.getCancelFreeHours() : 2);
            entity.setNoShowThreshold(bo.getNoShowThreshold() != null ? bo.getNoShowThreshold() : 3);
            entity.setAutoConfirm(bo.getAutoConfirm() != null ? bo.getAutoConfirm() : 0);
            entity.setMaxDailyBookings(bo.getMaxDailyBookings());
            entity.setProjectBookingEnabled(projectEnabled != null ? projectEnabled : 1);
            entity.setProjectBookingRequired(projectRequired != null ? projectRequired : 0);
            entity.setRoomBookingEnabled(bo.getRoomBookingEnabled() != null ? bo.getRoomBookingEnabled() : 1);
            entity.setRoomBookingRequired(bo.getRoomBookingRequired() != null ? bo.getRoomBookingRequired() : 0);
            entity.setStaffBookingEnabled(bo.getStaffBookingEnabled() != null ? bo.getStaffBookingEnabled() : 1);
            entity.setStaffBookingRequired(bo.getStaffBookingRequired() != null ? bo.getStaffBookingRequired() : 0);
            entity.setRemark(bo.getRemark());
            policyMapper.insert(entity);
            log.info("[savePolicy] INSERT id={}", entity.getId());
            return entity.getId();
        }
    }

    /** REQ-01：资源开关值域校验（仅允许 null/0/1） */
    private void validateSwitch(Integer value, String label) {
        if (value != null && value != 0 && value != 1) {
            throw new ServiceException(label + "取值不合法（仅允许 0 或 1）");
        }
    }
}
