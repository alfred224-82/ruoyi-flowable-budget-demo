package com.ruoyi.job.service;

import com.ruoyi.system.service.IFeeInvoiceSupplementService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 补票超时释放任务（T5.8）
 * <p>
 * XXL-JOB 配置：
 * - invoiceSupplementOverdueJob: cron = "0 40 0 * * ?"（每日 00:40）
 * <p>
 * 扫描超过预计补票日期仍未补票的记录 → 预占释放 + 异常标记（状态=OVERDUE）。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeInvoiceSupplementOverdueJob {

    private final IFeeInvoiceSupplementService supplementService;

    @XxlJob("invoiceSupplementOverdueJob")
    public void execute() {
        XxlJobHelper.log("开始扫描超时未补票记录");
        int count = supplementService.scanAndReleaseOverdue();
        XxlJobHelper.log("补票超时扫描完成，释放 {} 条", count);
        log.info("[FeeInvoiceSupplement] 超时扫描完成, count={}", count);
    }
}
