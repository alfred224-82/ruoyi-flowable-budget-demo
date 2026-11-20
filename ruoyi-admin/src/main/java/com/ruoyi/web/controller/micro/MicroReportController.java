package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.bo.CashFlowBo;
import com.ruoyi.system.domain.bo.ReportQueryBo;
import com.ruoyi.system.domain.vo.CashFlowVo;
import com.ruoyi.system.domain.vo.MicroDailyRevenueVo;
import com.ruoyi.system.domain.vo.RevenueSummaryVo;
import com.ruoyi.system.service.ICashFlowService;
import com.ruoyi.system.service.IMicroReportService;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营报表Controller（T15.5/T15.14，老板 PC + 老板小程序 mini 只读）
 * <p>报表口径基于 cash_flow（纯记账）实时聚合：今日概览/周报/月报/利润/资金水位；
 * 营收趋势取自每日快照 micro_daily_revenue；收支明细支持导出。
 * <p>★V1 零支付合规：所有数据均为记账口径，无任何支付流水。
 * <p>perms 对齐 M15_06：micro:report:weekly/monthly/profit/export/cashFlow；
 * 今日概览与营收趋势归 micro:report:weekly（老板小程序 mini boss 报表 7201 复用同权限）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/report")
public class MicroReportController extends BaseController {

    private final IMicroReportService microReportService;
    private final ICashFlowService cashFlowService;
    private final IMonthlyFixedCostService fixedCostService;

    /**
     * 今日概览（实时营收/净利，从 cash_flow 当日聚合）
     */
    @SaCheckPermission("micro:report:weekly")
    @GetMapping("/today")
    public R<RevenueSummaryVo> today() {
        return R.ok(microReportService.today());
    }

    /**
     * 经营周报（近 7 天聚合）
     */
    @SaCheckPermission("micro:report:weekly")
    @GetMapping("/weekly")
    public R<RevenueSummaryVo> weekly() {
        return R.ok(microReportService.weekly());
    }

    /**
     * 经营月报（当月聚合）
     */
    @SaCheckPermission("micro:report:monthly")
    @GetMapping("/monthly")
    public R<RevenueSummaryVo> monthly() {
        return R.ok(microReportService.monthly());
    }

    /**
     * 利润报表（指定日期范围：收入-支出）
     */
    @SaCheckPermission("micro:report:profit")
    @PostMapping("/profit")
    public R<RevenueSummaryVo> profit(@RequestBody ReportQueryBo bo) {
        return R.ok(microReportService.profit(bo));
    }

    /**
     * 资金水位（累计结余 = 全周期 Σincome-Σexpense，附当日数据）
     */
    @SaCheckPermission("micro:report:cashFlow")
    @GetMapping("/cashFlow")
    public R<RevenueSummaryVo> cashPosition() {
        return R.ok(microReportService.cashPosition());
    }

    /**
     * 营收趋势（按日快照，取自 micro_daily_revenue；指定范围，默认近 30 天）
     */
    @SaCheckPermission("micro:report:weekly")
    @PostMapping("/trend")
    public R<List<MicroDailyRevenueVo>> trend(@RequestBody ReportQueryBo bo) {
        return R.ok(microReportService.trend(bo));
    }

    /**
     * 导出收支明细（cash_flow 记账流水）
     */
    @SaCheckPermission("micro:report:export")
    @Log(title = "收支明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(CashFlowBo bo, HttpServletResponse response) {
        List<CashFlowVo> list = cashFlowService.queryList(bo);
        ExcelUtil.exportExcel(list, "收支明细", CashFlowVo.class, response);
    }

    /**
     * ★首页盈亏三指标聚合（T15.37：本月收入/支出/盈利）
     * <p>支出 = cash_flow 流水支出 + monthly_fixed_cost 本月合计
     * <p>仅 micro_pro 显示（lite 档前端隐藏，后端不强制拦截）。
     */
    @SaCheckPermission("micro:report:weekly")
    @GetMapping("/profitSummary")
    public R<Map<String, Object>> profitSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        // 本月收入（cash_flow 当月 income 合计）
        RevenueSummaryVo monthly = microReportService.monthly();
        BigDecimal income = monthly != null && monthly.getGrossIncome() != null ? monthly.getGrossIncome() : BigDecimal.ZERO;
        result.put("income", income);
        // 本月支出（cash_flow 当月 expense + monthly_fixed_cost 本月合计）
        BigDecimal cashExpense = monthly != null && monthly.getTotalExpense() != null ? monthly.getTotalExpense() : BigDecimal.ZERO;
        BigDecimal fixedCostSum = fixedCostService.sumCurrentMonth();
        BigDecimal totalExpense = cashExpense.add(fixedCostSum);
        result.put("expense", totalExpense);
        result.put("cashExpense", cashExpense);
        result.put("fixedCostSum", fixedCostSum);
        // 盈利 = 收入 - 支出
        result.put("profit", income.subtract(totalExpense));
        return R.ok(result);
    }
}
