package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算收入项视图对象（M36 T36.1）
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class SettlementIncomeItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long settlementId;
    private String itemName;
    /** TICKET/ADDON/COLLECTED */
    private String itemType;
    private BigDecimal amount;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer includeInCommission;
    private Integer includeInProfit;
    private Integer isDefaultTicket;
    private Integer sortOrder;
    private String remark;
}
