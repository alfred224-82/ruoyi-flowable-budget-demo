package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算调整申请视图对象 fee_budget_adjustment_app
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetAdjustmentAppVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @ExcelProperty("申请单号")
    private String appNo;

    private Long billId;

    @ExcelProperty("关联报销单号")
    private String billNo;

    private Long applicantId;

    @ExcelProperty("申请人")
    private String applicantName;

    private Long deptId;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("物资小类")
    private String itemCategory;

    @ExcelProperty("用途标签")
    private String purposeTag;

    @ExcelProperty("目标科目编码")
    private String targetSubject;

    @ExcelProperty("目标科目名称")
    private String targetSubjectName;

    private BigDecimal currentBalance;

    @ExcelProperty("申请调整金额")
    private BigDecimal requestAmount;

    @ExcelProperty("预算金额")
    private BigDecimal budgetAmount;

    private String reason;

    private Integer currentLevel;

    private BigDecimal allocateAmount;

    private Long allocateFromReservedId;

    private String rejectReason;

    @ExcelProperty("状态")
    private String status;

    private Date completeTime;

    private Date createTime;
}
