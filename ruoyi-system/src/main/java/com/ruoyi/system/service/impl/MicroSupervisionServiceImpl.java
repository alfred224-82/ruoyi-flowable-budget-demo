package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ComplaintRecord;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.ComplaintRecordVo;
import com.ruoyi.system.mapper.ComplaintRecordMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IMicroSupervisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 平台监管服务实现（T25.4，小微版 M25）
 * <p>★platform_admin 专属：跨租户查看投诉 / 停止活动 / 停止租户 / 活动留痕。
 * <p>★活动留痕数据租户不可删除，仅平台 Admin 可管理。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroSupervisionServiceImpl implements IMicroSupervisionService {

    private final ComplaintRecordMapper complaintMapper;
    private final PromotionActivityMapper activityMapper;
    private final SysTenantMapper tenantMapper;

    /** 租户状态：正常 */
    private static final String TENANT_STATUS_ACTIVE = "NORMAL";
    /** 租户状态：停止 */
    private static final String TENANT_STATUS_SUSPENDED = "DISABLED";

    @Override
    public List<ComplaintRecordVo> listAllComplaints(String channel, String status, Long tenantId) {
        // 平台 Admin 跨租户查询（complaint_record 含 tenant_id，由多租户插件自动过滤；
        // platform_admin 的 tenantId=1 平台租户，需忽略租户过滤）
        LambdaQueryWrapper<ComplaintRecord> wrapper = Wrappers.<ComplaintRecord>lambdaQuery();
        if (StrUtil.isNotBlank(channel)) {
            wrapper.eq(ComplaintRecord::getComplaintChannel, channel);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(ComplaintRecord::getStatus, status);
        }
        if (tenantId != null) {
            wrapper.eq(ComplaintRecord::getTenantId, tenantId);
        }
        wrapper.orderByDesc(ComplaintRecord::getSubmitTime);
        wrapper.last("LIMIT 500");
        List<ComplaintRecord> list = complaintMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 回填租户名称
        Set<Long> tenantIds = list.stream().map(ComplaintRecord::getTenantId).collect(Collectors.toSet());
        Map<Long, String> tenantNameMap = loadTenantNames(new ArrayList<>(tenantIds));
        return list.stream().map(entity -> {
            ComplaintRecordVo vo = new ComplaintRecordVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setTenantName(tenantNameMap.get(entity.getTenantId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopActivity(Long activityId) {
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        PromotionActivity update = new PromotionActivity();
        update.setId(activityId);
        update.setStatus("stopped");
        activityMapper.updateById(update);
        log.info("[supervision] 平台 Admin 停止活动：activityId={}, operator={}",
            activityId, LoginHelper.getNickName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopTenant(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        if (TENANT_STATUS_SUSPENDED.equals(tenant.getStatus())) {
            throw new ServiceException("租户已处于停止状态");
        }
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setStatus(TENANT_STATUS_SUSPENDED);
        tenantMapper.updateById(update);
        log.info("[supervision] 平台 Admin 停止租户：tenantId={}, operator={}",
            tenantId, LoginHelper.getNickName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeTenant(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setStatus(TENANT_STATUS_ACTIVE);
        tenantMapper.updateById(update);
        log.info("[supervision] 平台 Admin 恢复租户：tenantId={}, operator={}",
            tenantId, LoginHelper.getNickName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String issueLicense(Map<String, Object> params) {
        Long tenantId = params.get("tenantId") != null ? Long.valueOf(params.get("tenantId").toString()) : null;
        String licenseType = params.get("licenseType") != null ? params.get("licenseType").toString() : null;
        if (tenantId == null || StrUtil.isBlank(licenseType)) {
            throw new ServiceException("tenantId 和 licenseType 不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setLicenseType(licenseType);
        update.setStatus(TENANT_STATUS_ACTIVE);
        tenantMapper.updateById(update);
        log.info("[supervision] 平台 Admin 发放 license：tenantId={}, licenseType={}, operator={}",
            tenantId, licenseType, LoginHelper.getNickName());
        return "license 发放成功";
    }

    @Override
    public List<Map<String, Object>> activityTrace(Long tenantId, Integer limit) {
        // 活动留痕：查询 complaint_record 中渠道二（PLATFORM）的投诉记录作为留痕数据
        // ★租户不可删除，仅平台 Admin 可管理
        int maxLimit = (limit != null && limit > 0 && limit <= 500) ? limit : 100;
        LambdaQueryWrapper<ComplaintRecord> wrapper = Wrappers.<ComplaintRecord>lambdaQuery()
            .eq(ComplaintRecord::getComplaintChannel, "PLATFORM");
        if (tenantId != null) {
            wrapper.eq(ComplaintRecord::getTenantId, tenantId);
        }
        wrapper.orderByDesc(ComplaintRecord::getSubmitTime)
            .last("LIMIT " + maxLimit);
        List<ComplaintRecord> list = complaintMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> tenantIds = list.stream().map(ComplaintRecord::getTenantId).collect(Collectors.toSet());
        Map<Long, String> tenantNameMap = loadTenantNames(new ArrayList<>(tenantIds));
        return list.stream().map(record -> {
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("id", record.getId());
            trace.put("tenantId", record.getTenantId());
            trace.put("tenantName", tenantNameMap.get(record.getTenantId()));
            trace.put("channel", record.getComplaintChannel());
            trace.put("category", record.getCategory());
            trace.put("status", record.getStatus());
            trace.put("submitTime", record.getSubmitTime());
            trace.put("handleTime", record.getHandleTime());
            trace.put("handlerName", record.getHandlerName());
            return trace;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    private Map<Long, String> loadTenantNames(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysTenant> tenants = tenantMapper.selectBatchIds(tenantIds);
        if (tenants == null || tenants.isEmpty()) {
            return Collections.emptyMap();
        }
        return tenants.stream().collect(Collectors.toMap(SysTenant::getId, SysTenant::getCompanyName, (a, b) -> a));
    }
}
