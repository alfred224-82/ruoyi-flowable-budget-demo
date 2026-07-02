package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算科目规则校验对象 budget_validation_rule
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_validation_rule")
public class BudgetValidationRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 规则编码（唯一） */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型（REQUIRED/MIN_AMOUNT/MAX_AMOUNT/RATIO_LIMIT/SUM_CHECK/FORMULA） */
    private String ruleType;

    /** 严重级别（ERROR-错误/红色阻断, WARNING-警告/黄色, INFO-建议/蓝色） */
    private String severityLevel;

    /** 适用科目类型（NULL表示全部类型） */
    private String subjectType;

    /** 适用科目编码（NULL表示该类型全部） */
    private String subjectCode;

    /** 阈值 */
    private BigDecimal thresholdValue;

    /** 阈值2（用于范围校验） */
    private BigDecimal thresholdValue2;

    /** 规则表达式/说明 */
    private String ruleExpression;

    /** 校验不通过时的提示信息 */
    private String errorMessage;

    /** 是否启用（0-禁用，1-启用） */
    private Integer isActive;

    /** 排序号 */
    private Integer sortOrder;

    /** 规则说明 */
    private String description;
}
