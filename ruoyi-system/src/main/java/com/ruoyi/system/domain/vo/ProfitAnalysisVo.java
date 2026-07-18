package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盈利分析明细视图对象
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
public class ProfitAnalysisVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 责任中心编码
     */
    private String costCenterCode;

    /**
     * 责任中心名称
     */
    private String costCenterName;

    /**
     * 预算科目编码
     */
    private String budgetSubjectCode;

    /**
     * 预算科目名称
     */
    private String budgetSubjectName;

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
