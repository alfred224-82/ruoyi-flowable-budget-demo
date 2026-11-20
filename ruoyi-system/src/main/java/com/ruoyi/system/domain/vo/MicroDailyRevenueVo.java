package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 每日营收快照视图对象 micro_daily_revenue（T15.5/T15.14）
 * <p>报表趋势（周报/月报/资金水位）按日展示的数据行。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroDailyRevenueVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 营收日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date revenueDate;

    /** 当日总收入 */
    private BigDecimal grossIncome;

    /** 当日总支出 */
    private BigDecimal totalExpense;

    /** 当日净利润 */
    private BigDecimal netProfit;

    /** 当日订单数 */
    private Integer orderCount;

    /** 快照生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;
}
