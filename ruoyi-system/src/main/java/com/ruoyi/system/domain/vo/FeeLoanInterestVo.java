package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 利息计提记录视图对象 fee_loan_interest（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeLoanInterestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 贷款ID */
    private Long loanId;
    /** 贷款名称(关联展示) */
    private String loanName;
    /** 计提月份 yyyy-MM */
    private String accrualMonth;
    /** 月利息金额 */
    private BigDecimal amount;
    /** 创建时间 */
    private Date createTime;
}
