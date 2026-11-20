package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ActivityDetailQueryBo;
import com.ruoyi.system.domain.bo.MonthlyTaxBo;
import com.ruoyi.system.domain.vo.ActivityDetailReportVo;
import com.ruoyi.system.domain.vo.DailyReportVo;
import com.ruoyi.system.domain.vo.MonthlyClosingVo;
import com.ruoyi.system.domain.vo.MonthlyOperatingVo;
import com.ruoyi.system.domain.vo.ReconcileVo;
import com.ruoyi.system.service.IMicroOperatingReportService;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 小微经营核算与三张报表Controller（M38 T38.3/T38.4 + M39 T39.1~T39.5）
 * <p>数据源为 activity_settlement（结算口径），仅老板（micro_pro）可见，员工端无入口。
 * <p>perms：
 * <ul>
 *   <li>micro:report:operating     — 月度经营报表 / 月结状态查询</li>
 *   <li>micro:report:daily         — 日报</li>
 *   <li>micro:report:activityDetail— 活动明细表</li>
 *   <li>micro:report:reconcile     — 对账校验</li>
 *   <li>micro:report:closing       — 税费保存 / 月结锁定</li>
 *   <li>micro:report:export        — 导出 CSV</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/operating")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroOperatingReportController extends BaseController {

    private final IMicroOperatingReportService reportService;

    /**
     * ★T38.3 月度经营报表（月经营汇总 + 固定成本构成 + 税费/净利 + 保本线 + 对比上月）
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    @SaCheckPermission("micro:report:operating")
    @GetMapping("/monthly")
    public R<MonthlyOperatingVo> monthly(@RequestParam(required = false) String month) {
        return R.ok(reportService.monthlyOperating(month));
    }

    /**
     * ★T39.2 日报（当日汇总 + 本月进度 + 当日活动清单）
     *
     * @param date 日期（yyyy-MM-dd），空则取当日
     */
    @SaCheckPermission("micro:report:daily")
    @GetMapping("/daily")
    public R<DailyReportVo> daily(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        return R.ok(reportService.dailyReport(date));
    }

    /**
     * ★T39.1 活动明细表（一行一场 + 底部固定合计行 + 未结算提醒）
     */
    @SaCheckPermission("micro:report:activityDetail")
    @GetMapping("/activityDetail")
    public R<ActivityDetailReportVo> activityDetail(ActivityDetailQueryBo bo) {
        return R.ok(reportService.activityDetail(bo));
    }

    /**
     * ★T39.5 三张报表对账校验（4 条规则 + overallPassed）
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    @SaCheckPermission("micro:report:reconcile")
    @GetMapping("/reconcile")
    public R<ReconcileVo> reconcile(@RequestParam(required = false) String month) {
        return R.ok(reportService.reconcile(month));
    }

    /**
     * ★T38.4 月度税费 / 月结状态查询
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    @SaCheckPermission("micro:report:operating")
    @GetMapping("/closing")
    public R<MonthlyClosingVo> closing(@RequestParam(required = false) String month) {
        return R.ok(reportService.getClosing(month));
    }

    /**
     * ★T38.4 保存月度税费（MANUAL 手填 / RATE 按率估算；月结锁定后禁止修改）
     */
    @SaCheckPermission("micro:report:closing")
    @Log(title = "月度经营月结", businessType = BusinessType.UPDATE)
    @PostMapping("/tax")
    public R<MonthlyClosingVo> saveTax(@Validated @RequestBody MonthlyTaxBo bo) {
        return R.ok(reportService.saveTax(bo));
    }

    /**
     * ★T38.4 月结锁定（锁定后禁止改税费）
     *
     * @param month 月份（yyyy-MM）
     */
    @SaCheckPermission("micro:report:closing")
    @Log(title = "月度经营月结", businessType = BusinessType.UPDATE)
    @PostMapping("/lock/{month}")
    public R<MonthlyClosingVo> lock(@PathVariable String month) {
        return R.ok(reportService.lockMonth(month));
    }

    /**
     * ★T39.1 导出活动明细表 CSV（BOM 头，Excel 中文不乱码）
     */
    @SaCheckPermission("micro:report:export")
    @Log(title = "活动明细表", businessType = BusinessType.EXPORT)
    @GetMapping("/export/activityDetail")
    public void exportActivityDetail(ActivityDetailQueryBo bo, HttpServletResponse response) throws IOException {
        String csv = reportService.exportActivityDetailCsv(bo);
        writeCsv(response, "activity_detail.csv", csv);
    }

    /**
     * ★T38.3 导出月度经营报表 CSV
     *
     * @param month 月份（yyyy-MM），空则取当月
     */
    @SaCheckPermission("micro:report:export")
    @Log(title = "月度经营报表", businessType = BusinessType.EXPORT)
    @GetMapping("/export/monthly")
    public void exportMonthly(@RequestParam(required = false) String month,
                              HttpServletResponse response) throws IOException {
        String csv = reportService.exportMonthlyCsv(month);
        writeCsv(response, "monthly_operating.csv", csv);
    }

    /** CSV 输出（含 BOM 头） */
    private void writeCsv(HttpServletResponse response, String filename, String csv) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8);
        writer.write(csv);
        writer.flush();
    }
}
