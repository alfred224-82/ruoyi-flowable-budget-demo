package com.ruoyi.job.service;

import com.ruoyi.system.service.IFeeNotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 老板每日简报任务（T5.12）
 * <p>
 * XXL-JOB 配置：
 * - bossDailyBriefingJob: cron = "0 0 9 * * ?"（每日 09:00）
 * <p>
 * 聚合 6 类待办计数生成每日简报，限频：当日已发送则跳过（老板每日 ≤1 条）。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeBossDailyBriefingJob {

    private final IFeeNotificationService notificationService;

    @XxlJob("bossDailyBriefingJob")
    public void execute() {
        XxlJobHelper.log("开始生成老板每日简报");
        int sent = notificationService.sendBossDailyBriefing();
        XxlJobHelper.log("老板每日简报处理完成，发送 {} 条（0=当日已发送被限频跳过）", sent);
        log.info("[FeeBossDailyBriefing] 简报处理完成, sent={}", sent);
    }
}
