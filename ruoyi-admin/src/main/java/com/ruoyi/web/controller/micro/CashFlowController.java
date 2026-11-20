package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.CashFlowBo;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.vo.CashFlowVo;
import com.ruoyi.system.service.ICashFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收支记录Controller（T15.14，老板 PC）
 * <p>收入流水/手动补录收入 + 支出流水/手动补录支出（工资/房租/水电/采购...）。
 * <p>★纯记账口径（非支付流水）；收入/支出列表强制按 flow_type 过滤，避免越权查看另一口径。
 * perms 对齐 M15_06：micro:income:list/add、micro:expense:list/add。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/cashflow")
public class CashFlowController extends BaseController {

    private final ICashFlowService cashFlowService;

    /**
     * 收入流水列表
     */
    @SaCheckPermission("micro:income:list")
    @GetMapping("/income/list")
    public TableDataInfo<CashFlowVo> incomeList(CashFlowBo bo, PageQuery pageQuery) {
        bo.setFlowType(CashFlowType.INCOME.getCode());
        return cashFlowService.queryPageList(bo, pageQuery);
    }

    /**
     * 手动补录收入
     */
    @SaCheckPermission("micro:income:add")
    @Log(title = "补录收入", businessType = BusinessType.INSERT)
    @PostMapping("/income")
    public R<Long> addIncome(@Validated(AddGroup.class) @RequestBody CashFlowBo bo) {
        bo.setFlowType(CashFlowType.INCOME.getCode());
        bo.setRelatedType(CashFlowType.RELATED_MANUAL);
        return R.ok(cashFlowService.insert(bo));
    }

    /**
     * 支出流水列表（线下已付记账：工资/房租/水电）
     */
    @SaCheckPermission("micro:expense:list")
    @GetMapping("/expense/list")
    public TableDataInfo<CashFlowVo> expenseList(CashFlowBo bo, PageQuery pageQuery) {
        bo.setFlowType(CashFlowType.EXPENSE.getCode());
        return cashFlowService.queryPageList(bo, pageQuery);
    }

    /**
     * 手动补录支出
     */
    @SaCheckPermission("micro:expense:add")
    @Log(title = "补录支出", businessType = BusinessType.INSERT)
    @PostMapping("/expense")
    public R<Long> addExpense(@Validated(AddGroup.class) @RequestBody CashFlowBo bo) {
        bo.setFlowType(CashFlowType.EXPENSE.getCode());
        bo.setRelatedType(CashFlowType.RELATED_MANUAL);
        return R.ok(cashFlowService.insert(bo));
    }
}
