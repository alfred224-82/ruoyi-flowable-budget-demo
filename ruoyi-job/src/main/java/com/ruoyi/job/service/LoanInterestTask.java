package com.ruoyi.job.service;

import com.ruoyi.system.service.ILoanLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 借款利息计提定时任务
 * 设计文档 Phase 7 Task 7.3
 *
 * 每月1日凌晨3点执行上月利息计提
 * 每天凌晨检测逾期
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Component("loanInterestTask")
@RequiredArgsConstructor
public class LoanInterestTask {

    private final ILoanLedgerService loanLedgerService;

    /**
     * 每月1日凌晨3点执行上月利息计提
     */
    public void executeMonthly() {
        String period = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("[LoanInterestTask] 开始执行月度利息计提, period={}", period);
        try {
            int count = loanLedgerService.executeMonthlyInterest(period);
            log.info("[LoanInterestTask] 利息计提完成, period={}, 计提{}笔", period, count);
        } catch (Exception e) {
            log.error("[LoanInterestTask] 利息计提异常, period={}", period, e);
        }
    }

    /**
     * 每天凌晨检测逾期
     */
    public void checkOverdue() {
        log.info("[LoanInterestTask] 开始逾期检测...");
        try {
            loanLedgerService.checkOverdue();
            log.info("[LoanInterestTask] 逾期检测完成");
        } catch (Exception e) {
            log.error("[LoanInterestTask] 逾期检测异常", e);
        }
    }
}
