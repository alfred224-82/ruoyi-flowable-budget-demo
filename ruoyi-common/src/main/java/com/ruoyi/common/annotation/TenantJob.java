package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 租户级定时任务注解（T15.21）
 * <p>
 * 标注在 XXL-JOB Handler 方法（{@code @XxlJob}）上，由 {@code TenantJobAspect} 切面拦截：
 * 查询全部活跃租户 → 按 {@link #versions()} 过滤 → 逐个租户
 * {@code TenantHelper.setTenantId + MDC.put} → 执行原方法 → finally 清理上下文。
 * <p>
 * 核心约束（对齐开发规范第 14 条）：
 * <ul>
 *   <li>一个 Handler 服务全量租户，禁止按租户在 XXL-JOB 建多条任务；</li>
 *   <li>单租户异常被 catch 隔离，不抛回 XXL-JOB（不影响其他租户）；</li>
 *   <li>{@code versions} 为空表示不限版本（全部活跃租户）；如 {@code {"micro"}} 仅跑小微版租户。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantJob {

    /**
     * 是否并行执行各租户（默认 false 串行；并行须自行保证线程安全与上下文显式传递）
     */
    boolean parallel() default false;

    /**
     * 版本过滤（对齐 sys_tenant.edition，大小写不敏感）：
     * 空数组 = 不限版本（全部活跃租户）；如 {"micro"} 仅小微版、{"basic","advanced"} 仅企业版基础/高级。
     */
    String[] versions() default {};

    /**
     * 版本档位过滤（T15.26/T15.32，对齐 sys_tenant.version_type，大小写不敏感）：
     * 空数组 = 不限档位；如 {"micro_pro"} 仅小微专业档。
     * <p>与 {@link #versions()} 为【与】关系：两者均非空时需同时命中。
     * ★防爽约任务 {@code @TenantJob(versions={"micro"}, versionTypes={"micro_pro"})}：
     * lite 档无预约功能，自然无爽约判定，故按 version_type 精确排除 micro_lite。
     */
    String[] versionTypes() default {};
}
