package com.ruoyi.job.service;

import com.ruoyi.system.service.ICrossDeptAllocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 跨部门分摊定时任务
 * 设计文档 Phase 11 Task 11.3
 *
 * 每月最后一天23点执行
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Component("crossDeptAllocTask")
@RequiredArgsConstructor
public class CrossDeptAllocTask {

    private final ICrossDeptAllocService crossDeptAllocService;

    /**
     * 每月最后一天执行跨部门分摊
     * 默认分摊虚拟部门ID=999（待分摊池）
     */
    public void executeMonthly() {
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("[CrossDeptAllocTask] 开始执行月度跨部门分摊, period={}", period);
        try {
            int count = crossDeptAllocService.executeAllocation(period, 999L, "HEADCOUNT");
            log.info("[CrossDeptAllocTask] 分摊完成, period={}, 共{}笔", period, count);
        } catch (Exception e) {
            log.error("[CrossDeptAllocTask] 分摊异常, period={}", period, e);
        }
    }
}
