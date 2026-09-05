package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分账规则业务对象 sys_profit_sharing_rule（T14.3）
 * <p>platform_rate + tenant_rate = 1；服务层按 platform_rate 计算租户应得 = 订单额 ×(1-platform_rate)。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProfitSharingRuleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（编辑时必填） */
    private Long id;

    /** 平台服务费率(0~1) */
    @NotNull(message = "平台服务费率不能为空", groups = {EditGroup.class})
    @DecimalMin(value = "0.0", message = "平台服务费率不得小于0", groups = {EditGroup.class})
    @DecimalMax(value = "1.0", message = "平台服务费率不得大于1", groups = {EditGroup.class})
    private BigDecimal platformRate;

    /** 状态（0启用 1停用） */
    private String status;
}
