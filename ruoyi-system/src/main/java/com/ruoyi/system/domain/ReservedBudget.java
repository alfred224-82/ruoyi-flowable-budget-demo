package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预留预算对象 fee_reserved_budget
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_reserved_budget")
public class ReservedBudget extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 预算期间
     */
    private String budgetPeriod;

    /**
     * 层级类型：DEPT/BRANCH/HQ
     */
    private String levelType;

    /**
     * 部门ID（HQ级别为空）
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 预留预算总额度
     */
    private BigDecimal totalAmount;

    /**
     * 已分摊金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 剩余可用额度（生成列，不映射写入）
     */
    @TableField(exist = false)
    private BigDecimal availableAmount;

    /**
     * 预警阈值百分比
     */
    private BigDecimal warningThreshold;

    /**
     * 编制人ID
     */
    private Long creatorId;

    /**
     * 状态：ACTIVE/FROZEN/EXPIRED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
