package com.ruoyi.system.domain.bo;

import javax.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 活动结算单业务对象（M36 T36.3/T36.5）
 * <p>保存草稿 / 提交结算入参；收入项与成本项列表整体覆盖式更新。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class ActivitySettlementBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结算单ID（更新时非空；新建时为空） */
    private Long id;

    /** 活动ID（新建时必填） */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /** 备注 */
    private String remark;

    /** 收入项列表（整体覆盖式更新） */
    private List<SettlementIncomeItemBo> incomeItems;

    /** 其他成本项列表（EMPLOYEE 类型由后端自动生成，不接受前端传入） */
    private List<SettlementCostItemBo> otherCostItems;

    /** 预估利润（来自 M35.4 定价页快照，可选） */
    private BigDecimal estimatedProfit;

    /**
     * 收入项 BO
     */
    @Data
    public static class SettlementIncomeItemBo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
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

    /**
     * 成本项 BO（仅 OTHER 类型可手填）
     */
    @Data
    public static class SettlementCostItemBo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String itemName;
        private BigDecimal amount;
        private Integer sortOrder;
        private String remark;
    }
}
