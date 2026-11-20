package com.ruoyi.common.helper;

import java.util.function.Supplier;

/**
 * 租户上下文助手（T15.21 @TenantJob 多租户机制）
 * <p>
 * 背景：{@link LoginHelper#getTenantId()} 依赖 Sa-Token 请求态存储，定时任务 / 异步线程
 * 无登录态时取不到租户，多租户插件因而不拼接 tenant_id（跨租户）。本助手提供一个
 * 线程级（ThreadLocal）显式租户上下文，供 XXL-JOB {@code @TenantJob} 切面在逐个租户
 * 执行前 set、finally clear，使租户级 SQL 自动隔离生效。
 * <p>
 * 设计约束（对齐开发规范第 14 条）：
 * <ul>
 *   <li>Web 请求线程从不 set 本上下文，{@link LoginHelper#getTenantId()} 优先读本上下文、
 *       为空再回落 Sa-Token，故对既有 Web 行为零影响；</li>
 *   <li>异步 / 线程池场景禁止依赖 ThreadLocal 继承，必须用 {@link #execute(Long, Runnable)}
 *       或 {@link #supply(Long, Supplier)} 显式传递 tenantId；</li>
 *   <li>使用后必须在 finally 中 {@link #clear()}，防止线程复用导致上下文串租户。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public class TenantHelper {

    private TenantHelper() {
    }

    /**
     * 线程级租户上下文（不使用可继承 ThreadLocal，异步须显式传递）
     */
    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前线程租户上下文
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        CONTEXT.set(tenantId);
    }

    /**
     * 获取当前线程租户上下文（未设置返回 null）
     */
    public static Long getTenantId() {
        return CONTEXT.get();
    }

    /**
     * 是否已显式设置租户上下文
     */
    public static boolean isSet() {
        return CONTEXT.get() != null;
    }

    /**
     * 清除当前线程租户上下文（必须在 finally 调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 在指定租户上下文中执行一段逻辑，执行完毕恢复原上下文（无返回值）
     *
     * @param tenantId 目标租户ID
     * @param runnable 业务逻辑
     */
    public static void execute(Long tenantId, Runnable runnable) {
        Long previous = CONTEXT.get();
        try {
            CONTEXT.set(tenantId);
            runnable.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * 在指定租户上下文中执行一段逻辑，执行完毕恢复原上下文（有返回值）
     *
     * @param tenantId 目标租户ID
     * @param supplier 业务逻辑
     * @return 业务返回值
     */
    public static <T> T supply(Long tenantId, Supplier<T> supplier) {
        Long previous = CONTEXT.get();
        try {
            CONTEXT.set(tenantId);
            return supplier.get();
        } finally {
            restore(previous);
        }
    }

    /**
     * 恢复上一层上下文（支持嵌套调用），previous 为空则彻底清除
     */
    private static void restore(Long previous) {
        if (previous != null) {
            CONTEXT.set(previous);
        } else {
            CONTEXT.remove();
        }
    }
}
