package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeStaffSalary;
import com.ruoyi.system.domain.vo.FeeSalaryRecordVo;
import com.ruoyi.system.domain.vo.FeeStaffSalaryVo;
import com.ruoyi.system.service.IFeeStaffSalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 员工薪资Controller（T5.9，公司级）
 * 薪资社保基数维护 + 每月计提
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/salary")
@RequireFeature("FEAT_HIDDEN_COST")
public class FeeStaffSalaryController extends BaseController {

    private final IFeeStaffSalaryService salaryService;

    /**
     * 薪资基数列表
     */
    @SaCheckPermission("fee:staffSalary:list")
    @GetMapping("/list")
    public TableDataInfo<FeeStaffSalaryVo> list(
        @RequestParam(required = false) String status,
        PageQuery pageQuery) {
        return salaryService.queryPageList(status, pageQuery);
    }

    /**
     * 新增/修改薪资基数（userId 唯一）
     */
    @SaCheckPermission("fee:staffSalary:edit")
    @Log(title = "薪资基数", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Long> save(@RequestBody FeeStaffSalary salary) {
        return R.ok(salaryService.saveSalary(salary));
    }

    /**
     * 离职（停止计提）
     */
    @SaCheckPermission("fee:staffSalary:edit")
    @Log(title = "员工离职", businessType = BusinessType.UPDATE)
    @PutMapping("/resign/{id}")
    public R<Void> resign(@PathVariable Long id) {
        return toAjax(salaryService.resign(id));
    }

    /**
     * 计提记录列表
     */
    @SaCheckPermission("fee:staffSalary:query")
    @GetMapping("/record/list")
    public TableDataInfo<FeeSalaryRecordVo> recordList(
        @RequestParam(required = false) String accrualMonth,
        PageQuery pageQuery) {
        return salaryService.queryRecordPageList(accrualMonth, pageQuery);
    }

    /**
     * 手动触发月度薪资计提（幂等）
     */
    @SaCheckPermission("fee:staffSalary:edit")
    @Log(title = "薪资计提", businessType = BusinessType.UPDATE)
    @PutMapping("/accrue")
    public R<Integer> accrue(@RequestParam(required = false) String accrualMonth) {
        return R.ok(salaryService.accrueSalaries(accrualMonth));
    }
}
