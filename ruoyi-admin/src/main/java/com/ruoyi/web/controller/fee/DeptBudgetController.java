package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.service.IBudgetPreparationService;
import com.ruoyi.system.service.IDeptBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门级预算Controller（T6.1 / FEAT_DEPT_BUDGET，高级版增量）
 * 启用 department_id 双粒度：部门编制闭环复用公司级流程；报销按提交人部门扣额度
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/deptBudget")
public class DeptBudgetController extends BaseController {

    private final IDeptBudgetService deptBudgetService;
    private final IBudgetPreparationService budgetPreparationService;

    /**
     * 部门预算编制单列表（仅部门级单据）
     */
    @RequireFeature("FEAT_DEPT_BUDGET")
    @SaCheckPermission("fee:deptBudget:list")
    @GetMapping("/list")
    public TableDataInfo<BudgetPreparationVo> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer budgetYear,
            @RequestParam(required = false) Integer budgetMonth,
            @RequestParam(required = false) String status,
            PageQuery pageQuery) {
        Page<BudgetPreparationVo> page = deptBudgetService.deptPageList(departmentId, budgetYear, budgetMonth, status, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 部门预算编制单详情
     */
    @RequireFeature("FEAT_DEPT_BUDGET")
    @SaCheckPermission("fee:deptBudget:query")
    @GetMapping("/{id}")
    public R<BudgetPreparationVo> getInfo(@PathVariable Long id) {
        return R.ok(budgetPreparationService.queryById(id));
    }

    /**
     * 部门科目额度汇总（编制额/已用/剩余）
     */
    @RequireFeature("FEAT_DEPT_BUDGET")
    @SaCheckPermission("fee:deptBudget:query")
    @GetMapping("/quota")
    public R<List<Map<String, Object>>> quota(
            @RequestParam Long departmentId,
            @RequestParam Integer budgetYear,
            @RequestParam Integer budgetMonth) {
        return R.ok(deptBudgetService.deptQuotaSummary(departmentId, budgetYear, budgetMonth));
    }

    /**
     * 创建部门预算编制单（部门级编制闭环入口）
     */
    @RequireFeature("FEAT_DEPT_BUDGET")
    @SaCheckPermission("fee:deptBudget:edit")
    @Log(title = "部门预算编制", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody BudgetPreparationBo bo) {
        return toAjax(deptBudgetService.createDeptBudget(bo));
    }
}
