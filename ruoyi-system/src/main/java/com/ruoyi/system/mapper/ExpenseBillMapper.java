package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.vo.AnomalyMonitorVo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisSummaryVo;
import com.ruoyi.system.domain.vo.ProfitAnalysisVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

/**
 * 报销单Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface ExpenseBillMapper extends BaseMapperPlus<ExpenseBillMapper, ExpenseBill, ExpenseBillVo> {

    /**
     * 查询已审批通过的报销金额合计
     *
     * @param deptId            部门ID
     * @param budgetSubjectCode 预算科目编码
     * @return 合计金额
     */
    BigDecimal sumApprovedAmountByDeptAndSubject(@Param("deptId") Long deptId,
                                                  @Param("budgetSubjectCode") String budgetSubjectCode);

    /**
     * 更新报销单状态
     *
     * @param id     主键ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 盈利分析明细视图
     *
     * @param orgId 组织ID（null=全部）
     * @param year  年度
     * @param month 月份（null=全年汇总）
     * @return 分析结果列表
     */
    List<ProfitAnalysisVo> selectProfitAnalysisDetail(@Param("orgId") Long orgId,
                                                       @Param("year") Integer year,
                                                       @Param("month") Integer month);

    /**
     * 盈利分析汇总视图（按会计6大科目聚合）
     *
     * @param orgId 组织ID（null=全部）
     * @param year  年度
     * @param month 月份（null=全年汇总）
     * @return 汇总结果列表
     */
    List<ProfitAnalysisSummaryVo> selectProfitAnalysisSummary(@Param("orgId") Long orgId,
                                                               @Param("year") Integer year,
                                                               @Param("month") Integer month);

    /**
     * 异常监控表查询（分页）
     *
     * @param page 分页参数
     * @return 异常单据分页数据
     */
    Page<AnomalyMonitorVo> selectAnomalyMonitor(Page<AnomalyMonitorVo> page);

    /**
     * 报销单分页列表（含用途标签名称、预算科目名称）
     *
     * @param page    分页参数
     * @param wrapper 查询条件
     * @return 报销单VO分页数据
     */
    Page<ExpenseBillVo> selectExpenseBillPage(Page<ExpenseBillVo> page, @Param("ew") Wrapper<ExpenseBill> wrapper);

    /**
     * 按科目汇总查询指定部门、月份已审批通过的报销金额
     *
     * @param deptId 部门ID
     * @param year   年份
     * @param month  月份
     * @return 每行包含 subjectCode / subjectName / subjectType / totalAmount
     */
    List<Map<String, Object>> sumApprovedExpenseGroupBySubject(@Param("deptId") Long deptId,
                                                                @Param("year") Integer year,
                                                                @Param("month") Integer month);
}
