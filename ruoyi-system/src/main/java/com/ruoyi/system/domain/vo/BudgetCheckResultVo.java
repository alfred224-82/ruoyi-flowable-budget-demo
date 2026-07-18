package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预算校验结果视图对象
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
public class BudgetCheckResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 是否超预算
     */
    private Boolean overBudget;

    /**
     * 预算总额
     */
    private BigDecimal totalBudget;

    /**
     * 已使用金额
     */
    private BigDecimal usedAmount;

    /**
     * 剩余预算
     */
    private BigDecimal remaining;

    /**
     * 本次申请金额
     */
    private BigDecimal thisAmount;

    /**
     * 缺口金额
     */
    private BigDecimal shortageAmount;

    /**
     * 消息提示
     */
    private String message;
}
