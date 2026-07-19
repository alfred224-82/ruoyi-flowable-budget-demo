package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 超预算审批对象 fee_over_budget_approval
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_over_budget_approval")
public class OverBudgetApproval extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联报销单ID
     */
    private Long billId;

    /**
     * 关联报销单号
     */
    private String billNo;

    /**
     * 超预算的预算科目编码
     */
    private String budgetSubject;

    /**
     * 预算科目名称
     */
    private String budgetSubjectName;

    /**
     * 该科目原预算金额
     */
    private BigDecimal originalBudget;

    /**
     * 该科目已使用金额
     */
    private BigDecimal usedAmount;

    /**
     * 本次申请金额
     */
    private BigDecimal requestAmount;

    /**
     * 缺口金额
     */
    private BigDecimal shortageAmount;

    /**
     * 审批动作(APPROVE/REJECT/ADJUST)
     */
    private String approvalAction;

    /**
     * 调出科目编码
     */
    private String adjustFromSubject;

    /**
     * 调出科目名称
     */
    private String adjustFromSubjectName;

    /**
     * 调剂金额
     */
    private BigDecimal adjustAmount;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批意见
     */
    private String approvalOpinion;

    /**
     * 审批时间
     */
    private Date approvalTime;

    /**
     * 状态(PENDING/APPROVED/REJECTED)
     */
    private String status;
}
