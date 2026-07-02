package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.BudgetValidationRule;
import com.ruoyi.system.domain.vo.BudgetValidationRuleVo;
import com.ruoyi.system.domain.vo.ValidationResultVo;
import com.ruoyi.system.mapper.BudgetValidationRuleMapper;
import com.ruoyi.system.service.IBudgetValidationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算科目规则校验Service实现
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Service
@RequiredArgsConstructor
public class BudgetValidationRuleServiceImpl implements IBudgetValidationRuleService {

    private final BudgetValidationRuleMapper baseMapper;

    @Override
    public List<BudgetValidationRuleVo> listActiveRules() {
        return baseMapper.selectVoList(
            Wrappers.<BudgetValidationRule>lambdaQuery()
                .eq(BudgetValidationRule::getIsActive, 1)
                .orderByAsc(BudgetValidationRule::getSortOrder)
        );
    }

    @Override
    public List<ValidationResultVo> executeValidation(List<Map<String, Object>> details) {
        List<BudgetValidationRuleVo> rules = listActiveRules();
        List<ValidationResultVo> results = new ArrayList<>();

        // 按科目类型分组汇总
        Map<String, BigDecimal> typeTotals = new HashMap<>();
        for (Map<String, Object> detail : details) {
            String type = (String) detail.get("subjectType");
            BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
            typeTotals.merge(type, amount, BigDecimal::add);
        }

        // 按科目编码汇总（用于比例校验）
        Map<String, BigDecimal> codeAmounts = new HashMap<>();
        for (Map<String, Object> detail : details) {
            String code = (String) detail.get("subjectCode");
            BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
            codeAmounts.merge(code, amount, BigDecimal::add);
        }

        for (BudgetValidationRuleVo rule : rules) {
            List<ValidationResultVo> ruleResults = evaluateRule(rule, details, typeTotals, codeAmounts);
            results.addAll(ruleResults);
        }

        return results;
    }

    /**
     * 执行单条规则校验
     */
    private List<ValidationResultVo> evaluateRule(BudgetValidationRuleVo rule,
                                                   List<Map<String, Object>> details,
                                                   Map<String, BigDecimal> typeTotals,
                                                   Map<String, BigDecimal> codeAmounts) {
        List<ValidationResultVo> results = new ArrayList<>();

        switch (rule.getRuleType()) {
            case "REQUIRED":
                results.addAll(evaluateRequired(rule, details));
                break;
            case "MIN_AMOUNT":
                results.addAll(evaluateMinAmount(rule, details));
                break;
            case "MAX_AMOUNT":
                results.addAll(evaluateMaxAmount(rule, details));
                break;
            case "RATIO_LIMIT":
                results.addAll(evaluateRatioLimit(rule, details, typeTotals, codeAmounts));
                break;
            case "SUM_CHECK":
                results.addAll(evaluateSumCheck(rule, typeTotals));
                break;
            case "FORMULA":
                results.addAll(evaluateFormula(rule, details));
                break;
            default:
                break;
        }

        return results;
    }

    /**
     * 必填校验：检查所有叶子科目是否填写了金额
     */
    private List<ValidationResultVo> evaluateRequired(BudgetValidationRuleVo rule,
                                                       List<Map<String, Object>> details) {
        List<ValidationResultVo> results = new ArrayList<>();
        for (Map<String, Object> detail : details) {
            BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
            // 只检查叶子节点（isLeaf=1的科目，前端传入的明细都是叶子节点）
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                ValidationResultVo result = buildResult(rule, detail, false);
                result.setMessage("科目[" + detail.get("subjectName") + "]未填写预算金额");
                results.add(result);
            }
        }
        // 如果全部通过，返回一条通过记录
        if (results.isEmpty()) {
            ValidationResultVo pass = new ValidationResultVo();
            pass.setRuleCode(rule.getRuleCode());
            pass.setRuleName(rule.getRuleName());
            pass.setSeverityLevel(rule.getSeverityLevel());
            pass.setPassed(true);
            pass.setMessage("所有科目已填写预算金额");
            results.add(pass);
        }
        return results;
    }

    /**
     * 最低金额校验
     */
    private List<ValidationResultVo> evaluateMinAmount(BudgetValidationRuleVo rule,
                                                        List<Map<String, Object>> details) {
        List<ValidationResultVo> results = new ArrayList<>();
        BigDecimal minAmount = rule.getThresholdValue() != null ? rule.getThresholdValue() : BigDecimal.ZERO;

        for (Map<String, Object> detail : details) {
            String type = (String) detail.get("subjectType");
            if (rule.getSubjectType() != null && !rule.getSubjectType().equals(type)) {
                continue;
            }
            BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
            if (amount != null && amount.compareTo(minAmount) < 0) {
                ValidationResultVo result = buildResult(rule, detail, false);
                result.setMessage(rule.getErrorMessage());
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            results.add(buildPassResult(rule));
        }
        return results;
    }

    /**
     * 最高金额校验
     */
    private List<ValidationResultVo> evaluateMaxAmount(BudgetValidationRuleVo rule,
                                                        List<Map<String, Object>> details) {
        List<ValidationResultVo> results = new ArrayList<>();
        BigDecimal maxAmount = rule.getThresholdValue();
        if (maxAmount == null) {
            return Collections.singletonList(buildPassResult(rule));
        }

        for (Map<String, Object> detail : details) {
            String type = (String) detail.get("subjectType");
            if (rule.getSubjectType() != null && !rule.getSubjectType().equals(type)) {
                continue;
            }
            BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
            if (amount != null && amount.compareTo(maxAmount) > 0) {
                ValidationResultVo result = buildResult(rule, detail, false);
                result.setMessage(rule.getErrorMessage());
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            results.add(buildPassResult(rule));
        }
        return results;
    }

    /**
     * 比例限制校验
     */
    private List<ValidationResultVo> evaluateRatioLimit(BudgetValidationRuleVo rule,
                                                         List<Map<String, Object>> details,
                                                         Map<String, BigDecimal> typeTotals,
                                                         Map<String, BigDecimal> codeAmounts) {
        List<ValidationResultVo> results = new ArrayList<>();

        // 获取目标科目金额
        String targetCode = rule.getSubjectCode();
        BigDecimal subjectAmount = codeAmounts.getOrDefault(targetCode, BigDecimal.ZERO);

        // 获取所属类型的总额
        String subjectType = rule.getSubjectType();
        BigDecimal totalAmount = typeTotals.getOrDefault(subjectType, BigDecimal.ZERO);

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            results.add(buildPassResult(rule));
            return results;
        }

        // 计算比例
        BigDecimal ratio = subjectAmount.divide(totalAmount, 4, RoundingMode.HALF_UP);

        // 从 ruleExpression 中提取阈值，如 "subject_sum / type_total <= 0.4"
        BigDecimal threshold = extractThresholdFromExpression(rule.getRuleExpression());
        if (threshold != null && ratio.compareTo(threshold) > 0) {
            ValidationResultVo result = new ValidationResultVo();
            result.setRuleCode(rule.getRuleCode());
            result.setRuleName(rule.getRuleName());
            result.setSeverityLevel(rule.getSeverityLevel());
            result.setSubjectCode(targetCode);
            result.setSubjectType(subjectType);
            result.setBudgetAmount(subjectAmount);
            result.setPassed(false);
            result.setMessage(rule.getErrorMessage() + "（当前比例：" + ratio.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%）");
            results.add(result);
        } else {
            results.add(buildPassResult(rule));
        }

        return results;
    }

    /**
     * 合计校验
     */
    private List<ValidationResultVo> evaluateSumCheck(BudgetValidationRuleVo rule,
                                                       Map<String, BigDecimal> typeTotals) {
        List<ValidationResultVo> results = new ArrayList<>();
        String subjectType = rule.getSubjectType();
        BigDecimal total = typeTotals.getOrDefault(subjectType, BigDecimal.ZERO);

        boolean passed = total.compareTo(BigDecimal.ZERO) > 0;
        if (!passed) {
            ValidationResultVo result = new ValidationResultVo();
            result.setRuleCode(rule.getRuleCode());
            result.setRuleName(rule.getRuleName());
            result.setSeverityLevel(rule.getSeverityLevel());
            result.setSubjectType(subjectType);
            result.setBudgetAmount(total);
            result.setPassed(false);
            result.setMessage(rule.getErrorMessage());
            results.add(result);
        } else {
            results.add(buildPassResult(rule));
        }
        return results;
    }

    /**
     * 公式/建议校验
     */
    private List<ValidationResultVo> evaluateFormula(BudgetValidationRuleVo rule,
                                                      List<Map<String, Object>> details) {
        List<ValidationResultVo> results = new ArrayList<>();

        // 零基预算建议：始终显示为 INFO
        if ("INFO_ZERO_BASED".equals(rule.getRuleCode())) {
            results.add(buildPassResult(rule));
            return results;
        }

        // 金额取整检查
        if ("INFO_ROUND_CHECK".equals(rule.getRuleCode())) {
            List<ValidationResultVo> unrounded = new ArrayList<>();
            for (Map<String, Object> detail : details) {
                BigDecimal amount = toBigDecimal(detail.get("budgetAmount"));
                if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal remainder = amount.remainder(new BigDecimal("100"));
                    if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                        ValidationResultVo result = buildResult(rule, detail, false);
                        result.setMessage(rule.getErrorMessage() + "（当前金额：" + amount + "）");
                        unrounded.add(result);
                    }
                }
            }
            if (unrounded.isEmpty()) {
                results.add(buildPassResult(rule));
            } else {
                // 只返回前3条未取整的提示
                results.addAll(unrounded.stream().limit(3).collect(Collectors.toList()));
            }
            return results;
        }

        // 默认通过
        results.add(buildPassResult(rule));
        return results;
    }

    // ==================== 工具方法 ====================

    private ValidationResultVo buildResult(BudgetValidationRuleVo rule, Map<String, Object> detail, boolean passed) {
        ValidationResultVo result = new ValidationResultVo();
        result.setRuleCode(rule.getRuleCode());
        result.setRuleName(rule.getRuleName());
        result.setSeverityLevel(rule.getSeverityLevel());
        result.setSubjectCode((String) detail.get("subjectCode"));
        result.setSubjectName((String) detail.get("subjectName"));
        result.setSubjectType((String) detail.get("subjectType"));
        result.setBudgetAmount(toBigDecimal(detail.get("budgetAmount")));
        result.setPassed(passed);
        return result;
    }

    private ValidationResultVo buildPassResult(BudgetValidationRuleVo rule) {
        ValidationResultVo result = new ValidationResultVo();
        result.setRuleCode(rule.getRuleCode());
        result.setRuleName(rule.getRuleName());
        result.setSeverityLevel(rule.getSeverityLevel());
        result.setPassed(true);
        result.setMessage("校验通过");
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 从表达式中提取阈值，如 "<= 0.4" -> 0.4
     */
    private BigDecimal extractThresholdFromExpression(String expression) {
        if (StrUtil.isBlank(expression)) {
            return null;
        }
        // 匹配 <= 0.4 或 <= 0.15 等
        String[] parts = expression.split("<=");
        if (parts.length > 1) {
            try {
                return new BigDecimal(parts[1].trim());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
