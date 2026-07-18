package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 超预算审批业务对象 fee_over_budget_approval
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OverBudgetApprovalBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 审批状态（PENDING/PROCESSED，列表查询用）
     */
    private String status;

    /**
     * 审批动作(APPROVE/REJECT/ADJUST)
     */
    @NotBlank(message = "审批动作不能为空")
    private String action;

    /**
     * 调出科目（ADJUST时必填）
     */
    private String adjustFromSubject;

    /**
     * 调剂金额（ADJUST时必填）
     */
    private BigDecimal adjustAmount;

    /**
     * 审批意见（必填）
     */
    @NotBlank(message = "审批意见不能为空")
    private String opinion;
}
