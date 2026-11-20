package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 周期规则业务对象（T18.9，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class PeriodRuleBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    @NotBlank(message = "规则名称不能为空", groups = {AddGroup.class})
    private String ruleName;

    @NotNull(message = "周期数值不能为空", groups = {AddGroup.class})
    private BigDecimal periodValue;

    @NotBlank(message = "时间单位不能为空", groups = {AddGroup.class})
    private String periodUnit;

    private Integer totalDays;
    private Long tagId;
    private String status;
    private String remark;
}
