package com.ruoyi.common.core.service;

/**
 * 租户状态校验接口（framework 与 system 解耦）
 * <p>
 * ruoyi-framework 不依赖 ruoyi-system（避免模块循环依赖），
 * 由 system 层 TenantStatusChecker 实现本接口，框架层按接口注入。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface TenantChecker {

    /**
     * 校验当前登录用户所属租户是否可访问
     * 不可用时抛出业务异常（admin 超管与未绑定租户的平台账号豁免）
     */
    void checkCurrentTenant();

    /**
     * 判断当前租户是否处于只读宽限期（T14.2：到期后 3 天内允许只读访问）
     *
     * @return true=宽限期内（应拦截写操作），false=非宽限期
     */
    boolean isCurrentTenantInGracePeriod();
}
