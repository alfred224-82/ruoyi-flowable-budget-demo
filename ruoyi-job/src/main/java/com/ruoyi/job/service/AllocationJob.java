package com.ruoyi.job.service;

import com.ruoyi.system.service.IAllocationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 费用分摊定时任务
 * <p>
 * XXL-JOB 配置：
 * - allocationMonthlyJob: cron = "0 0 2 1 * ?"（每月1日 02:00）
 *
 * @author ruoyi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationJob {

    private final IAllocationService allocationService;

    /**
     * 月度费用分摊任务
     * 触发时间：每月1日 02:00
     * XXL-JOB JobHandler: allocationMonthlyJob
     */
    @XxlJob("allocationMonthlyJob")
    public void executeMonthlyAllocation() {
        XxlJobHelper.log("开始执行月度费用分摊任务");

        try {
            // 获取当前年月
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            XxlJobHelper.log("执行 {}年{}月 的费用分摊", year, month);

            // 执行分摊逻辑
            allocationService.executeMonthlyAllocation(year, month);

            XxlJobHelper.log("月度费用分摊任务执行完成");
        } catch (Exception e) {
            XxlJobHelper.log("月度费用分摊任务执行失败: {}", e.getMessage());
            log.error("月度费用分摊任务执行失败", e);
            throw e;
        }
    }
}
