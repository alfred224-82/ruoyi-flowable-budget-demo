package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 分账规则视图对象 sys_profit_sharing_rule（T14.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysProfitSharingRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 平台服务费率(0~1) */
    private BigDecimal platformRate;

    /** 租户应得率(0~1) */
    private BigDecimal tenantRate;

    /** 生效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;

    /** 状态（0启用 1停用） */
    private String status;
}
