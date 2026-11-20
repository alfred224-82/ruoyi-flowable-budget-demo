package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 轻量收支概览视图对象（T5.19④，基础版首页标配）
 * <p>仅本月收入/支出/结余三卡片口径，不含大类下钻/项目利润拆解/风险预警/固定成本
 * （这些属高级版老板看板 FEAT_BOSS_DASHBOARD）。
 *
 * @author ruoyi
 * @date 2026-09-06
 */
@Data
public class FeeLightOverviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计月份（yyyy-MM） */
    private String month;

    /** 本月收入：当月已核验收入确认合计 */
    private BigDecimal income;

    /** 本月支出：当月已审批报销单 + 付款单合计 */
    private BigDecimal expense;

    /** 本月结余：收入 − 支出 */
    private BigDecimal balance;
}
