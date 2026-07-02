package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算科目规则校验视图对象
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetValidationRuleVo {

    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "规则编码")
    private String ruleCode;

    @ExcelProperty(value = "规则名称")
    private String ruleName;

    @ExcelProperty(value = "规则类型")
    private String ruleType;

    @ExcelProperty(value = "严重级别")
    private String severityLevel;

    @ExcelProperty(value = "适用科目类型")
    private String subjectType;

    @ExcelProperty(value = "适用科目编码")
    private String subjectCode;

    @ExcelProperty(value = "阈值")
    private BigDecimal thresholdValue;

    @ExcelProperty(value = "阈值2")
    private BigDecimal thresholdValue2;

    @ExcelProperty(value = "规则表达式")
    private String ruleExpression;

    @ExcelProperty(value = "错误提示")
    private String errorMessage;

    @ExcelProperty(value = "是否启用")
    private Integer isActive;

    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    @ExcelProperty(value = "规则说明")
    private String description;

    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
