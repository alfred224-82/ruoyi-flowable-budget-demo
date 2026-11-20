package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 老板经营驾驶舱首页 VO（T18.7，小微版 M18）
 * <p>核心经营数据 + 成本构成 + 业务待办。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class BossDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 核心经营数据 =====

    /** 今日收入 */
    private BigDecimal todayIncome;

    /** 今日支出 */
    private BigDecimal todayExpense;

    /** 今日净利 */
    private BigDecimal todayNetProfit;

    /** 本月收入 */
    private BigDecimal monthIncome;

    /** 本月支出 */
    private BigDecimal monthExpense;

    /** 本月净利 */
    private BigDecimal monthNetProfit;

    /** 本月订单数 */
    private Integer monthOrderCount;

    // ===== REQ-02 活动应收/实收拆分（M29_02，v1.3） =====

    /** 今日应收（今日开始的活动 receivable_amount 汇总，兜底=活动总价） */
    private BigDecimal todayReceivable;

    /** 今日实收（今日开始的活动 received_amount 汇总） */
    private BigDecimal todayReceived;

    /** 本月应收（本月开始的活动 receivable_amount 汇总，兜底=活动总价） */
    private BigDecimal monthReceivable;

    /** 本月实收（本月开始的活动 received_amount 汇总） */
    private BigDecimal monthReceived;

    // ===== 成本构成（按分类占比） =====

    /** 本月固定支出合计 */
    private BigDecimal monthFixedCostTotal;

    /** 按分类占比：[{category: "rent", name: "房租", amount: 5000, percent: 45.5}] */
    private List<Map<String, Object>> costComposition;

    // ===== T38.5 结算口径月利润 / 保本线（数据源：activity_settlement + monthly_operating_closing） =====

    /** 本月毛利（Σ已结算活动 profit） */
    private BigDecimal monthGrossProfit;

    /** 本月经营净利（结算口径：月毛利 − 固定成本合计 − 当月税费；区别于现金流口径的 monthNetProfit） */
    private BigDecimal monthOperatingNetProfit;

    /** 月保本毛利（= 固定成本合计） */
    private BigDecimal breakevenGrossProfit;

    /** 保本进度（% = 月毛利 ÷ 月保本毛利 × 100） */
    private BigDecimal breakevenProgress;

    // ===== 业务待办 =====

    /** 待处理预约数 */
    private Integer pendingBookingCount;

    /** 今日待服务数 */
    private Integer todayServiceCount;

    /** 合规待处理数 */
    private Integer compliancePendingCount;

    /** 固定支出异常数（本月未配置等） */
    private Integer costAnomalyCount;
}
