package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动明细表行视图对象（M39 T39.1，7.14）
 * <p>一行一场：以 activity_settlement 为权威数据源，冗余 promotion_activity 展示信息。
 * <p>金额口径：totalRevenue=ticket+addon（不含代收）；profit=totalRevenue-totalCost；
 * profitDiff=profit-estimatedProfit（负红/零灰/正绿着色）。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class ActivityDetailRowVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID（下钻单场结算单用） */
    private Long activityId;

    /** 结算单ID */
    private Long settlementId;

    /** 活动标题 */
    private String activityTitle;

    /** 本场员工姓名（逗号拼接，来自 selected_staff_ids / activity_staff_snapshot 解析） */
    private String staffNames;

    /** 参与人数 */
    private Integer participantCount;

    // ===== 收入 =====
    private BigDecimal ticketRevenue;
    private BigDecimal addonRevenue;
    /** 代收金额（不计入收入/利润，单列展示） */
    private BigDecimal collectedAmount;
    /** 收入合计=ticket+addon（不含代收） */
    private BigDecimal totalRevenue;

    // ===== 成本 =====
    private BigDecimal employeeCost;
    private BigDecimal otherCost;
    private BigDecimal totalCost;

    // ===== 利润 =====
    private BigDecimal profit;
    private BigDecimal estimatedProfit;
    /** 差额=profit-estimatedProfit */
    private BigDecimal profitDiff;

    /** 结算状态：DRAFT/SUBMITTED/LOCKED */
    private String settlementStatus;

    /** 是否已结算（SUBMITTED/LOCKED 且非调整单） */
    private Boolean settled;

    /** 活动结束时间（跨月归属依据） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
