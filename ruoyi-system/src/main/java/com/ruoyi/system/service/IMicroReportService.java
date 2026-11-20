package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.ReportQueryBo;
import com.ruoyi.system.domain.vo.MicroDailyRevenueVo;
import com.ruoyi.system.domain.vo.RevenueSummaryVo;

import java.util.Date;
import java.util.List;

/**
 * 小微版经营报表服务接口（T15.5/T15.14/T15.22，老板 PC + 小程序）
 * <p>报表口径基于 cash_flow（纯记账）实时聚合；每日营收快照 micro_daily_revenue 由 microDailyRevenueJob（T15.22）
 * 或 {@link #generateDailyRevenue} 生成，用于趋势展示与历史留存。
 * <p>老板小程序（client_type='mini'）仅查看：今日概览/周报/月报/资金水位。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IMicroReportService {

    /**
     * 今日概览（实时营收/净利，从 cash_flow 当日聚合）
     */
    RevenueSummaryVo today();

    /**
     * 经营周报（近 7 天聚合）
     */
    RevenueSummaryVo weekly();

    /**
     * 经营月报（当月聚合）
     */
    RevenueSummaryVo monthly();

    /**
     * 利润报表（指定日期范围：收入-支出）
     */
    RevenueSummaryVo profit(ReportQueryBo bo);

    /**
     * 资金水位（累计结余 = 全周期 Σincome-Σexpense，附当日数据）
     */
    RevenueSummaryVo cashPosition();

    /**
     * 营收趋势（按日快照，取自 micro_daily_revenue；指定范围，默认近 30 天）
     */
    List<MicroDailyRevenueVo> trend(ReportQueryBo bo);

    /**
     * 生成/更新指定租户某日的营收快照（uk_tenant_date 幂等 upsert）。
     * <p>★供 microDailyRevenueJob（T15.22）逐租户调用，跨租户写入：显式按 tenantId 聚合 cash_flow/booking_order 并 setTenantId；
     * tenantId 为空时回落当前登录租户（老板 PC 手动补算）。
     *
     * @param tenantId 目标租户（空=当前登录租户）
     * @param date     营收日期（空=当天）
     * @return 影响条数
     */
    int generateDailyRevenue(Long tenantId, Date date);
}
