package com.ruoyi.web.controller.fee;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.AnomalyMonitorVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisSummaryVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisVo;
import com.ruoyi.system.service.IReportAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表分析控制器
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/report")
public class ReportAnalysisController {

    private final IReportAnalysisService reportAnalysisService;

    /**
     * 盈利分析明细视图
     */
    @GetMapping("/profitAnalysis/detail")
    public R<List<ProfitAnalysisVo>> profitAnalysisDetail(
        @RequestParam(required = false) Long orgId,
        @RequestParam Integer year,
        @RequestParam(required = false) Integer month) {
        return R.ok(reportAnalysisService.profitAnalysisDetail(orgId, year, month));
    }

    /**
     * 盈利分析汇总视图（按会计6大科目聚合）
     */
    @GetMapping("/profitAnalysis/summary")
    public R<List<ProfitAnalysisSummaryVo>> profitAnalysisSummary(
        @RequestParam(required = false) Long orgId,
        @RequestParam Integer year,
        @RequestParam(required = false) Integer month) {
        return R.ok(reportAnalysisService.profitAnalysisSummary(orgId, year, month));
    }

    /**
     * 预算归属异常监控表
     */
    @GetMapping("/anomalyMonitor")
    public TableDataInfo<AnomalyMonitorVo> anomalyMonitorList(PageQuery pageQuery) {
        return reportAnalysisService.anomalyMonitorList(pageQuery);
    }
}
