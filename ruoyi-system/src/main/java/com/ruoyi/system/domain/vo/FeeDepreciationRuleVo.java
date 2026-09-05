package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 折旧规则视图对象 fee_depreciation_rule（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeDepreciationRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 资产分类 */
    private String category;
    /** 使用年限(月) */
    private Integer lifeMonths;
    /** 残值率 */
    private BigDecimal residualRate;
    /** 是否启用 */
    private Integer isEnabled;
    /** 创建时间 */
    private Date createTime;
}
