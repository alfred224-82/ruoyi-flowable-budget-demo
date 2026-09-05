package com.ruoyi.job.service;

import com.ruoyi.system.service.IFeeAssetService;
import com.ruoyi.system.service.IFeeLoanService;
import com.ruoyi.system.service.IFeeStaffSalaryService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * M5 隐形成本月度计提任务（T5.9）
 * <p>
 * XXL-JOB 配置（建议每月1日 01:00~01:30 错峰执行，计提月份默认上一自然月）：
 * - salaryAccrualJob:      cron = "0 0 1 1 * ?"
 * - assetDepreciationJob:  cron = "0 10 1 1 * ?"
 * - loanInterestJob:       cron = "0 20 1 1 * ?"
 * <p>
 * 三类计提均为幂等（同月同主体仅一条），重复执行不会产生重复数据；
 * 也可通过对应 Controller 手动触发（参数传计提月份）。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeAccrualJob {

    private final IFeeStaffSalaryService salaryService;
    private final IFeeAssetService assetService;
    private final IFeeLoanService loanService;

    /**
     * 薪资社保计提（公司级）
     */
    @XxlJob("salaryAccrualJob")
    public void salaryAccrual() {
        String month = lastMonth();
        XxlJobHelper.log("开始执行薪资社保计提，计提月份: {}", month);
        int count = salaryService.accrueSalaries(month);
        XxlJobHelper.log("薪资社保计提完成，计提 {} 条", count);
        log.info("[FeeAccrual] 薪资计提完成, month={}, count={}", month, count);
    }

    /**
     * 资产折旧计提（含项目分摊）
     */
    @XxlJob("assetDepreciationJob")
    public void assetDepreciation() {
        String month = lastMonth();
        XxlJobHelper.log("开始执行资产折旧计提，计提月份: {}", month);
        int count = assetService.accrueDepreciation(month);
        XxlJobHelper.log("资产折旧计提完成，计提 {} 条", count);
        log.info("[FeeAccrual] 折旧计提完成, month={}, count={}", month, count);
    }

    /**
     * 贷款利息计提
     */
    @XxlJob("loanInterestJob")
    public void loanInterest() {
        String month = lastMonth();
        XxlJobHelper.log("开始执行贷款利息计提，计提月份: {}", month);
        int count = loanService.accrueInterests(month);
        XxlJobHelper.log("贷款利息计提完成，计提 {} 条", count);
        log.info("[FeeAccrual] 利息计提完成, month={}, count={}", month, count);
    }

    /**
     * 上一自然月（yyyy-MM）；支持任务参数覆盖计提月份
     */
    private String lastMonth() {
        String param = XxlJobHelper.getJobParam();
        if (param != null && !param.trim().isEmpty()) {
            return param.trim();
        }
        return LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
