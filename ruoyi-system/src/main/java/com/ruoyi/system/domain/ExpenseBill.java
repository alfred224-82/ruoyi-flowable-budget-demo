package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_expense_bill")
public class ExpenseBill extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 报销单号
     */
    private String billNo;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 责任中心编码
     */
    private String costCenterCode;

    /**
     * 物资小类编码
     */
    private String itemCategory;

    /**
     * 用途标签编码
     */
    private String purposeTag;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 计算后预算科目编码
     */
    private String calcBudgetSubject;

    /**
     * 计算后会计大类
     */
    private String calcAccountingClass;

    /**
     * 报销金额
     */
    private BigDecimal amount;

    /**
     * 费用发生日期
     */
    private Date expenseDate;

    /**
     * 是否固定资产(0否 1是)
     */
    private Integer isFixedAsset;

    /**
     * 支出类型(CAPITAL/OPERATING)
     */
    private String expenditureType;

    /**
     * 是否超预算(0否 1是)
     */
    private Integer isOverBudget;

    /**
     * 关联超预算审批单ID
     */
    private Long overBudgetApprovalId;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    private String status;

    /**
     * 分摊状态(NONE/PENDING/ALLOCATED)
     */
    private String allocationStatus;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 驳回时间
     */
    private Date rejectTime;

    /**
     * 备注
     */
    private String remark;
}
