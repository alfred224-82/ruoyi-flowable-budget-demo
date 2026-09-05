package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单Excel导入模型
 *
 * @author ruoyi
 * @date 2026-07-25
 */
@Data
@NoArgsConstructor
public class ExpenseBillImportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物资小类编码
     */
    @ExcelProperty(value = "物资小类编码")
    @NotBlank(message = "物资小类编码不能为空")
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @ExcelProperty(value = "用途标签编码")
    @NotBlank(message = "用途标签编码不能为空")
    private String purposeTag;

    /**
     * 项目编号
     */
    @ExcelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 报销金额（人民币）
     */
    @ExcelProperty(value = "报销金额")
    @NotNull(message = "报销金额不能为空")
    @DecimalMin(value = "0.01", message = "报销金额必须大于0")
    private BigDecimal amount;

    /**
     * 币种编码（CNY/USD/EUR/GBP/JPY），默认CNY
     */
    @ExcelProperty(value = "币种")
    private String currencyCode;

    /**
     * 原币金额（外币时填写）
     */
    @ExcelProperty(value = "原币金额")
    private BigDecimal originalAmount;

    /**
     * 出差天数（用于补助核算）
     */
    @ExcelProperty(value = "出差天数")
    private Integer travelDays;

    /**
     * 费用发生日期
     */
    @ExcelProperty(value = "费用发生日期")
    @NotNull(message = "费用发生日期不能为空")
    private Date expenseDate;

    /**
     * 报销年月（格式：YYYY-MM）
     */
    @ExcelProperty(value = "报销年月")
    @NotBlank(message = "报销年月不能为空")
    private String expensePeriod;

    /**
     * 使用年限(年)，仅固定资产时填写
     */
    @ExcelProperty(value = "使用年限(年)")
    private Integer usefulLife;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
