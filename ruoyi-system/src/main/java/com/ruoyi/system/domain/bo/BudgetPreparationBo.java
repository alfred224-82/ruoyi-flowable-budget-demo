package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算编制主表业务对象 budget_preparation
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BudgetPreparationBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 预算单号（唯一，格式：BG-YYYYMM-XXX）
     */
    @NotBlank(message = "预算单号不能为空", groups = { EditGroup.class })
    private String sheetNo;

    /**
     * 组织ID（关联sys_dept表的dept_id）
     */
    @NotNull(message = "组织ID不能为空", groups = { EditGroup.class })
    private Long orgId;

    /**
     * 组织名称（冗余字段，便于查询）
     */
    private String orgName;

    /**
     * 预算年度
     */
    @NotNull(message = "预算年度不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer budgetYear;

    /**
     * 预算月份（1-12）
     */
    @NotNull(message = "预算月份不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 1, message = "预算月份必须在1-12之间", groups = { AddGroup.class, EditGroup.class })
    @Max(value = 12, message = "预算月份必须在1-12之间", groups = { AddGroup.class, EditGroup.class })
    private Integer budgetMonth;

    /**
     * 预算期间（格式：YYYY-MM，自动生成）
     */
    private String budgetPeriod;

    /**
     * 状态（Draft-草稿，Pending_Review-待审核，Branch_Pending-分公司待审，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）
     */
    private String status;

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
