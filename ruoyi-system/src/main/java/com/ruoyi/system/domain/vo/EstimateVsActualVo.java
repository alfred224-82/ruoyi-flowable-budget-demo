package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预估 vs 实际报表行视图对象（M35 T35.7）
 * <p>报表字段：活动 / 预计利润 / 实际利润 / 差额 / 原因。
 * <p>数据来源：预计取自 T35.4 定价页试算（活动创建时快照存于 remark 或独立字段），
 * 实际取自 M36 活动结算单（activity_settlement.profit）。
 * <p>★M36 未交付前 actualProfit 返回 null，差额留空。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@ExcelIgnoreUnannotated
public class EstimateVsActualVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动标题 */
    @ExcelProperty(value = "活动")
    private String activityTitle;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @ExcelProperty(value = "开始时间")
    private Date startTime;

    /** 活动状态 */
    @ExcelProperty(value = "状态")
    private String status;

    /** 预计人数 */
    @ExcelProperty(value = "预计人数")
    private Integer expectedHeadcount;

    /** 实际人数（已确认报名） */
    @ExcelProperty(value = "实际人数")
    private Integer actualHeadcount;

    /** 单人售价 */
    @ExcelProperty(value = "单人售价")
    private BigDecimal unitPrice;

    /** 预计收入 */
    @ExcelProperty(value = "预计收入")
    private BigDecimal expectedRevenue;

    /** 员工成本（快照合计） */
    @ExcelProperty(value = "员工成本")
    private BigDecimal employeeCost;

    /** 其他成本 */
    @ExcelProperty(value = "其他成本")
    private BigDecimal otherCost;

    /** 预计利润（= 预计收入 - 员工成本 - 其他成本） */
    @ExcelProperty(value = "预计利润")
    private BigDecimal estimatedProfit;

    /** 实际利润（取自 M36 活动结算单，未结算为 null） */
    @ExcelProperty(value = "实际利润")
    private BigDecimal actualProfit;

    /** 差额（= 实际利润 - 预计利润；任一为 null 时返回 null） */
    @ExcelProperty(value = "差额")
    private BigDecimal diff;

    /** 差额原因（人工填写，M36 结算单备注） */
    @ExcelProperty(value = "原因")
    private String diffReason;

    /** 结算状态（DRAFT/SUBMITTED/LOCKED/UNSETTLED） */
    @ExcelProperty(value = "结算状态")
    private String settlementStatus;
}
