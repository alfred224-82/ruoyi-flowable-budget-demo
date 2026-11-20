package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 周期规则对象 period_rule（T18.9，小微版 M18）
 * <p>租户级周期规则：数字 + 时间单位（如"1 个月""15 天"）。
 * <p>时间单位由 Admin 全局字典定义，本表仅存具体数值。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("period_rule")
public class PeriodRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 周期数值 */
    private BigDecimal periodValue;

    /** 时间单位：DAY/WEEK/MONTH/QUARTER/YEAR */
    private String periodUnit;

    /** 换算天数 */
    private Integer totalDays;

    /** 关联标签ID */
    private Long tagId;

    /** 状态：ENABLED/DISABLED */
    private String status;

    private String remark;
}
