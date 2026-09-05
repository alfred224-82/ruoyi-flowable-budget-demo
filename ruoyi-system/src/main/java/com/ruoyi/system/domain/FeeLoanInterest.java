package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 利息计提记录对象 fee_loan_interest（T5.9）
 * 每月1号 loanInterestJob
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_loan_interest")
public class FeeLoanInterest extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 贷款ID
     */
    private Long loanId;

    /**
     * 计提月份 yyyy-MM
     */
    private String accrualMonth;

    /**
     * 月利息金额
     */
    private BigDecimal amount;
}
