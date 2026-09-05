package com.ruoyi.job.service;

import com.ruoyi.system.service.IEmployeeCreditService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 员工信用分等级重算任务（T6.3 / FEAT_CREDIT_SCORE）
 * <p>
 * XXL-JOB 配置：
 * - creditScoreCalcJob: cron = "0 0 2 ? * MON"（每周一 02:00，见 M6_02 脚本）
 * <p>
 * 跨租户扫描信用记录重算等级；执行前按租户版本校验开关，
 * 基础版租户跳过（防串验收用例 9：无执行记录）。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditScoreCalcJob {

    private final IEmployeeCreditService creditService;

    @XxlJob("creditScoreCalcJob")
    public void execute() {
        XxlJobHelper.log("开始执行员工信用分等级重算（仅高级版+租户）");
        int count = creditService.recalcLevels();
        XxlJobHelper.log("信用分等级重算完成，更新 {} 条", count);
        log.info("[CreditScore] 等级重算完成, count={}", count);
    }
}
