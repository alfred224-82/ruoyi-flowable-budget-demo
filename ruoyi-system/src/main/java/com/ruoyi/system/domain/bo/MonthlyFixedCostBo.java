package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 每月固定支出业务对象 monthly_fixed_cost（T15.37）
 * <p>boss PC 端新增/编辑固定支出项；查询过滤：name/category/effectiveMonth/enabled。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class MonthlyFixedCostBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "支出项ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 支出项名称 */
    @NotBlank(message = "支出项名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 分类：rent/utility/staff_salary/staff_reward/other */
    @NotBlank(message = "分类不能为空", groups = {AddGroup.class, EditGroup.class})
    private String category;

    /** 金额（元） */
    @NotNull(message = "金额不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal amount;

    /** 是否每月重复（0-否；1-是） */
    private Integer isRecurring;

    /** 关联周期规则ID（period_rule.id，可选） */
    private Long periodRuleId;

    /** 生效月份（YYYY-MM） */
    @NotBlank(message = "生效月份不能为空", groups = {AddGroup.class, EditGroup.class})
    private String effectiveMonth;

    /** 是否启用（0-停用；1-启用） */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
