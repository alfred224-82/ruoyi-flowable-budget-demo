package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算调整申请对象 fee_budget_adjustment_app
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_budget_adjustment_app")
public class BudgetAdjustmentApp extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 申请单号（BA-YYYYMMDD-XXX）
     */
    private String appNo;

    /**
     * 关联报销单ID
     */
    private Long billId;

    /**
     * 关联报销单号
     */
    private String billNo;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请人部门ID
     */
    private Long deptId;

    /**
     * 申请人部门名称
     */
    private String deptName;

    /**
     * 物资小类编码
     */
    private String itemCategory;

    /**
     * 用途标签编码
     */
    private String purposeTag;

    /**
     * 目标预算科目编码
     */
    private String targetSubject;

    /**
     * 目标预算科目名称
     */
    private String targetSubjectName;

    /**
     * 申请时科目可用余额
     */
    private BigDecimal currentBalance;

    /**
     * 申请调整金额
     */
    private BigDecimal requestAmount;

    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;

    /**
     * 调整原因说明
     */
    private String reason;

    /**
     * 当前审批层级：1部门/2分公司/3总公司
     */
    private Integer currentLevel;

    /**
     * 部门预留预算ID
     */
    private Long deptReservedId;

    /**
     * 分公司预留预算ID
     */
    private Long branchReservedId;

    /**
     * 总公司预留预算ID
     */
    private Long hqReservedId;

    /**
     * 实际分摊金额
     */
    private BigDecimal allocateAmount;

    /**
     * 实际分摊来源的预留预算ID
     */
    private Long allocateFromReservedId;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 状态：PENDING/DEPT_APPROVING/BRANCH_APPROVING/HQ_APPROVING/APPROVED/REJECTED
     */
    private String status;

    /**
     * 完成时间
     */
    private Date completeTime;
}
