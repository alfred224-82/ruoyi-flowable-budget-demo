package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 规则版本发布业务对象（T7.4 / FEAT_RULE_GRAYSCALE）
 * 新规则默认 10% 灰度，比对命中率后再全量
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeRulePublishBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 灰度百分比（默认10%，100=直接全量） */
    @NotNull(message = "灰度百分比不能为空")
    @DecimalMin(value = "1", message = "灰度百分比不能低于1%")
    @DecimalMax(value = "100", message = "灰度百分比不能超过100%")
    private BigDecimal grayscalePct;
}
