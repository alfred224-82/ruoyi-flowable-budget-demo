package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeLoan;
import com.ruoyi.system.domain.vo.FeeLoanInterestVo;
import com.ruoyi.system.domain.vo.FeeLoanVo;
import com.ruoyi.system.service.IFeeLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 贷款Controller（T5.9）
 * 财务设参数，老板只读；每月利息计提
 * 注：路径 /fee/loanInterest 区别于旧版借款台账 LoanLedgerController（/fee/loan）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/loanInterest")
@RequireFeature("FEAT_HIDDEN_COST")
public class FeeLoanController extends BaseController {

    private final IFeeLoanService loanService;

    /**
     * 贷款列表
     */
    @SaCheckPermission("fee:loan:list")
    @GetMapping("/list")
    public TableDataInfo<FeeLoanVo> list(
        @RequestParam(required = false) String status,
        PageQuery pageQuery) {
        return loanService.queryPageList(status, pageQuery);
    }

    /**
     * 新增/修改贷款
     */
    @SaCheckPermission("fee:loan:edit")
    @Log(title = "贷款维护", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Long> save(@RequestBody FeeLoan loan) {
        return R.ok(loanService.saveLoan(loan));
    }

    /**
     * 结清贷款（停止计息）
     */
    @SaCheckPermission("fee:loan:edit")
    @Log(title = "贷款结清", businessType = BusinessType.UPDATE)
    @PutMapping("/settle/{id}")
    public R<Void> settle(@PathVariable Long id) {
        return toAjax(loanService.settleLoan(id));
    }

    /**
     * 利息计提记录列表
     */
    @SaCheckPermission("fee:loan:query")
    @GetMapping("/interest/list")
    public TableDataInfo<FeeLoanInterestVo> interestList(
        @RequestParam(required = false) String accrualMonth,
        PageQuery pageQuery) {
        return loanService.queryInterestPageList(accrualMonth, pageQuery);
    }

    /**
     * 手动触发月度利息计提（幂等）
     */
    @SaCheckPermission("fee:loan:edit")
    @Log(title = "利息计提", businessType = BusinessType.UPDATE)
    @PutMapping("/accrue")
    public R<Integer> accrue(@RequestParam(required = false) String accrualMonth) {
        return R.ok(loanService.accrueInterests(accrualMonth));
    }
}
