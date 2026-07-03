package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算编制主表对象 budget_preparationd
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_preparation")
public class BudgetPreparation extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 预算单号（唯一，格式：BG-YYYYMM-XXX）
     */
    private String sheetNo;

    /**
     * 组织ID（关联sys_dept表的dept_id）
     */
    private Long orgId;

    /**
     * 组织名称（冗余字段，便于查询）
     */
    private String orgName;

    /**
     * 预算年度
     */
    private Integer budgetYear;

    /**
     * 预算月份（1-12）
     */
    private Integer budgetMonth;

    /**
     * 预算期间（格式：YYYY-MM，自动生成）
     */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String budgetPeriod;

    /**
     * 生命周期状态（Draft-草稿，Pending_Review-待审核，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）
     */
    private String status;

    /**
     * 审批阶段（None-无，Dept-部门领导，Branch-分公司领导，HQ-总公司领导）
     */
    private String approvalStage;

    /**
     * 驳回来源（HQ-总部，Branch-分公司，None-无）
     */
    private String rejectLevel;

    /**
     * 驳回理由
     */
    private String rejectReason;

    /**
     * 截止时间
     */
    private Date deadlineTime;

    /**
     * 当前处理人（用户账号）
     */
    private String currentHandler;

    /**
     * 预算总额（自动汇总明细表）
     */
    private BigDecimal totalBudget;

    /**
     * 实际总额
     */
    private BigDecimal totalActual;

    /**
     * 差异率（百分比）
     */
    private BigDecimal varianceRate;

    /**
     * Flowable流程实例ID
     */
    private String processInstanceId;

    /**
     * 备注
     */
    private String remark;

}
