package com.ruoyi.job.service;

import com.ruoyi.system.service.IFeeProjectChangeService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合同变更逾期升级预警任务（T5.5）
 * <p>
 * XXL-JOB 配置：
 * - changeOverdueWarnJob: cron = "0 50 0 * * ?"（每日 00:50）
 * <p>
 * 扫描审批通过待收款确认的变更单，30/60/90 天阶梯升级预警：
 * 30 天提醒财务 → 60 天升级通知老板+财务 → ≥90 天状态转 OVERDUE 并预警高风险。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeChangeOverdueWarnJob {

    private final IFeeProjectChangeService changeService;

    @XxlJob("changeOverdueWarnJob")
    public void execute() {
        XxlJobHelper.log("开始扫描合同变更逾期预警");
        int count = changeService.scanOverdueWarnings();
        XxlJobHelper.log("合同变更逾期扫描完成，命中预警 {} 条", count);
        log.info("[FeeChangeOverdue] 逾期扫描完成, count={}", count);
    }
}
