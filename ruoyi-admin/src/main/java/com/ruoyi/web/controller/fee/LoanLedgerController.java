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
import com.ruoyi.system.domain.FeeLoanLedger;
import com.ruoyi.system.service.ILoanLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 借款台账Controller
 * 设计文档 Phase 7 Task 7.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/loan")
public class LoanLedgerController extends BaseController {

    private final ILoanLedgerService loanLedgerService;

    /**
     * 借款列表
     */
    @RequireFeature("FEE_LOAN_LEDGER")
    @SaCheckPermission("fee:loan:list")
    @GetMapping("/list")
    public TableDataInfo<FeeLoanLedger> list(
            @RequestParam(required = false) String status, PageQuery pageQuery) {
        Page<FeeLoanLedger> page = loanLedgerService.listLoans(status, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 新增借款
     */
    @RequireFeature("FEE_LOAN_LEDGER")
    @SaCheckPermission("fee:loan:add")
    @Log(title = "借款台账", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FeeLoanLedger loan) {
        return toAjax(loanLedgerService.addLoan(loan));
    }

    /**
     * 结清借款
     */
    @RequireFeature("FEE_LOAN_LEDGER")
    @SaCheckPermission("fee:loan:edit")
    @Log(title = "借款结清", businessType = BusinessType.UPDATE)
    @PostMapping("/settle/{id}")
    public R<Void> settle(@PathVariable Long id) {
        return toAjax(loanLedgerService.settle(id));
    }

    /**
     * 手动触发利息计提
     */
    @RequireFeature("FEE_LOAN_LEDGER")
    @SaCheckPermission("fee:loan:execute")
    @Log(title = "利息计提", businessType = BusinessType.OTHER)
    @PostMapping("/interest/execute")
    public R<Integer> executeInterest(@RequestParam String period) {
        return R.ok(loanLedgerService.executeMonthlyInterest(period));
    }

    /**
     * 逾期检测
     */
    @RequireFeature("FEE_LOAN_LEDGER")
    @SaCheckPermission("fee:loan:edit")
    @PostMapping("/checkOverdue")
    public R<Void> checkOverdue() {
        loanLedgerService.checkOverdue();
        return R.ok();
    }
}
