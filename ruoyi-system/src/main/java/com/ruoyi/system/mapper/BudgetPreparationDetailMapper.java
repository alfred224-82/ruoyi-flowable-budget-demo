package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 预算编制明细表Mapper接口
 *
 * @author ruoyi
 * @date 2026-06-19
 */
public interface BudgetPreparationDetailMapper extends BaseMapperPlus<BudgetPreparationDetailMapper, BudgetPreparationDetail, BudgetPreparationDetailVo> {

    /**
     * 按科目和期间汇总预算金额
     *
     * @param subjectCode 科目编码
     * @param deptId      部门ID
     * @param budgetYear  预算年度
     * @param budgetMonth 预算月份
     * @return 汇总金额
     */
    BigDecimal sumBudgetBySubjectAndPeriod(@Param("subjectCode") String subjectCode,
                                           @Param("deptId") Long deptId,
                                           @Param("budgetYear") Integer budgetYear,
                                           @Param("budgetMonth") Integer budgetMonth);

    /**
     * 追加预算额度（按部门+期间隔离）
     *
     * @param subjectCode 科目编码
     * @param deptId      部门ID（关联budget_preparation.org_id）
     * @param amount      追加金额
     * @param budgetYear  预算年度
     * @param budgetMonth 预算月份
     * @return 影响行数
     */
    int addBudgetBySubject(@Param("subjectCode") String subjectCode,
                           @Param("deptId") Long deptId,
                           @Param("amount") BigDecimal amount,
                           @Param("budgetYear") Integer budgetYear,
                           @Param("budgetMonth") Integer budgetMonth);

    /**
     * 调剂预算（从调出科目转到调入科目）
     *
     * @param fromSubject   调出科目
     * @param toSubject     调入科目
     * @param amount        调剂金额
     * @return 影响行数
     */
    int transferBudget(@Param("fromSubject") String fromSubject,
                       @Param("toSubject") String toSubject,
                       @Param("amount") BigDecimal amount);

    /**
     * 获取科目剩余预算（按部门+期间隔离）= 预算编制金额 - 已使用金额
     *
     * @param subjectCode 科目编码
     * @param deptId      部门ID
     * @param budgetYear  预算年度
     * @param budgetMonth 预算月份
     * @return 剩余预算
     */
    BigDecimal getRemainingBudgetByPeriod(@Param("subjectCode") String subjectCode,
                                          @Param("deptId") Long deptId,
                                          @Param("budgetYear") Integer budgetYear,
                                          @Param("budgetMonth") Integer budgetMonth);

    /**
     * 直接设置科目预算编制金额为目标值（调整审批通过时调用）
     *
     * @param subjectCode 科目编码
     * @param deptId      部门ID
     * @param amount      目标预算金额
     * @param budgetYear  预算年度
     * @param budgetMonth 预算月份
     * @return 影响行数
     */
    int setBudgetAmount(@Param("subjectCode") String subjectCode,
                        @Param("deptId") Long deptId,
                        @Param("amount") BigDecimal amount,
                        @Param("budgetYear") Integer budgetYear,
                        @Param("budgetMonth") Integer budgetMonth);

    /**
     * 获取科目总预算（按部门隔离）
     *
     * @param subjectCode 科目编码
     * @param deptId      部门ID（关联budget_preparation.org_id）
     * @return 总预算
     */
    BigDecimal getTotalBudget(@Param("subjectCode") String subjectCode,
                              @Param("deptId") Long deptId);
}
