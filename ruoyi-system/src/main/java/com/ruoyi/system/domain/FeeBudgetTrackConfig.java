package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 三轨预算管控配置对象 fee_budget_track_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_budget_track_config")
public class FeeBudgetTrackConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 预算科目编码
     */
    private String budgetSubjectCode;

    /**
     * 轨道类型（EXPENSE/CAPITAL/CASHFLOW）
     */
    private String trackType;

    /**
     * 管控模式（SOFT软预警/HARD硬阻断）
     */
    private String controlMode;

    /**
     * 预警阈值百分比
     */
    private BigDecimal warningThreshold;

    /**
     * 行业编码
     */
    private String industryCode;

    /**
     * 状态
     */
    private String status;

    private Date createTime;
    private Date updateTime;
}
