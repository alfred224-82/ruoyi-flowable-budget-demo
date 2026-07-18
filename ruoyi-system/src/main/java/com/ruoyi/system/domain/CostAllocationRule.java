package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 费用分摊规则对象 fee_cost_allocation_rule
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_cost_allocation_rule")
public class CostAllocationRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 分摊方法(HEAD_COUNT/WORK_HOUR/RATIO)
     */
    private String allocationMethod;

    /**
     * 目标部门ID
     */
    private Long targetDeptId;

    /**
     * 目标责任中心编码
     */
    private String targetCostCenter;

    /**
     * 权重
     */
    private BigDecimal weight;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;
}
