package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算收入项明细 settlement_income_item（M36 T36.1）
 * <p>三类：TICKET=票面（默认行不可删）/ ADDON=追加消费 / COLLECTED=代收（不计入利润/分账基数）。
 * <p>退款以负向金额（amount&lt;0）记入，不改原金额。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settlement_income_item")
public class SettlementIncomeItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID（BaseEntity 无 tenant_id，此处显式声明便于多租户插件识别） */
    private Long tenantId;

    /** FK→activity_settlement.id */
    private Long settlementId;

    /** 收入项名称（如"成人团费"/"饮料零食"） */
    private String itemName;

    /** 类型：TICKET=票面 / ADDON=追加 / COLLECTED=代收 */
    private String itemType;

    /** 金额（退款为负值） */
    private BigDecimal amount;

    /** 数量（人数/份数） */
    private Integer quantity;

    /** 单价（可选，用于校验 amount=quantity*unit_price） */
    private BigDecimal unitPrice;

    /** 是否计入分账基数（代收=0） */
    private Integer includeInCommission;

    /** 是否计入利润（代收=0） */
    private Integer includeInProfit;

    /** 是否默认票面行（不可删） */
    private Integer isDefaultTicket;

    /** 排序 */
    private Integer sortOrder;

    /** 备注（退款原因等） */
    private String remark;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;
}
