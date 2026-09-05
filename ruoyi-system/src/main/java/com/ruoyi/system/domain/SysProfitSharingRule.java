package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 分账规则对象 sys_profit_sharing_rule（T14.3 平台服务费 + 租户应得）
 * <p>每租户唯一（uk_tenant）；platform_rate + tenant_rate = 1；未配置时默认平台 0、租户全额。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_profit_sharing_rule")
public class SysProfitSharingRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 平台服务费率(0~1,如 0.0060=0.6%) */
    private BigDecimal platformRate;

    /** 租户应得率(0~1；platform_rate+tenant_rate=1) */
    private BigDecimal tenantRate;

    /** 生效时间 */
    private Date effectiveTime;

    /** 状态（0启用 1停用） */
    private String status;
}
