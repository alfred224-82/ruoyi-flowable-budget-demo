package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.ActivityDetailQueryBo;
import com.ruoyi.system.domain.bo.MonthlyTaxBo;
import com.ruoyi.system.domain.vo.ActivityDetailReportVo;
import com.ruoyi.system.domain.vo.DailyReportVo;
import com.ruoyi.system.domain.vo.MonthlyClosingVo;
import com.ruoyi.system.domain.vo.MonthlyOperatingVo;
import com.ruoyi.system.domain.vo.ReconcileVo;

import java.util.Date;

/**
 * 小微经营核算与三张报表服务接口（M38 T38.3/T38.4 + M39 T39.1~T39.5）
 * <p>数据源：activity_settlement（单场毛利=profit）为权威，promotion_activity.end_time 决定跨月归属；
 * monthly_fixed_cost（T30.6 月分摊）+ staff_commission（人力成本）构成固定成本；monthly_operating_closing 承载税费/月结。
 * <p>三层利润模型（§20）：单场毛利 → 月毛利（Σ已结算 profit）→ 月净利（月毛利 − 固定成本合计 − 税费）。
 * <p>对账铁律（T39.5）：日报月累计毛利 = 活动明细同月毛利合计 = 月报月毛利（同源于 settlement.profit）。
 * <p>权限：三张报表 + 月报仅老板（micro_pro），员工端无入口。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
public interface IMicroOperatingReportService {

    /**
     * 月度经营报表（M38 T38.3，7.13）：月经营汇总 + 固定成本构成 + 税费/净利 + 保本线 + 对比上月。
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    MonthlyOperatingVo monthlyOperating(String month);

    /**
     * 日报（M39 T39.2，7.15）：当日汇总 + 本月进度 + 当日活动清单。
     *
     * @param date 日期，空则取当日
     */
    DailyReportVo dailyReport(Date date);

    /**
     * 活动明细表（M39 T39.1，7.14）：一行一场 + 底部固定合计行 + 未结算提醒。
     * <p>合计仅累加已结算（SUBMITTED/LOCKED）行，与月报月毛利天然一致。
     */
    ActivityDetailReportVo activityDetail(ActivityDetailQueryBo bo);

    /**
     * 三张报表对账校验（M39 T39.5）：4 条规则 + overallPassed。
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    ReconcileVo reconcile(String month);

    /**
     * 查询月度税费/月结状态（M38 T38.4）：不存在则返回默认 OPEN 视图（不落库）。
     *
     * @param month 月份（yyyy-MM）
     */
    MonthlyClosingVo getClosing(String month);

    /**
     * 保存月度税费（M38 T38.4）：MANUAL 手填 taxAmount；RATE 按税前利润 × taxRate% 估算。
     * <p>月结锁定后禁止修改。
     */
    MonthlyClosingVo saveTax(MonthlyTaxBo bo);

    /**
     * 月结锁定（M38 T38.4）：置 status=LOCKED，锁定后禁止改税费。
     *
     * @param month 月份（yyyy-MM）
     */
    MonthlyClosingVo lockMonth(String month);

    /**
     * 导出活动明细表 CSV（M39 T39.1）。
     */
    String exportActivityDetailCsv(ActivityDetailQueryBo bo);

    /**
     * 导出月度经营报表 CSV（M38 T38.3）。
     *
     * @param month 月份（yyyy-MM）
     */
    String exportMonthlyCsv(String month);
}
