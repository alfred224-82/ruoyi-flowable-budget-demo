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
import com.ruoyi.system.domain.FeeAccrualRecord;
import com.ruoyi.system.service.IAccrualService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 计提记录Controller
 * 设计文档 Phase 6 Task 6.6
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/accrual")
public class AccrualRecordController extends BaseController {

    private final IAccrualService accrualService;

    /**
     * 计提记录列表
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:accrual:list")
    @GetMapping("/list")
    public TableDataInfo<FeeAccrualRecord> list(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String sourceType,
            PageQuery pageQuery) {
        Page<FeeAccrualRecord> page = accrualService.listRecords(period, sourceType, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 财务复核确认凭证
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:accrual:confirm")
    @Log(title = "计提确认", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm/{id}")
    public R<Void> confirm(@PathVariable Long id) {
        return toAjax(accrualService.confirmVoucher(id));
    }

    /**
     * 作废计提记录
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:accrual:void")
    @Log(title = "计提作废", businessType = BusinessType.UPDATE)
    @PostMapping("/void/{id}")
    public R<Void> voidRecord(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(accrualService.voidVoucher(id, reason));
    }

    /**
     * 手动触发月度计提
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:accrual:execute")
    @Log(title = "手动计提", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public R<Integer> executeMonthly(@RequestParam String period) {
        return R.ok(accrualService.executeMonthlyAccrual(period));
    }
}
