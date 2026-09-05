package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeContractLedger;
import com.ruoyi.system.mapper.FeeContractLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合同台账Controller
 * 设计文档 Phase 6 Task 6.6
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/contract")
public class ContractLedgerController extends BaseController {

    private final FeeContractLedgerMapper contractLedgerMapper;

    /**
     * 合同台账列表
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:contract:list")
    @GetMapping("/list")
    public TableDataInfo<FeeContractLedger> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String contractType,
            PageQuery pageQuery) {
        Page<FeeContractLedger> page = contractLedgerMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeContractLedger>lambdaQuery()
                .eq(status != null, FeeContractLedger::getStatus, status)
                .eq(deptId != null, FeeContractLedger::getDeptId, deptId)
                .eq(contractType != null, FeeContractLedger::getContractType, contractType)
                .orderByDesc(FeeContractLedger::getCreateTime)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 合同详情
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:contract:query")
    @GetMapping("/{id}")
    public R<FeeContractLedger> getInfo(@PathVariable Long id) {
        return R.ok(contractLedgerMapper.selectById(id));
    }

    /**
     * 新增合同
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:contract:add")
    @Log(title = "合同台账", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FeeContractLedger contract) {
        // 自动计算月摊销额 = 合同总额 / 摊销月数
        if (contract.getMonthlyAmount() == null && contract.getTotalAmount() != null && contract.getAmortizeMonths() != null) {
            contract.setMonthlyAmount(contract.getTotalAmount()
                .divide(new BigDecimal(contract.getAmortizeMonths()), 2, RoundingMode.HALF_UP));
        }
        if (contract.getAccumulatedAmount() == null) {
            contract.setAccumulatedAmount(BigDecimal.ZERO);
        }
        if (contract.getStatus() == null) {
            contract.setStatus("ACTIVE");
        }
        return toAjax(contractLedgerMapper.insert(contract) > 0);
    }

    /**
     * 修改合同
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:contract:edit")
    @Log(title = "合同台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FeeContractLedger contract) {
        return toAjax(contractLedgerMapper.updateById(contract) > 0);
    }

    /**
     * 终止合同
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:contract:edit")
    @Log(title = "合同终止", businessType = BusinessType.UPDATE)
    @PostMapping("/terminate/{id}")
    public R<Void> terminate(@PathVariable Long id) {
        FeeContractLedger contract = contractLedgerMapper.selectById(id);
        if (contract == null) {
            return R.fail("合同不存在");
        }
        contract.setStatus("TERMINATED");
        return toAjax(contractLedgerMapper.updateById(contract) > 0);
    }
}
