package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 活动结算单视图对象（M36 T36.1/T36.3）
 * <p>老板端结算单页面聚合返回：主表字段 + 收入项列表 + 成本项列表 + 活动信息冗余。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class ActivitySettlementVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long activityId;

    /** 活动信息冗余（前端展示只读区） */
    private String activityTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date activityStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date activityEndTime;
    private Integer participantCount;

    private String settlementNo;
    private String status;
    private Integer isAdjustment;
    private Long originSettlementId;

    // 收入汇总
    private BigDecimal ticketRevenue;
    private BigDecimal addonRevenue;
    private BigDecimal collectedAmount;
    private BigDecimal totalRevenue;

    // 成本汇总
    private BigDecimal employeeCost;
    private BigDecimal otherCost;
    private BigDecimal totalCost;

    // 利润
    private BigDecimal profit;
    private BigDecimal estimatedProfit;
    private BigDecimal profitDiff;

    // 结算元数据
    private Long settledBy;
    private String settledByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date settledTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockedTime;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 收入项明细列表 */
    private List<SettlementIncomeItemVo> incomeItems;

    /** 成本项明细列表 */
    private List<SettlementCostItemVo> costItems;

    /** ★T36.6 收工时追加消费提醒标志（追加消费为空时=true，前端弹提示） */
    private Boolean needAddonReminder;
}
