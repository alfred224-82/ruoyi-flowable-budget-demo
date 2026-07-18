package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BudgetCheckResultVo;

import java.math.BigDecimal;

/**
 * 预算校验Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IBudgetCheckService {

    /**
     * 校验预算
     *
     * @param deptId            部门ID
     * @param budgetSubjectCode 预算科目编码
     * @param amount            申请金额
     * @param budgetYear        预算年度
     * @param budgetMonth       预算月份
     * @return 校验结果
     */
    BudgetCheckResultVo checkBudget(Long deptId, String budgetSubjectCode, BigDecimal amount,
                                     Integer budgetYear, Integer budgetMonth);
}
