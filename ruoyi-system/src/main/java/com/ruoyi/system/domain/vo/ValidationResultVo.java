package com.ruoyi.system.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 规则校验结果视图对象
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
public class ValidationResultVo {

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 严重级别（ERROR-错误/红色阻断, WARNING-警告/黄色, INFO-建议/蓝色）
     */
    private String severityLevel;

    /**
     * 科目编码（NULL表示全局规则）
     */
    private String subjectCode;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 科目类型
     */
    private String subjectType;

    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;

    /**
     * 校验结果信息
     */
    private String message;

    /**
     * 是否通过校验
     */
    private Boolean passed;
}
