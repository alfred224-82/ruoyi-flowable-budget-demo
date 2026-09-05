package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 贷款对象 fee_loan（T5.9 / ADR-027）
 * 财务设参数，老板只读
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_loan")
public class FeeLoan extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 贷款名称
     */
    private String loanName;

    /**
     * 贷款金额
     */
    private BigDecimal amount;

    /**
     * 年利率
     */
    private BigDecimal annualRate;

    /**
     * 还款方式(EQUAL_INSTALLMENT/INTEREST_FIRST)
     */
    private String method;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 状态(ACTIVE/SETTLED)
     */
    private String status;
}
