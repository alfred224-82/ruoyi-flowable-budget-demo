package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 每月固定支出对象 monthly_fixed_cost（T15.37，小微版 pro 档 boss PC 端管理）
 * <p>boss 管理每月固定支出项（房租/水电/工资等），首页盈亏三指标中「支出」= 现金流流水支出 + 本月固定支出合计。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；含 del_flag 逻辑删除。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("monthly_fixed_cost")
public class MonthlyFixedCost extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 支出项名称（如：房租、水电费、员工工资）
     */
    private String name;

    /**
     * 分类：rent(房租)/utility(水电)/staff_salary(员工工资)/staff_reward(员工提成)/other(其他)
     */
    private String category;

    /**
     * 金额（元）
     */
    private BigDecimal amount;

    /**
     * 是否每月重复（0-否，仅当月；1-是，每月自动计入）
     */
    private Integer isRecurring;

    /**
     * 关联周期规则ID（period_rule.id，可选）
     */
    private Long periodRuleId;

    /**
     * 生效月份（格式：YYYY-MM，如 2026-09）
     */
    private String effectiveMonth;

    /**
     * 是否启用（0-停用；1-启用）
     */
    private Integer enabled;

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
