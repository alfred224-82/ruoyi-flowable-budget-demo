package com.ruoyi.web.controller.fee;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.BudgetAdjustmentAppBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentAppVo;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.service.IBudgetAdjustmentAppService;
import com.ruoyi.system.service.IFundPoolConfigService;
import com.ruoyi.system.service.IReservedBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 预算调整申请Controller
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/budgetAdjustment")
public class BudgetAdjustmentAppController extends BaseController {

    private final IBudgetAdjustmentAppService budgetAdjustmentAppService;
    private final IReservedBudgetService reservedBudgetService;
    private final IFundPoolConfigService fundPoolConfigService;

    /**
     * 提交预算调整申请
     */
    @RepeatSubmit
    @Log(title = "预算调整申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public R<BudgetAdjustmentAppVo> apply(@Validated(AddGroup.class) @RequestBody BudgetAdjustmentAppBo bo) {
        return R.ok(budgetAdjustmentAppService.apply(bo));
    }

    /**
     * 查询申请列表（超预算审批页 & 预算调整申请页共用，无需权限校验）
     */
    @GetMapping("/list")
    public TableDataInfo<BudgetAdjustmentAppVo> list(BudgetAdjustmentAppBo bo, PageQuery pageQuery) {
        Page<BudgetAdjustmentAppVo> page = budgetAdjustmentAppService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询申请详情（超预算审批页 & 预算调整申请页共用，无需权限校验）
     */
    @GetMapping("/{id}")
    public R<BudgetAdjustmentAppVo> getInfo(@PathVariable Long id) {
        return R.ok(budgetAdjustmentAppService.queryById(id));
    }

    /**
     * 同意分摊
     * - 部门审批：按差额分摊到科目预算
     * - 分公司审批：分摊金额到申请部门的部门资金池
     * - 总公司审批：分摊金额到申请分公司的分公司资金池
     */
    @RepeatSubmit
    @Log(title = "预算调整审批", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id,
                           @RequestParam(required = false) BigDecimal allocateAmount,
                           @RequestParam(required = false) String opinion) {
        return toAjax(budgetAdjustmentAppService.approve(id, allocateAmount, opinion));
    }

    /**
     * 驳回申请
     */
    @RepeatSubmit
    @Log(title = "预算调整审批", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        return toAjax(budgetAdjustmentAppService.reject(id, reason));
    }

    /**
     * 向上级转审
     */
    @RepeatSubmit
    @Log(title = "预算调整审批", businessType = BusinessType.UPDATE)
    @PostMapping("/escalate/{id}")
    public R<Void> escalate(@PathVariable Long id, @RequestParam String reason) {
        return toAjax(budgetAdjustmentAppService.escalate(id, reason));
    }

    /**
     * 待我审批列表
     */
    @GetMapping("/pendingList")
    public TableDataInfo<BudgetAdjustmentAppVo> pendingList(PageQuery pageQuery) {
        return TableDataInfo.build(budgetAdjustmentAppService.pendingList(pageQuery));
    }

    /**
     * 查询指定科目的预算余额（当前用户部门）
     */
    @GetMapping("/subjectBudget/{subjectCode}")
    public R<BigDecimal> subjectBudget(@PathVariable String subjectCode) {
        return R.ok(budgetAdjustmentAppService.getSubjectBudget(subjectCode));
    }

    /**
     * 获取当前用户的资金池（预留预算）信息
     */
    @GetMapping("/myFundPool")
    public R<ReservedBudget> myFundPool() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String budgetPeriod = cal.get(java.util.Calendar.YEAR) + "-" + String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1);
        ReservedBudget reserved = reservedBudgetService.getMyReservedBudget(budgetPeriod);
        if (reserved == null) {
            // 无记录时返回默认资金池额度（从配置表读取）
            reserved = new ReservedBudget();
            reserved.setBudgetPeriod(budgetPeriod);
            // 根据当前用户角色判断层级并从配置表读取额度
            java.util.Set<String> roleKeys = com.ruoyi.common.helper.LoginHelper.getLoginUser().getRolePermission();
            String roleKey = (roleKeys != null && !roleKeys.isEmpty()) ? roleKeys.iterator().next() : "";
            String levelType = fundPoolConfigService.getLevelTypeByRoleKey(roleKey);
            reserved.setLevelType(levelType);
            reserved.setTotalAmount(fundPoolConfigService.getInitialAmount(levelType));
            reserved.setAllocatedAmount(java.math.BigDecimal.ZERO);
            reserved.setAvailableAmount(reserved.getTotalAmount());
            return R.ok(reserved);
        }
        // 计算剩余可用额度
        reserved.setAvailableAmount(reserved.getTotalAmount().subtract(reserved.getAllocatedAmount()));
        return R.ok(reserved);
    }

    /**
     * 撤回申请（仅申请人本人可操作，仅审批中的可撤回）
     */
    @PostMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(budgetAdjustmentAppService.cancel(id));
    }

    /**
     * 删除申请单（未审批通过的可删除）
     */
    @Log(title = "预算调整申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(budgetAdjustmentAppService.deleteByIds(java.util.Arrays.asList(ids)));
    }
}
