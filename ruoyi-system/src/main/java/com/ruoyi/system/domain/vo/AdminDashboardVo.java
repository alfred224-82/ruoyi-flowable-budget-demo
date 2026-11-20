package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Admin 平台驾驶舱首页聚合 VO（T18.5，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class AdminDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 租户大盘 =====

    /** 租户总数 */
    private Long tenantTotal;

    /** 正常状态租户数 */
    private Long tenantActive;

    /** 到期/宽限期租户数 */
    private Long tenantExpired;

    /** 按行业分布：[{industry: "xxx", count: 10}] */
    private List<Map<String, Object>> industryDistribution;

    /** 按版本档位分布：[{versionType: "micro_pro", count: 5}] */
    private List<Map<String, Object>> versionDistribution;

    /** 近 7 天新增租户趋势：[{date: "2026-09-11", count: 2}] */
    private List<Map<String, Object>> tenantTrend7d;

    // ===== 系统健康度 =====

    /** 系统状态：HEALTH / WARNING / CRITICAL */
    private String systemHealth;

    /** 到期预警租户数（30 天内到期） */
    private Long expiringSoonCount;

    /** 禁用租户数 */
    private Long disabledTenantCount;

    // ===== 全局合规风险监控 =====

    /** 近 7 天合规命中总数 */
    private Long complianceHits7d;

    /** 待处理合规告警数 */
    private Long compliancePendingCount;

    /** 合规命中按类型分布：[{hitType: "SENSITIVE_WORD", count: 5}] */
    private List<Map<String, Object>> complianceByType;

    /** 合规命中按天趋势：[{date: "2026-09-11", count: 3}] */
    private List<Map<String, Object>> complianceTrend7d;
}
