package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.excel.ExcelResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.BatchApprovalResultVo;
import com.ruoyi.system.domain.vo.ExpenseBillImportVo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.service.IExpenseApprovalService;
import com.ruoyi.system.service.IExpenseBillService;
import com.ruoyi.system.service.IProfitTipService;
import com.ruoyi.system.service.ISubsidyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 报销单管理Controller（V5.4增强版）
 * 新增端点：补助试算、盈利提示、汇率查询
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/expense")
public class ExpenseBillController extends BaseController {

    private final IExpenseBillService expenseBillService;
    private final IExpenseApprovalService expenseApprovalService;

    /**
     * 查询报销单列表
     */
    @SaCheckPermission("fee:expense:list")
    @GetMapping("/list")
    public TableDataInfo<ExpenseBillVo> list(ExpenseBillBo bo, PageQuery pageQuery) {
        Page<ExpenseBillVo> page = expenseBillService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询待审批报销单列表（部门经理/上级查看，无需权限注解；T6.7 支持证据等级筛选）
     */
    @GetMapping("/pendingApproval")
    public TableDataInfo<ExpenseBillVo> pendingApproval(PageQuery pageQuery,
                                                        @RequestParam(required = false) String evidenceLevel) {
        Page<ExpenseBillVo> page = expenseApprovalService.selectPendingApprovalPage(pageQuery, evidenceLevel);
        return TableDataInfo.build(page);
    }

    /**
     * 获取报销单详细信息（查看模式，无需权限）
     */
    @GetMapping("/{id}")
    public R<ExpenseBillVo> getInfo(@PathVariable Long id) {
        return R.ok(expenseBillService.queryById(id));
    }

    /**
     * 新增报销单（创建即提交，无需权限）
     * V5.4增强：支持多币种、补助、合规校验
     */
    @Log(title = "报销单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<ExpenseBill> add(@Validated(AddGroup.class) @RequestBody ExpenseBillBo bo) {
        return R.ok(expenseBillService.insertByBo(bo));
    }

    /**
     * 部门经理审批通过
     */
    @RepeatSubmit
    @Log(title = "报销单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id, @RequestParam(required = false) String opinion) {
        return toAjax(expenseApprovalService.approve(id, opinion));
    }

    /**
     * 部门经理驳回
     */
    @RepeatSubmit
    @Log(title = "报销单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam String reason,
                          @RequestParam(required = false) String opinion) {
        return toAjax(expenseApprovalService.reject(id, reason, opinion));
    }

    /**
     * 批量审批通过（T6.7 / FEAT_EVIDENCE_CHAIN，逐条独立事务，返回成功数+失败明细）
     */
    @RequireFeature("FEAT_EVIDENCE_CHAIN")
    @SaCheckPermission("fee:expense:batchApprove")
    @Log(title = "报销单批量审批", businessType = BusinessType.UPDATE)
    @PostMapping("/batchApprove")
    public R<BatchApprovalResultVo> batchApprove(@RequestBody List<Long> billIds,
                                                 @RequestParam(required = false) String opinion) {
        return R.ok(expenseApprovalService.batchApprove(billIds, opinion));
    }

    /**
     * 批量驳回（T6.7 / FEAT_EVIDENCE_CHAIN，逐条独立事务，返回成功数+失败明细）
     */
    @RequireFeature("FEAT_EVIDENCE_CHAIN")
    @SaCheckPermission("fee:expense:batchReject")
    @Log(title = "报销单批量驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/batchReject")
    public R<BatchApprovalResultVo> batchReject(@RequestBody List<Long> billIds,
                                                @RequestParam String reason,
                                                @RequestParam(required = false) String opinion) {
        return R.ok(expenseApprovalService.batchReject(billIds, reason, opinion));
    }

    // ========== V5.4 新增端点 ==========

    /**
     * 试算补助金额
     */
    @GetMapping("/subsidy/calculate")
    public R<ISubsidyService.SubsidyCalcResult> calculateSubsidy(
            @RequestParam String subsidyType,
            @RequestParam int days) {
        return R.ok(expenseBillService.calculateSubsidy(subsidyType, days));
    }

    /**
     * 获取盈利提示
     */
    @GetMapping("/profitTip")
    public R<IProfitTipService.ProfitTipResult> getProfitTip(
            @RequestParam String budgetSubjectCode,
            @RequestParam(required = false) BigDecimal amount) {
        return R.ok(expenseBillService.getProfitTip(budgetSubjectCode, amount));
    }

    /**
     * 获取汇率
     */
    @GetMapping("/exchangeRate")
    public R<BigDecimal> getExchangeRate(
            @RequestParam String currencyCode,
            @RequestParam String period) {
        return R.ok(expenseBillService.getExchangeRate(currencyCode, period));
    }

    // ========== 导入功能 ==========

    /**
     * 导入报销单数据
     */
    @Log(title = "报销单导入", businessType = BusinessType.IMPORT)
    @SaCheckPermission("fee:expense:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<ExpenseBillImportVo> excelResult = ExcelUtil.importExcel(
            file.getInputStream(), ExpenseBillImportVo.class, true);
        String message = expenseBillService.importBills(excelResult.getList());
        return R.ok(message);
    }

    /**
     * 下载导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "报销单导入模板", ExpenseBillImportVo.class, response);
    }
}
