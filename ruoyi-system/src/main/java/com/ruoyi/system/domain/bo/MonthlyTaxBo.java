package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 月度税费保存业务对象（M38 T38.4）
 * <p>taxMode=MANUAL 时用 taxAmount（手填）；taxMode=RATE 时用 taxRate 按税前利润估算。
 * <p>月结锁定后禁止修改（Service 层校验 status）。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class MonthlyTaxBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    @NotBlank(message = "月份不能为空")
    private String month;

    /** 税费金额（元，MANUAL 手填） */
    private BigDecimal taxAmount;

    /** 税率（%，RATE 估算用） */
    private BigDecimal taxRate;

    /** 计税方式：MANUAL=手填 / RATE=按率估算 */
    private String taxMode;

    /** 备注 */
    private String remark;
}
