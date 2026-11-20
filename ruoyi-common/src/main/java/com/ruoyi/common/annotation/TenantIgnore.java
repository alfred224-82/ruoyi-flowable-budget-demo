package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 平台级任务标记（T15.21）
 * <p>
 * 标注在 XXL-JOB Handler 方法上，声明该任务为平台级（跨租户 / 无租户语义），
 * 切面据此在执行前 {@code TenantHelper.clear()}，避免误拼 tenant_id。
 * 与 {@link TenantJob} 互斥：平台级任务标 {@code @TenantIgnore}，租户级任务标 {@code @TenantJob}。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantIgnore {
}
