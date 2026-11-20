package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 每月固定支出视图对象 monthly_fixed_cost（T15.37）
 * <p>boss PC 端列表 + 小程序端查看展示。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class MonthlyFixedCostVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 支出项名称 */
    private String name;

    /** 分类：rent/utility/staff_salary/staff_reward/other */
    private String category;

    /** 分类显示名 */
    private String categoryName;

    /** 金额（元） */
    private BigDecimal amount;

    /** 是否每月重复（0-否；1-是） */
    private Integer isRecurring;

    /** 关联周期规则ID */
    private Long periodRuleId;

    /** 周期规则名称（回填，非表字段） */
    private String periodRuleName;

    /** 每月分摊成本（回填，非表字段）：总额 ÷ 周期总月数；无周期规则时等于 amount */
    private BigDecimal monthlyAmount;

    /** 分摊月数（回填，非表字段）：0=不分摊（全额计入生效月）；>0=总额分摊的月数 */
    private Integer amortizeMonths;

    /** 生效月份（YYYY-MM） */
    private String effectiveMonth;

    /** 是否启用（0-停用；1-启用） */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
