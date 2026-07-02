package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BudgetValidationRuleVo;
import com.ruoyi.system.domain.vo.ValidationResultVo;

import java.util.List;
import java.util.Map;

/**
 * 预算科目规则校验Service接口
 *
 * @author ruoyi
 * @date 2026-07-02
 */
public interface IBudgetValidationRuleService {

    /**
     * 查询所有启用的校验规则
     */
    List<BudgetValidationRuleVo> listActiveRules();

    /**
     * 执行规则校验
     *
     * @param details 预算明细列表（每个元素包含 subjectCode, subjectName, subjectType, budgetAmount）
     * @return 校验结果列表
     */
    List<ValidationResultVo> executeValidation(List<Map<String, Object>> details);
}
