package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 费用分摊记录对象 fee_cost_allocation
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_cost_allocation")
public class CostAllocation extends TenantEntity {

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
     * 分摊批次(YYYY-MM)
     */
    private String allocationBatch;

    /**
     * 目标部门ID
     */
    private Long targetDeptId;

    /**
     * 分摊金额
     */
    private BigDecimal allocAmount;

    /**
     * 分摊比例
     */
    private BigDecimal allocRatio;

    /**
     * 分摊规则ID
     */
    private Long allocRuleId;
}
