package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盈利分析汇总视图对象（按会计6大科目聚合）
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
public class ProfitAnalysisSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会计大类
     */
    private String accountingClass;

    /**
     * 实际金额
     */
    private BigDecimal actualAmount;

    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;

    /**
     * 差异金额（预算-实际）
     */
    private BigDecimal varianceAmount;

    /**
     * 差异率（百分比）
     */
    private BigDecimal varianceRate;
}
