package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.AnomalyMonitorVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisSummaryVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisVo;

import java.util.List;

/**
 * 报表分析服务接口
 *
 * @author ruoyi
 */
public interface IReportAnalysisService {

    /**
     * 组织机构盈利分析表（明细视图）
     *
     * @param orgId 组织ID（null=全部）
     * @param year  年度
     * @param month 月份（null=全年汇总）
     * @return 盈利分析明细列表
     */
    List<ProfitAnalysisVo> profitAnalysisDetail(Long orgId, Integer year, Integer month);

    /**
     * 组织机构盈利分析表（汇总视图，按会计6大科目聚合）
     *
     * @param orgId 组织ID（null=全部）
     * @param year  年度
     * @param month 月份（null=全年汇总）
     * @return 盈利分析汇总列表
     */
    List<ProfitAnalysisSummaryVo> profitAnalysisSummary(Long orgId, Integer year, Integer month);

    /**
     * 预算归属异常监控表
     *
     * @param pageQuery 分页参数
     * @return 异常监控分页数据
     */
    TableDataInfo<AnomalyMonitorVo> anomalyMonitorList(PageQuery pageQuery);
}
