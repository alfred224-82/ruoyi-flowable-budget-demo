package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 活动定价试算视图对象（M35 T35.4 定价页自动算账）
 * <p>老板 PC 活动创建/编辑页实时计算：
 * <pre>
 *   预计收入 = 预计人数 × 单人售价
 *   员工成本 = 已选员工规则快照求和
 *   预估利润 = 预计收入 - 员工成本 - 其他成本
 *   保本人数 = ceil(固定成本 ÷ 单位边际贡献)
 *            = ceil((员工成本 + 其他成本) ÷ (单人售价 - 单人变动员工成本))
 * </pre>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class ActivityPricingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单人售价 */
    private BigDecimal unitPrice;

    /** 预计人数 */
    private Integer expectedHeadcount;

    /** 预计收入（= unitPrice × expectedHeadcount） */
    private BigDecimal expectedRevenue;

    /** 员工成本合计（已选员工按当前规则试算之和） */
    private BigDecimal employeeCost;

    /** 其他成本（场地/物料等） */
    private BigDecimal otherCost;

    /** 成本合计（= employeeCost + otherCost） */
    private BigDecimal totalCost;

    /** 预估利润（= expectedRevenue - totalCost） */
    private BigDecimal estimatedProfit;

    /** 单位边际贡献（= unitPrice - 单人变动员工成本；无变动员工成本时 = unitPrice） */
    private BigDecimal unitContribution;

    /** 保本人数（= ceil(totalCost ÷ unitContribution)，unitContribution<=0 时返回 null 表示无法保本） */
    private Integer breakevenHeadcount;

    /** 已选员工试算明细（每人一行） */
    private List<StaffCostItem> staffCostItems;

    /** 脚注口径说明 */
    private String footnote;

    /**
     * 单员工成本试算明细
     */
    @Data
    public static class StaffCostItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 员工用户ID */
        private Long staffId;

        /** 员工姓名 */
        private String staffName;

        /** 分成类型 */
        private String commissionType;

        /** 试算金额（FIXED_AMOUNT=fixedAmount；PERCENTAGE=expectedRevenue×rate/100） */
        private BigDecimal amount;

        /** 规则描述（如「固定 200 元/场」「按 10% 提成」） */
        private String ruleDesc;
    }
}
