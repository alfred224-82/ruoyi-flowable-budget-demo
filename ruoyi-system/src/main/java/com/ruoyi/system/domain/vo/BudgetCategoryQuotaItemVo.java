package com.ruoyi.system.domain.vo;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 老板大类预算额度行视图对象（T5.14 ①，粗粒度）
 * 一行 = 一个费用大类的预算额度
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class BudgetCategoryQuotaItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大类编码（关联 fee_expense_category.category_code）
     */
    @NotBlank(message = "大类编码不能为空")
    private String categoryCode;

    /**
     * 大类名称（冗余展示，保存时以主数据为准）
     */
    private String categoryName;

    /**
     * 大类预算额度
     */
    @NotNull(message = "大类预算额度不能为空")
    @DecimalMin(value = "0", message = "大类预算额度不能为负数")
    private BigDecimal amount;
}
