package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 营收汇总视图对象（T15.14 报表中心：今日概览/周报/月报/利润/资金水位统一返回结构）
 * <p>由 cash_flow 按 flow_type 聚合而成（grossIncome=Σincome，totalExpense=Σexpense，netProfit=收入-支出）；
 * {@code cashBalance} 为累计结余（资金水位口径，全周期 Σincome-Σexpense），仅资金水位报表填充。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class RevenueSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计周期起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 统计周期止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 总收入 */
    private BigDecimal grossIncome;

    /** 总支出 */
    private BigDecimal totalExpense;

    /** 净利润（收入-支出） */
    private BigDecimal netProfit;

    /** 订单数 */
    private Integer orderCount;

    /** 累计资金结余（资金水位；非资金水位报表为 null） */
    private BigDecimal cashBalance;
}
