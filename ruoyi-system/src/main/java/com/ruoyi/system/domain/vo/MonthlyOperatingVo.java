package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 月度经营报表视图对象（M38 T38.3，7.13；三层利润模型 §20）
 * <p>月毛利=Σ已结算活动 profit；月净利=月毛利−固定成本合计−人力成本−税费；
 * 月保本毛利=固定成本合计；月保本场次=ceil(固定成本合计 ÷ 平均单场毛利)。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class MonthlyOperatingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    private String month;

    // ===== 月经营汇总 =====
    /** 月实收=Σ(ticket_revenue+addon_revenue)，不含代收 */
    private BigDecimal monthRevenue;
    /** 月变动成本=Σtotal_cost */
    private BigDecimal monthCost;
    /** 月毛利=Σ已结算活动 profit */
    private BigDecimal monthGrossProfit;
    /** 已结算场次数 */
    private Integer activityCount;
    /** 平均单场毛利=月毛利 ÷ 已结算场次 */
    private BigDecimal avgSessionProfit;
    /** 未结算场次数（DRAFT，提醒） */
    private Integer unsettledCount;

    // ===== 固定成本构成（含人力成本） =====
    /** 成本构成：[{category,name,amount,percent}] */
    private List<Map<String, Object>> fixedCostComposition;
    /** 固定成本合计（分摊后 + 人力成本） */
    private BigDecimal fixedCostTotal;

    // ===== 税费与净利 =====
    /** 税前利润=月毛利 − 固定成本合计 */
    private BigDecimal pretaxProfit;
    /** 税费金额 */
    private BigDecimal taxAmount;
    /** 税率（%） */
    private BigDecimal taxRate;
    /** 计税方式：MANUAL/RATE */
    private String taxMode;
    /** 月净利润=税前利润 − 税费 */
    private BigDecimal netProfit;

    // ===== 保本线 =====
    /** 月保本毛利=固定成本合计 */
    private BigDecimal breakevenGrossProfit;
    /** 月保本场次=ceil(固定成本合计 ÷ 平均单场毛利) */
    private Integer breakevenSessions;
    /** 保本进度（%=月毛利 ÷ 月保本毛利 × 100，封顶展示由前端处理） */
    private BigDecimal breakevenProgress;
    /** 是否已过保本线 */
    private Boolean passedBreakeven;

    // ===== 对比上月 =====
    private BigDecimal prevMonthGrossProfit;
    private BigDecimal prevNetProfit;

    /** 月结状态：OPEN/LOCKED */
    private String closingStatus;
}
