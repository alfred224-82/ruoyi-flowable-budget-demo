package com.ruoyi.job.aspect;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.TenantIgnore;
import com.ruoyi.common.annotation.TenantJob;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 租户级定时任务切面（T15.21）
 * <p>
 * 拦截标注 {@link TenantJob} 的 XXL-JOB Handler 方法：查询全部活跃租户（status=NORMAL）→
 * 按 {@code versions} 过滤 → 逐个租户 {@code TenantHelper.setTenantId + MDC.put} →
 * {@code point.proceed()} → finally {@code TenantHelper.clear() + MDC.remove}。
 * <p>
 * 关键约束（对齐开发规范第 14 条）：
 * <ul>
 *   <li>一个 Handler 服务全量租户，禁止按租户在 XXL-JOB 建多条任务；</li>
 *   <li>单租户异常被 catch 隔离，不抛回 XXL-JOB，不影响其他租户；</li>
 *   <li>版本过滤大小写不敏感，{@code @TenantJob(versions={"micro"})} 仅命中 edition=MICRO 的小微版租户；</li>
 *   <li>{@code @TenantIgnore} 标记的平台级任务执行前清理上下文，避免线程复用误拼 tenant_id。</li>
 * </ul>
 * <p>
 * 注：{@code parallel} 属性为规范预留（当前 3 个小微版任务均串行执行，默认 false）；
 * 如需并行须自行保证线程安全并用 {@link TenantHelper#execute} 显式传递租户，禁止依赖 ThreadLocal 继承。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Aspect
@Order(0)
@Component
@RequiredArgsConstructor
public class TenantJobAspect {

    /** MDC 租户键（日志按租户检索，见 T15.21 验收：日志含 tenantId） */
    private static final String MDC_TENANT = "tenantId";

    private final SysTenantMapper tenantMapper;

    /**
     * 租户级任务：逐个活跃租户执行原方法，单租户异常隔离
     */
    @Around("@annotation(tenantJob)")
    public Object aroundTenantJob(ProceedingJoinPoint point, TenantJob tenantJob) {
        List<SysTenant> tenants = tenantMapper.selectList(
            Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getStatus, "NORMAL"));
        String[] versions = tenantJob.versions();
        String[] versionTypes = tenantJob.versionTypes();
        int hit = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;
        for (SysTenant tenant : tenants) {
            // ★版本(edition) 与 档位(version_type) 双重过滤（T15.32）：任一不命中则跳过
            if (!matchVersion(tenant.getEdition(), versions)
                || !matchVersion(tenant.getVersionType(), versionTypes)) {
                skipped++;
                continue;
            }
            hit++;
            Long tenantId = tenant.getId();
            try {
                TenantHelper.setTenantId(tenantId);
                MDC.put(MDC_TENANT, String.valueOf(tenantId));
                point.proceed();
                success++;
            } catch (Throwable e) {
                // 单租户异常隔离：记录后继续下一个租户，绝不抛回 XXL-JOB
                failed++;
                log.error("[TenantJob] 租户 {} 执行异常（已隔离，不影响其他租户）", tenantId, e);
                XxlJobHelper.log("租户 {} 执行异常（已隔离）：{}", tenantId, e.getMessage());
            } finally {
                TenantHelper.clear();
                MDC.remove(MDC_TENANT);
            }
        }
        XxlJobHelper.log("[TenantJob] 活跃租户 {} 个，命中 {}，成功 {}，失败 {}，版本/档位过滤跳过 {}",
            tenants.size(), hit, success, failed, skipped);
        return null;
    }

    /**
     * 平台级任务：执行前后清理租户上下文，避免误拼 tenant_id
     */
    @Around("@annotation(tenantIgnore)")
    public Object aroundTenantIgnore(ProceedingJoinPoint point, TenantIgnore tenantIgnore) throws Throwable {
        TenantHelper.clear();
        MDC.remove(MDC_TENANT);
        try {
            return point.proceed();
        } finally {
            TenantHelper.clear();
            MDC.remove(MDC_TENANT);
        }
    }

    /**
     * 版本/档位归一化匹配：allowed 为空=不限；否则按值归一化（去空格 + 小写）匹配任一。
     * <p>同时用于 edition（{@code versions}）与 version_type（{@code versionTypes}）两个维度的过滤。
     */
    private boolean matchVersion(String edition, String[] versions) {
        if (versions == null || versions.length == 0) {
            return true;
        }
        if (edition == null || edition.trim().isEmpty()) {
            return false;
        }
        String normalized = edition.trim().toLowerCase();
        for (String v : versions) {
            if (v != null && v.trim().toLowerCase().equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
