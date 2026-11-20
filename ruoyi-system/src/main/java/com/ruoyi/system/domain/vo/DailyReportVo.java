package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 日报视图对象（M39 T39.2，7.15）
 * <p>当日汇总（场次/实收/成本/毛利/日均固定成本「均摊参考」/预估净利）
 * + 本月进度（累计毛利/月保本毛利/保本进度/已过安全线）+ 当日活动清单。
 * <p>对账铁律（T39.5）：monthAccumGrossProfit 与活动明细同月合计、月报月毛利同源。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class DailyReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期（yyyy-MM-dd） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;

    // ===== 当日汇总 =====
    /** 当日已结算场次数 */
    private Integer sessionCount;
    /** 当日实收 */
    private BigDecimal revenue;
    /** 当日变动成本 */
    private BigDecimal cost;
    /** 当日毛利 */
    private BigDecimal grossProfit;
    /** 日均固定成本参考（月固定成本合计 ÷ 当月天数，标注「均摊参考」） */
    private BigDecimal dailyFixedCostRef;
    /** 当日预估净利=当日毛利 − 日均固定成本参考 */
    private BigDecimal estNetProfit;

    // ===== 本月进度 =====
    /** 本月累计毛利（截至当日） */
    private BigDecimal monthAccumGrossProfit;
    /** 月保本毛利=固定成本合计 */
    private BigDecimal breakevenGrossProfit;
    /** 保本进度（%） */
    private BigDecimal breakevenProgress;
    /** 是否已过安全线 */
    private Boolean passedBreakeven;

    /** 当日活动清单（点击下钻单场结算单） */
    private List<ActivityDetailRowVo> sessions;
}
