package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 打卡触发规则配置对象 fee_clock_trigger_rule（T6.2 / FEAT_TIME_CLOCK）
 * tenant_id=0 为内置默认模板（差旅招待/≥500/历史异常≥2），租户级可覆盖
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_clock_trigger_rule")
public class FeeClockTriggerRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 规则编码(TRAVEL_CATEGORY/AMOUNT_THRESHOLD/HISTORY_ABNORMAL)
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 阈值(金额阈值/历史异常次数)
     */
    private BigDecimal threshold;

    /**
     * 大类范围(逗号分隔大类编码)
     */
    private String categoryScope;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
