package com.ruoyi.framework.datasource;

import cn.hutool.core.util.StrUtil;

/**
 * 租户数据隔离上下文（T7.6 / FEAT_ISOLATED_SCHEMA 预留）
 * <p>
 * 设计说明（设计文档 4.2.6）：
 * <ul>
 *   <li>SHARED（默认）：所有租户共享同一套表，依靠 tenant_id 行级隔离（TenantLineInnerInterceptor）</li>
 *   <li>DEDICATED：大客户独立 Schema，按租户路由到独立数据源（本期仅预留，不实现完整切换）</li>
 * </ul>
 * 当前版本仅提供隔离模式常量与数据源路由 Key 的线程级持有能力，
 * 完整的动态数据源切换（AbstractRoutingDataSource 接入）在专业版后续迭代实现。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public final class TenantIsolationContextHolder {

    /** 共享表模式（默认，行级租户隔离） */
    public static final String MODE_SHARED = "SHARED";
    /** 独立Schema模式（预留，物理隔离） */
    public static final String MODE_DEDICATED = "DEDICATED";
    /** 默认数据源Key */
    public static final String DEFAULT_DATASOURCE_KEY = "master";

    private static final ThreadLocal<String> DATASOURCE_KEY = new ThreadLocal<>();

    private TenantIsolationContextHolder() {
    }

    /**
     * 设置当前线程数据源路由Key（DEDICATED 模式下按租户切换用）
     */
    public static void setDataSourceKey(String key) {
        DATASOURCE_KEY.set(key);
    }

    /**
     * 获取当前线程数据源路由Key（未设置返回默认主库）
     */
    public static String getDataSourceKey() {
        String key = DATASOURCE_KEY.get();
        return StrUtil.isBlank(key) ? DEFAULT_DATASOURCE_KEY : key;
    }

    /**
     * 清除线程级路由Key（请求结束必须调用，防止线程复用污染）
     */
    public static void clear() {
        DATASOURCE_KEY.remove();
    }

    /**
     * 判断隔离模式是否为独立Schema（预留校验入口）
     *
     * @param isolationMode sys_tenant.isolation_mode
     */
    public static boolean isDedicated(String isolationMode) {
        return MODE_DEDICATED.equalsIgnoreCase(isolationMode);
    }
}
