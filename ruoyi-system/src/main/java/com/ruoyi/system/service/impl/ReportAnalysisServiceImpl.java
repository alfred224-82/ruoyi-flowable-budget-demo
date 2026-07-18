package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.AnomalyMonitorVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisSummaryVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisVo;
import com.ruoyi.system.mapper.CostAllocationMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IReportAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报表分析服务实现
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisServiceImpl implements IReportAnalysisService {

    private final ExpenseBillMapper expenseBillMapper;
    private final CostAllocationMapper allocationMapper;

    @Override
    public List<ProfitAnalysisVo> profitAnalysisDetail(Long orgId, Integer year, Integer month) {
        log.info("查询盈利分析明细，orgId={}, year={}, month={}", orgId, year, month);
        // SQL 聚合：按组织机构 + 预算科目分组，汇总预算金额、实际金额、执行差异
        return expenseBillMapper.selectProfitAnalysisDetail(orgId, year, month);
    }

    @Override
    public List<ProfitAnalysisSummaryVo> profitAnalysisSummary(Long orgId, Integer year, Integer month) {
        log.info("查询盈利分析汇总，orgId={}, year={}, month={}", orgId, year, month);
        // SQL 聚合：按会计大类分组
        return expenseBillMapper.selectProfitAnalysisSummary(orgId, year, month);
    }

    @Override
    public TableDataInfo<AnomalyMonitorVo> anomalyMonitorList(PageQuery pageQuery) {
        log.info("查询异常监控列表，pageQuery={}", pageQuery);
        // 查询待分摊 + 人工二次确认的单据
        Page<AnomalyMonitorVo> result = expenseBillMapper.selectAnomalyMonitor(pageQuery.build());
        return TableDataInfo.build(result);
    }
}
