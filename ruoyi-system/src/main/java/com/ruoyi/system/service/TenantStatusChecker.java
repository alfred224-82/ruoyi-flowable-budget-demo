package com.ruoyi.system.service;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.service.TenantChecker;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.SysLicense;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysLicenseMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

/**
 * 租户状态校验器（T3.4 租户到期检测与拦截核心组件）
 * <p>
 * 职责：
 * 1. 登录时校验：到期/停用/待启用租户的用户禁止登录（{@code SysLoginService} 调用）；
 * 2. 全局接口拦截：已登录会话在租户到期/停用后访问任意业务接口被拦截（{@code SaTokenConfig} 调用）；
 * 3. 被动检测兜底：即使每日检测任务（tenantExpireCheckJob）尚未运行，
 *    发现 status=NORMAL 但 expire_date 已过，也立即纠正状态为 EXPIRED 并拦截（防"过期续用"）。
 * <p>
 * 性能：状态走 Redis 缓存（5 分钟），管理员变更租户状态/到期日时发布刷新消息清缓存。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantStatusChecker implements TenantChecker {

    /** 租户状态缓存时长（分钟） */
    private static final long CACHE_MINUTES = 5;
    /** 缓存值：租户不存在 */
    private static final String STATUS_NOT_EXISTS = "-1";
    /** 租户状态：正常 */
    private static final String STATUS_NORMAL = "NORMAL";
    /** 租户状态：到期 */
    private static final String STATUS_EXPIRED = "EXPIRED";
    /** 租户状态：停用 */
    private static final String STATUS_DISABLED = "DISABLED";
    /** 租户状态：待启用 */
    private static final String STATUS_PENDING = "PENDING";
    /** Redis 通道：租户功能开关刷新（与 SysTenantServiceImpl 共用，状态变更时清缓存） */
    private static final String TENANT_FEATURE_REFRESH_CHANNEL = "tenant:feature:refresh";

    private final SysTenantMapper tenantMapper;
    private final SysLicenseMapper licenseMapper;

    /**
     * 校验当前登录用户所属租户是否可访问（全局接口拦截入口）
     * <p>
     * admin 超管与未绑定租户的平台账号豁免（任务文档 0.2 admin 豁免策略）。
     */
    @Override
    public void checkCurrentTenant() {
        // admin 超管豁免
        if (LoginHelper.isAdmin()) {
            return;
        }
        Long tenantId = LoginHelper.getTenantId();
        // 未绑定租户的平台级账号豁免
        if (ObjectUtil.isNull(tenantId)) {
            return;
        }
        checkTenant(tenantId);
    }

    /**
     * 校验指定租户是否处于可用状态，不可用时抛出业务异常
     *
     * @param tenantId 租户ID
     */
    public void checkTenant(Long tenantId) {
        String status = getStatus(tenantId);
        if (STATUS_NOT_EXISTS.equals(status)) {
            throw new ServiceException("租户不存在，禁止访问");
        }
        if (STATUS_EXPIRED.equals(status)) {
            throw new ServiceException("租户授权已到期，请联系平台管理员续费");
        }
        if (STATUS_DISABLED.equals(status)) {
            throw new ServiceException("租户已被停用，请联系平台管理员");
        }
        if (STATUS_PENDING.equals(status)) {
            throw new ServiceException("租户尚未启用，请联系平台管理员");
        }
    }

    /**
     * 获取租户状态（带缓存 + 被动到期检测）
     * <p>
     * 缓存命中直接返回；未命中查库并被动检测到期：
     * status=NORMAL 但 expire_date 已过 → 纠正为 EXPIRED 并写回（幂等）。
     */
    private String getStatus(Long tenantId) {
        String cacheKey = CacheConstants.TENANT_STATUS_KEY + tenantId;
        String cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            RedisUtils.setCacheObject(cacheKey, STATUS_NOT_EXISTS, Duration.ofMinutes(CACHE_MINUTES));
            return STATUS_NOT_EXISTS;
        }
        String status = tenant.getStatus();
        Date now = new Date();
        // 被动检测兜底：状态正常但到期日已过（每日检测任务尚未执行时的防线）
        if (STATUS_NORMAL.equals(status) && tenant.getExpireDate() != null
            && tenant.getExpireDate().before(now)) {
            markExpired(tenantId);
            status = STATUS_EXPIRED;
        }
        // 反向纠正：管理员已延长到期日（或设为永久），但状态仍为 EXPIRED → 恢复为 NORMAL
        if (STATUS_EXPIRED.equals(status)
            && (tenant.getExpireDate() == null || tenant.getExpireDate().after(now))) {
            markNormal(tenantId);
            status = STATUS_NORMAL;
        }
        // T3.13：被动检测 License 到期——仅触发功能降级，不影响租户状态（租户到期才禁用）
        checkLicenseExpiry(tenantId);
        RedisUtils.setCacheObject(cacheKey, status, Duration.ofMinutes(CACHE_MINUTES));
        return status;
    }

    /**
     * 将租户标记为到期（幂等：仅 NORMAL 状态生效）
     */
    private void markExpired(Long tenantId) {
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setStatus(STATUS_EXPIRED);
        update.setUpdateBy("tenant-expire-check");
        tenantMapper.updateById(update);
        evictStatusCache(tenantId);
        log.warn("[Tenant] 租户 {} 已到期，状态被动纠正为 EXPIRED", tenantId);
    }

    /**
     * 将租户恢复为正常（幂等：仅 EXPIRED 状态生效，管理员延长到期日后触发）
     */
    private void markNormal(Long tenantId) {
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setStatus(STATUS_NORMAL);
        update.setUpdateBy("tenant-expire-check");
        tenantMapper.updateById(update);
        evictStatusCache(tenantId);
        log.info("[Tenant] 租户 {} 到期日已延长，状态被动恢复为 NORMAL", tenantId);
    }

    /**
     * 清除租户状态缓存（租户状态/到期日变更后调用）
     *
     * @param tenantId 租户ID
     */
    public void evictStatusCache(Long tenantId) {
        RedisUtils.deleteObject(CacheConstants.TENANT_STATUS_KEY + tenantId);
        // 通知其他节点刷新（复用既有租户刷新通道）
        RedisUtils.publish(TENANT_FEATURE_REFRESH_CHANNEL, tenantId.toString());
    }

    /**
     * T3.13：被动检测租户 License 到期——仅触发功能降级，不影响租户状态
     * License 到期 → 标记 EXPIRED → 功能降级为基础版（租户到期才禁用登录）
     */
    private void checkLicenseExpiry(Long tenantId) {
        try {
            SysLicense license = licenseMapper.selectOne(
                Wrappers.<SysLicense>lambdaQuery()
                    .eq(SysLicense::getTenantId, tenantId)
                    .eq(SysLicense::getStatus, "ACTIVE")
                    .orderByDesc(SysLicense::getLicenseVersion)
                    .last("LIMIT 1")
            );
            if (license != null && license.getExpiresAt() != null
                && license.getExpiresAt().before(new Date())) {
                license.setStatus("EXPIRED");
                licenseMapper.updateById(license);
                log.warn("[Tenant] 租户 {} License 已到期，功能降级为基础版", tenantId);
            }
        } catch (Exception e) {
            log.error("[Tenant] License 到期检测异常，tenantId={}", tenantId, e);
        }
    }
}
