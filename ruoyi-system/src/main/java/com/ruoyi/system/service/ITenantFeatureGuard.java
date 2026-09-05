package com.ruoyi.system.service;

import java.util.Set;

/**
 * 租户级功能开关校验（T6.8 ③ / T4.4 定时任务开关前置校验）
 * <p>定时任务无登录态，租户拦截器不过滤，批处理方法需全量扫描后按"租户版本默认功能集"
 * 逐租户筛选：未开通对应开关的（基础版）租户跳过，保证「基础版租户无执行记录」（防串验收）。
 * <p>功能集来源以 {@code feature_flag} 表驱动优先、{@code EditionFeatureConfig} 常量兜底（T10.3）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ITenantFeatureGuard {

    /**
     * 返回已开通指定功能开关的正常状态租户ID集合
     *
     * @param featureCode 开关编码（如 FEAT_HIDDEN_COST）
     * @return 已开通该开关的租户ID集合（不含停用/过期租户）
     */
    Set<Long> enabledTenantIds(String featureCode);

    /**
     * 指定租户是否已开通该功能开关
     *
     * @param tenantId    租户ID（为空视为未开通）
     * @param featureCode 开关编码
     * @return true=已开通
     */
    boolean isEnabled(Long tenantId, String featureCode);
}
