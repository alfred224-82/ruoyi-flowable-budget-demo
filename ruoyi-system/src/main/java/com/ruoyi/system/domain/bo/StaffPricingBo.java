package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 助教价格业务对象 staff_pricing（T21.7，支持 4 种计价模式）
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
public class StaffPricingBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（编辑时必填） */
    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 助教用户ID */
    @NotNull(message = "助教不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long userId;

    /** ★基准价（元）—— 计时=时薪，套餐=基础价，计量=单价，一口价=固定价 */
    @NotNull(message = "价格不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal hourlyRate;

    /** 计价模式（duration/package/times/fixed，默认 duration） */
    private String pricingType;

    /** 扩展配置JSON（套餐/一口价的含时长、超时费等） */
    private String extraConfig;

    /** 是否启用（0停用 1启用） */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
