package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 贷款视图对象 fee_loan（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeLoanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 贷款名称 */
    private String loanName;
    /** 贷款金额 */
    private BigDecimal amount;
    /** 年利率 */
    private BigDecimal annualRate;
    /** 还款方式(EQUAL_INSTALLMENT/INTEREST_FIRST) */
    private String method;
    /** 开始日期 */
    private Date startDate;
    /** 结束日期 */
    private Date endDate;
    /** 月利息(计算字段) */
    private BigDecimal monthlyInterest;
    /** 状态(ACTIVE/SETTLED) */
    private String status;
    /** 创建时间 */
    private Date createTime;
}
