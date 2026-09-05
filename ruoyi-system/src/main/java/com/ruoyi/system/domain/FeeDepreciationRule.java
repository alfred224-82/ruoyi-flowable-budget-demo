package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 折旧规则对象 fee_depreciation_rule（T5.9）
 * 财务设置：资产分类/年限/残值率
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_depreciation_rule")
public class FeeDepreciationRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 资产分类 ELECTRONIC/FURNITURE/VEHICLE/OFFICE
     */
    private String category;

    /**
     * 使用年限(月)
     */
    private Integer lifeMonths;

    /**
     * 残值率
     */
    private BigDecimal residualRate;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
