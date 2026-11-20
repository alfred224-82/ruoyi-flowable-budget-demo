package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

/**
 * 合规告警看板服务（M18 T18.5，聚合 sensitive_word_hit_log）
 * <p>Admin 跨租户聚合；租户查询走标准租户隔离自动过滤。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IComplianceDashboardService {

    /** 看板总览：命中总数/FAIL 数/通过率/涉及租户数等 */
    Map<String, Object> dashboard(int days);

    /** 命中趋势（按日） */
    List<Map<String, Object>> trend(int days);

    /** 命中词 TOP10 */
    List<Map<String, Object>> top10(int days);

    /** 高风险租户排行（仅 Admin） */
    List<Map<String, Object>> highRiskTenants(int days);
}
