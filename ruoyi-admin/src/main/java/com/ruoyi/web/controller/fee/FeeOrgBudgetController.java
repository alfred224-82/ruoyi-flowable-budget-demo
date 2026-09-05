package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.FeeOrgBudgetVersionVo;
import com.ruoyi.system.domain.vo.FeeOrgConsolidatedVo;
import com.ruoyi.system.service.IFeeOrgBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织预算Controller（T7.1 / FEAT_MULTI_ORG + FEAT_ORG_SNAPSHOT）
 * 按组织分别编制与锁定快照、集团合并视角（数据范围：本组织+下级组织）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/orgBudget")
@RequireFeature("FEAT_MULTI_ORG")
public class FeeOrgBudgetController extends BaseController {

    private final IFeeOrgBudgetService orgBudgetService;

    /**
     * 组织维度预算编制列表
     *
     * @param orgId  组织ID（必填）
     * @param period 预算期间 yyyy-MM（可空）
     */
    @SaCheckPermission("fee:orgBudget:list")
    @GetMapping("/preparations")
    public R<List<BudgetPreparationVo>> preparations(@RequestParam Long orgId,
                                                     @RequestParam(required = false) String period) {
        return R.ok(orgBudgetService.listOrgPreparations(orgId, period));
    }

    /**
     * 集团合并视角：各组织编制汇总 + 集团合计行
     */
    @SaCheckPermission("fee:orgBudget:list")
    @GetMapping("/consolidated")
    public R<List<FeeOrgConsolidatedVo>> consolidated(@RequestParam(required = false) String period) {
        return R.ok(orgBudgetService.consolidated(period));
    }

    /**
     * 锁定组织预算快照（版本号+1不覆盖，锁定后不可变）
     */
    @RequireFeature("FEAT_ORG_SNAPSHOT")
    @SaCheckPermission("fee:orgBudget:snapshot")
    @Log(title = "组织预算快照", businessType = BusinessType.INSERT)
    @PostMapping("/snapshot")
    public R<FeeOrgBudgetVersionVo> lockSnapshot(@RequestParam Long orgId, @RequestParam String period) {
        return R.ok(orgBudgetService.lockSnapshot(orgId, period));
    }

    /**
     * 组织快照版本列表
     */
    @RequireFeature("FEAT_ORG_SNAPSHOT")
    @SaCheckPermission("fee:orgBudget:query")
    @GetMapping("/versions")
    public R<List<FeeOrgBudgetVersionVo>> versions(@RequestParam Long orgId) {
        return R.ok(orgBudgetService.listVersions(orgId));
    }

    /**
     * 快照详情
     */
    @RequireFeature("FEAT_ORG_SNAPSHOT")
    @SaCheckPermission("fee:orgBudget:query")
    @GetMapping("/version/{id}")
    public R<FeeOrgBudgetVersionVo> version(@PathVariable Long id) {
        return R.ok(orgBudgetService.getVersion(id));
    }
}
