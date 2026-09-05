package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信用分规则配置对象 fee_credit_rule（T6.3 / FEAT_CREDIT_SCORE）
 * 内置默认（tenant_id=0）：按时+2 / 逾期-5 / 连续3次按时+10；财务可维护租户级规则
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_credit_rule")
public class FeeCreditRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 事件类型(ON_TIME补票按时/OVERDUE补票逾期/CONSECUTIVE_BONUS连续按时奖励)
     */
    private String eventType;

    /**
     * 加减分值(+2/-5/+10)
     */
    private Integer scoreDelta;

    /**
     * 触发阈值(连续次数等)
     */
    private Integer threshold;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
