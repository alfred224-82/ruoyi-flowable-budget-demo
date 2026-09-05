package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IFeatureFlagService;
import com.ruoyi.system.service.ITenantFeatureGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 租户级功能开关校验实现（T6.8 ③ / T4.4）
 * <p>逐租户取其版本默认功能集（表驱动优先、常量兜底），判定是否开通目标开关；
 * 供无登录态的定时任务在批处理前做"基础版租户跳过"的前置筛选。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFeatureGuardImpl implements ITenantFeatureGuard {

    private final SysTenantMapper tenantMapper;
    private final IFeatureFlagService featureFlagService;

    @Override
    public Set<Long> enabledTenantIds(String featureCode) {
        Set<Long> ids = new HashSet<>();
        if (StrUtil.isBlank(featureCode)) {
            return ids;
        }
        // Job 无登录态，sys_tenant 不受租户拦截器过滤，可全量扫描
        for (SysTenant tenant : tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery())) {
            if (tenant.getId() == null) {
                continue;
            }
            Set<String> features = featureFlagService.getEditionFeatureSet(tenant.getEdition());
            if (features.contains(featureCode)) {
                ids.add(tenant.getId());
            }
        }
        return ids;
    }

    @Override
    public boolean isEnabled(Long tenantId, String featureCode) {
        return tenantId != null && enabledTenantIds(featureCode).contains(tenantId);
    }
}
