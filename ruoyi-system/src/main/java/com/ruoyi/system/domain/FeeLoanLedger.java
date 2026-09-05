package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 借款台账对象 fee_loan_ledger
 * 设计文档 Phase 7 Task 7.1
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_loan_ledger")
public class FeeLoanLedger extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 借款编号 */
    private String loanCode;

    /** 借款类型（SHORT_TERM/LONG_TERM） */
    private String loanType;

    /** 本金 */
    private BigDecimal principal;

    /** 年化利率 */
    private BigDecimal annualRate;

    /** 月利息（principal×annual_rate÷12） */
    private BigDecimal monthlyInterest;

    /** 借款起始日 */
    private Date startDate;

    /** 借款到期日 */
    private Date endDate;

    /** 计提周期（MONTHLY/QUARTERLY） */
    private String interestCycle;

    /** 归属部门 */
    private Long deptId;

    /** 关联科目（默认利息支出） */
    private String budgetSubjectCode;

    /** 累计已计提利息 */
    private BigDecimal accumulatedInterest;

    /** 录入来源（AI/MANUAL） */
    private String source;

    /** 状态（ACTIVE/SETTLED/OVERDUE） */
    private String status;
}
