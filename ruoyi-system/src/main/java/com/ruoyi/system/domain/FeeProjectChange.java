package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同变更对象 fee_project_change（T5.5 / ADR-022）
 * 增量叠加不回溯，分步生效，30/60/90 逾期升级
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project_change")
public class FeeProjectChange extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 变更类型(CONTRACT_INCREASE/BUDGET_INCREASE/COMPREHENSIVE)
     */
    private String changeType;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 原合同额
     */
    private BigDecimal oldContractAmount;

    /**
     * 新合同额
     */
    private BigDecimal newContractAmount;

    /**
     * 合同额变更量
     */
    private BigDecimal contractDelta;

    /**
     * 按大类预算变更 JSON {"差旅招待":2000}
     */
    private String budgetDeltas;

    /**
     * 变更状态(PENDING/APPROVED/REJECTED/CONFIRMED/OVERDUE)
     */
    private String changeStatus;

    /**
     * 依据附件
     */
    private String basisUrls;

    /**
     * 发起人
     */
    private Long createdBy;

    /**
     * 审批人(老板)
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private Date approvedAt;

    /**
     * 收款确认时间
     */
    private Date confirmedAt;
}
