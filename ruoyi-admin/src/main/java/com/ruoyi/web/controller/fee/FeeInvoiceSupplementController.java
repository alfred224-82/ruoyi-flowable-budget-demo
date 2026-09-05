package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeeInvoiceSupplementVo;
import com.ruoyi.system.service.IFeeInvoiceSupplementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 补票记录Controller（T5.8 无发票后补通道）
 * 注：菜单无专属权限码，复用所属报销模块权限标识（fee:expense:*）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/invoiceSupplement")
@RequireFeature("FEAT_INVOICE_SUPPLEMENT")
public class FeeInvoiceSupplementController extends BaseController {

    private final IFeeInvoiceSupplementService supplementService;

    /**
     * 补票记录列表
     */
    @SaCheckPermission("fee:expense:list")
    @GetMapping("/list")
    public TableDataInfo<FeeInvoiceSupplementVo> list(
        @RequestParam(required = false) String status,
        PageQuery pageQuery) {
        return supplementService.queryPageList(status, pageQuery);
    }

    /**
     * 补票记录详情
     */
    @SaCheckPermission("fee:expense:list")
    @GetMapping("/{id}")
    public R<FeeInvoiceSupplementVo> getInfo(@PathVariable Long id) {
        return R.ok(supplementService.queryById(id));
    }

    /**
     * 创建补票记录（报销单勾选后补时调用，额度预占；与报销单新增一致，登录即可）
     */
    @Log(title = "补票申请", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@RequestParam Long billId,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date estimatedDate) {
        return R.ok(supplementService.createSupplement(billId, estimatedDate));
    }

    /**
     * 财务审核（后补单理由必填）
     */
    @SaCheckPermission("fee:expense:list")
    @Log(title = "补票审核", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{id}")
    public R<Void> audit(@PathVariable Long id, @RequestParam String auditReason) {
        return toAjax(supplementService.auditSupplement(id, auditReason));
    }

    /**
     * 补传发票（预占转实扣）
     */
    @SaCheckPermission("fee:expense:list")
    @Log(title = "发票补传", businessType = BusinessType.UPDATE)
    @PutMapping("/supplement/{id}")
    public R<Void> supplement(@PathVariable Long id, @RequestParam String invoiceUrls) {
        return toAjax(supplementService.supplementInvoice(id, invoiceUrls));
    }

    /**
     * 手动扫描超时未补记录并释放（定时任务兜底）
     */
    @SaCheckPermission("fee:expense:list")
    @Log(title = "补票超时扫描", businessType = BusinessType.UPDATE)
    @PutMapping("/scanOverdue")
    public R<Integer> scanOverdue() {
        return R.ok(supplementService.scanAndReleaseOverdue());
    }
}
