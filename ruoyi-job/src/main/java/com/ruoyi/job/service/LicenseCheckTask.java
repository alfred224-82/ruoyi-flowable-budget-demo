package com.ruoyi.job.service;

import com.ruoyi.system.service.ILicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * License定时检测任务
 * 每小时执行一次：
 * 1. 复核试用剩余时长 → 到期则即时降级
 * 2. 正式授权到期前30天 → 站内消息提醒续期
 * 3. 时钟回拨检测：当前时间 < lastCheckTime → 试用立即到期
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Component("licenseCheckTask")
@RequiredArgsConstructor
public class LicenseCheckTask {

    private final ILicenseService licenseService;

    /**
     * 定时执行License校验（建议每小时执行一次）
     */
    public void execute() {
        log.info("[LicenseCheck] 开始定时校验...");
        try {
            // 重新加载并校验License（内部包含有效期检测、降级逻辑）
            licenseService.loadAndVerify();
            // 检查试用状态
            long remainingHours = licenseService.getTrialRemainingHours();
            if (remainingHours == 0) {
                log.warn("[LicenseCheck] 试用已到期，系统已降级为BASIC版本");
            } else if (remainingHours > 0 && remainingHours <= 72) {
                log.warn("[LicenseCheck] 试用即将到期，剩余 {} 小时", remainingHours);
            }
            log.info("[LicenseCheck] 校验完成 | 版本={} | 功能数={}",
                licenseService.getCurrentEdition(), licenseService.getActiveFeatures().size());
        } catch (Exception e) {
            log.error("[LicenseCheck] 校验异常", e);
        }
    }
}
