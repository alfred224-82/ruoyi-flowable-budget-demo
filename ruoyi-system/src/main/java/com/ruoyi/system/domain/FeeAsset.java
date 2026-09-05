package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资产对象 fee_asset（T5.9 / ADR-026）
 * 折旧规则财务设，系统自动算
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_asset")
public class FeeAsset extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 资产分类(ELECTRONIC/FURNITURE/VEHICLE/OFFICE)
     */
    private String category;

    /**
     * 原值
     */
    private BigDecimal originalValue;

    /**
     * 残值
     */
    private BigDecimal residualValue;

    /**
     * 使用年限(月)
     */
    private Integer lifeMonths;

    /**
     * 月折旧额
     */
    private BigDecimal monthlyDepreciation;

    /**
     * 已折旧月数
     */
    private Integer depreciatedMonths;

    /**
     * 净值
     */
    private BigDecimal netValue;

    /**
     * 归属类型(COMPANY/PROJECT)
     */
    private String attributionType;

    /**
     * 归属项目(可空=公司级)
     */
    private Long projectId;

    /**
     * 状态(ACTIVE/SCRAPPED)
     */
    private String status;
}
