package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 补助标准配置对象 fee_subsidy_standard
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_subsidy_standard")
public class FeeSubsidyStandard extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 补助类型（TRAVEL/MEAL/TRANSPORT）
     */
    private String subsidyType;

    /**
     * 补助名称
     */
    private String subsidyName;

    /**
     * 单位金额（元/天）
     */
    private BigDecimal unitAmount;

    /**
     * 计量单位（DAY/TRIP/MONTH）
     */
    private String unit;

    /**
     * 归集科目（职工福利费）
     */
    private String budgetSubjectCode;

    /**
     * 最大天数限制
     */
    private Integer maxDays;

    /**
     * 行业编码
     */
    private String industryCode;

    /**
     * 状态
     */
    private String status;
}
