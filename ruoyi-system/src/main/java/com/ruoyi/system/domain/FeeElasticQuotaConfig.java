package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 弹性额度配置对象 fee_elastic_quota_config（T6.1 / FEAT_ELASTIC_QUOTA）
 * 部门大类超支在弹性阈值内（如+10%）→ 预警放行，超出才拦截
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_elastic_quota_config")
public class FeeElasticQuotaConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 费用大类ID(可空=全局)
     */
    private Long categoryId;

    /**
     * 弹性阈值百分比(如+10%)
     */
    private BigDecimal thresholdPct;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
