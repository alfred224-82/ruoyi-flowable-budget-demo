package com.ruoyi.job.service;

import com.ruoyi.system.service.IAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 周期计提定时任务
 * 设计文档 Phase 6 Task 6.5
 *
 * 每月1日凌晨2点执行上月计提（资产折旧+合同摊销）
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Component("accrualTask")
@RequiredArgsConstructor
public class AccrualTask {

    private final IAccrualService accrualService;

    /**
     * 执行月度计提（每月1日凌晨执行上月）
     */
    public void executeMonthly() {
        String period = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("[AccrualTask] 开始执行月度计提任务, period={}", period);
        try {
            int count = accrualService.executeMonthlyAccrual(period);
            log.info("[AccrualTask] 月度计提完成, period={}, 计提{}笔", period, count);
        } catch (Exception e) {
            log.error("[AccrualTask] 月度计提异常, period={}", period, e);
        }
    }

    /**
     * 手动指定期间执行计提
     */
    public void executeByPeriod(String period) {
        log.info("[AccrualTask] 手动执行计提, period={}", period);
        try {
            int count = accrualService.executeMonthlyAccrual(period);
            log.info("[AccrualTask] 计提完成, period={}, 计提{}笔", period, count);
        } catch (Exception e) {
            log.error("[AccrualTask] 计提异常, period={}", period, e);
        }
    }
}
