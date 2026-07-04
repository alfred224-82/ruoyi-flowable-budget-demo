package com.ruoyi.job.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetRejectHistory;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.BudgetRejectHistoryMapper;
import com.ruoyi.system.service.IBudgetNotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 预算通知定时任务
 * <p>
 * XXL-JOB 配置：
 * - budgetPeriodStartJob: cron = "0 0 9 26 * ?"（每月26日 09:00）
 * - budgetTimeoutMonitorJob: cron = "0 0 * * * ?"（每小时执行）
 *
 * @author ruoyi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetNotificationJob {

    private final BudgetPreparationMapper preparationMapper;
    private final BudgetRejectHistoryMapper rejectHistoryMapper;
    private final IBudgetNotificationService notificationService;

    /**
     * N1: 编制期开始提醒
     * 触发时间：每月26日 09:00
     * XXL-JOB JobHandler: budgetPeriodStartJob
     */
    @XxlJob("budgetPeriodStartJob")
    public void sendPeriodStartNotify() {
        XxlJobHelper.log("开始执行编制期开始提醒任务");

        // 计算当前预算期间
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        // 编制的是下个月的预算
        int budgetMonth = month + 1;
        int budgetYear = year;
        if (budgetMonth > 12) {
            budgetMonth = 1;
            budgetYear = year + 1;
        }

        String budgetPeriod = budgetYear + "年" + budgetMonth + "月";

        // 编制期：本月26日 - 次月3日
        Date startDate = cal.getTime();
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.MONTH, 1);
        endCal.set(Calendar.DAY_OF_MONTH, 3);
        endCal.set(Calendar.HOUR_OF_DAY, 18);
        Date endDate = endCal.getTime();

        notificationService.sendPeriodStartNotify(budgetPeriod, startDate, endDate);

        XxlJobHelper.log("编制期开始提醒任务执行完成，预算期间: {}", budgetPeriod);
    }

    /**
     * N5/N6/N7: 超时监控任务
     * 触发时间：每小时执行
     * XXL-JOB JobHandler: budgetTimeoutMonitorJob
     */
    @XxlJob("budgetTimeoutMonitorJob")
    public void monitorTimeout() {
        XxlJobHelper.log("开始执行超时监控任务");
        Date now = new Date();

        // 1. 查询有截止时间的驳回记录（未超时且未处理）
        List<BudgetRejectHistory> pendingHistories = rejectHistoryMapper.selectList(
            Wrappers.<BudgetRejectHistory>lambdaQuery()
                .isNotNull(BudgetRejectHistory::getDeadlineTime)
                .isNull(BudgetRejectHistory::getHandleTime)
                .eq(BudgetRejectHistory::getIsTimeout, 0)
        );

        int remindCount = 0;
        int alertCount = 0;

        for (BudgetRejectHistory history : pendingHistories) {
            long diffMs = history.getDeadlineTime().getTime() - now.getTime();
            long diffHours = diffMs / (1000 * 60 * 60);

            if (diffHours > 4) {
                // 距离截止还有4小时以上，不处理
                continue;
            }

            // 获取预算单信息
            BudgetPreparation preparation = preparationMapper.selectById(history.getSheetId());
            if (preparation == null) {
                continue;
            }

            if (diffHours > 0) {
                // N5/N6: 即将超时（4小时内或1小时内）
                int remainingHours = (int) diffHours;
                notificationService.sendTimeoutRemind(
                    history.getSheetNo(), remainingHours, history.getDeadlineTime(), history.getSheetId());
                remindCount++;
            } else {
                // N7: 已超时
                long timeoutHours = Math.abs(diffHours);
                notificationService.sendTimeoutAlert(
                    history.getSheetNo(), (int) timeoutHours, history.getDeadlineTime(), history.getSheetId());

                // 更新 is_timeout 标记
                history.setIsTimeout(1);
                rejectHistoryMapper.updateById(history);
                alertCount++;
            }
        }

        XxlJobHelper.log("超时监控任务执行完成，发送提醒: {} 条，超时告警: {} 条", remindCount, alertCount);
    }
}
