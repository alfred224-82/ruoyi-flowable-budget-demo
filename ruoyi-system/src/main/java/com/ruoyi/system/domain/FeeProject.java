package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目信息对象 fee_project（T5.1）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project")
public class FeeProject extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 客户名称
     */
    private String clientName;

    /**
     * 初始合同额
     */
    private BigDecimal contractAmount;

    /**
     * 当前合同额(含变更)
     */
    private BigDecimal currentContractAmount;

    /**
     * 状态(DRAFT/PENDING_APPROVAL/ACTIVE/COMPLETED/CLOSED/LOST)
     */
    private String status;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 项目经理ID
     */
    private Long projectManagerId;

    /**
     * 项目经理姓名
     */
    private String projectManagerName;

    /**
     * 项目总预算
     */
    private BigDecimal totalBudget;

    /**
     * 已用预算
     */
    private BigDecimal usedBudget;

    /**
     * 已核验收入
     */
    private BigDecimal verifiedIncome;

    /**
     * 待核验收入
     */
    private BigDecimal pendingIncome;

    /**
     * 总支出(报销+付款)
     */
    private BigDecimal totalExpense;

    /**
     * 烂账原因
     */
    private String lossReason;

    /**
     * 烂账类型
     */
    private String lossType;

    /**
     * 坏账计提金额
     */
    private BigDecimal badDebtProvision;

    /**
     * 变更次数
     */
    private Integer changeCount;

    /**
     * 部门ID(高级版双粒度预留)
     */
    private Long departmentId;

    /**
     * 组织ID(专业版多组织预留)
     */
    private Long orgId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
