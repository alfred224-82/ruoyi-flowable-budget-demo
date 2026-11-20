package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.SysBillingPlanVo;
import com.ruoyi.system.service.IBillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小微版套餐管理Controller（T15.11/12，platform_admin 查看/管理小微版计费套餐）
 * <p>复用 {@link IBillingPlanService}，按 version_type 前缀 micro_ 过滤，仅展示小微版套餐。
 * <p>V1 零支付合规：仅展示/排序，不含任何支付 SDK 调用。
 * <p>perms：micro:package:list / micro:package:edit（M15_17 platform_admin 菜单区段未单独建种子，
 * 复用 micro:platform:upgrade 入口页内嵌展示，也可独立挂载）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/package")
public class MicroPackageController extends BaseController {

    private final IBillingPlanService billingPlanService;

    /**
     * 小微版套餐列表（按 version_type 过滤 micro_* 套餐，分组返回）
     *
     * @param versionType 可选：micro_lite / micro_pro；为空返回全部小微版套餐
     */
    @SaCheckPermission("micro:platform:upgrade")
    @GetMapping("/list")
    public R<Map<String, List<SysBillingPlanVo>>> list(@RequestParam(required = false) String versionType) {
        List<SysBillingPlanVo> allPlans = billingPlanService.listEnabled();
        // 过滤 micro_ 前缀套餐
        List<SysBillingPlanVo> microPlans = allPlans.stream()
            .filter(p -> p.getPlanCode() != null && p.getPlanCode().startsWith("micro_"))
            .filter(p -> versionType == null || versionType.isEmpty()
                || p.getPlanCode().startsWith("micro_" + versionType.replace("micro_", "")))
            .collect(Collectors.toList());
        // 按 version_type 分组
        Map<String, List<SysBillingPlanVo>> grouped = new LinkedHashMap<>();
        grouped.put("micro_lite", microPlans.stream()
            .filter(p -> p.getPlanCode().startsWith("micro_lite"))
            .collect(Collectors.toList()));
        grouped.put("micro_pro", microPlans.stream()
            .filter(p -> p.getPlanCode().startsWith("micro_pro"))
            .collect(Collectors.toList()));
        return R.ok(grouped);
    }

    /**
     * 小微版套餐概览（含价格/天数/描述，供版本升级页展示）
     */
    @SaCheckPermission("micro:platform:upgrade")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        List<SysBillingPlanVo> allPlans = billingPlanService.listEnabled();
        List<SysBillingPlanVo> microPlans = allPlans.stream()
            .filter(p -> p.getPlanCode() != null && p.getPlanCode().startsWith("micro_"))
            .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", microPlans.size());
        result.put("plans", microPlans);
        // V1 零支付提示
        result.put("v1Notice", "V1 版本为零支付模式，所有套餐价格仅供展示，实际续费请线下联系平台管理员。");
        return R.ok(result);
    }
}
