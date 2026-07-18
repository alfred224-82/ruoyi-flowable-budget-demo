package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单视图对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@ExcelIgnoreUnannotated
public class ExpenseBillVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 报销单号
     */
    @ExcelProperty(value = "报销单号")
    private String billNo;

    /**
     * 部门ID
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    @ExcelProperty(value = "申请人")
    private String applicantName;

    /**
     * 责任中心编码
     */
    @ExcelProperty(value = "责任中心编码")
    private String costCenterCode;

    /**
     * 责任中心名称
     */
    @ExcelProperty(value = "责任中心名称")
    private String costCenterName;

    /**
     * 物资小类编码
     */
    @ExcelProperty(value = "物资小类编码")
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @ExcelProperty(value = "用途标签编码")
    private String purposeTag;

    /**
     * 用途标签名称
     */
    @ExcelProperty(value = "用途标签名称")
    private String purposeTagName;

    /**
     * 项目编号
     */
    @ExcelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 计算后预算科目编码
     */
    @ExcelProperty(value = "预算科目编码")
    private String calcBudgetSubject;

    /**
     * 预算科目名称
     */
    @ExcelProperty(value = "预算科目名称")
    private String calcBudgetSubjectName;

    /**
     * 计算后会计大类
     */
    @ExcelProperty(value = "会计大类")
    private String calcAccountingClass;

    /**
     * 报销金额
     */
    @ExcelProperty(value = "报销金额")
    private BigDecimal amount;

    /**
     * 费用发生日期
     */
    @ExcelProperty(value = "费用发生日期")
    private Date expenseDate;

    /**
     * 是否固定资产(0否 1是)
     */
    @ExcelProperty(value = "是否固定资产")
    private Integer isFixedAsset;

    /**
     * 支出类型(CAPITAL/OPERATING)
     */
    @ExcelProperty(value = "支出类型")
    private String expenditureType;

    /**
     * 是否超预算(0否 1是)
     */
    @ExcelProperty(value = "是否超预算")
    private Integer isOverBudget;

    /**
     * 关联超预算审批单ID
     */
    @ExcelProperty(value = "超预算审批单ID")
    private Long overBudgetApprovalId;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 分摊状态(NONE/PENDING/ALLOCATED)
     */
    @ExcelProperty(value = "分摊状态")
    private String allocationStatus;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
