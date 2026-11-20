package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.IComplianceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin 合规告警看板 Controller（M18 T18.5，platform_admin 专属）
 * <p>类级 {@code @SaCheckRole("platform_admin")}；双开关任一启用即可访问，故不加 {@code @RequireFeature}
 *   （看板聚合的是历史命中日志，与实时开关状态解耦）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/admin/alert")
@SaCheckRole("platform_admin")
public class AdminComplianceAlertController extends BaseController {

    private final IComplianceDashboardService dashboardService;

    /** 看板总览（命中总数/FAIL 数/通过率） */
    @SaCheckPermission("compliance:alert:dashboard")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.dashboard(days));
    }

    /** 命中趋势（按日） */
    @SaCheckPermission("compliance:alert:dashboard")
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.trend(days));
    }

    /** 命中词 TOP10 */
    @SaCheckPermission("compliance:alert:dashboard")
    @GetMapping("/top10")
    public R<List<Map<String, Object>>> top10(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.top10(days));
    }

    /** 高风险租户排行 */
    @SaCheckPermission("compliance:alert:dashboard")
    @GetMapping("/highRiskTenants")
    public R<List<Map<String, Object>>> highRiskTenants(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.highRiskTenants(days));
    }
}
