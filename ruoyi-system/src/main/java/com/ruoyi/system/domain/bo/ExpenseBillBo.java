package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单业务对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseBillBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 物资小类编码
     */
    @NotBlank(message = "物资小类不能为空", groups = {AddGroup.class})
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @NotBlank(message = "用途标签不能为空", groups = {AddGroup.class})
    private String purposeTag;

    /**
     * 项目编号（用途=项目交付时必填）
     */
    private String projectCode;

    /**
     * 报销金额
     */
    @NotNull(message = "报销金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "报销金额必须大于0", groups = {AddGroup.class})
    private BigDecimal amount;

    /**
     * 费用发生日期
     */
    @NotNull(message = "费用发生日期不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expenseDate;

    /**
     * 报销年月（格式：YYYY-MM，对应预算编制期间）
     */
    @NotBlank(message = "报销年月不能为空", groups = {AddGroup.class})
    private String expensePeriod;

    /**
     * 使用年限(年)，仅固定资产时填写
     */
    private Integer usefulLife;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
