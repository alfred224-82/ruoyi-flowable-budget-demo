package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 折旧记录对象 fee_asset_depreciation（T5.9）
 * 每月1号 assetDepreciationJob，含项目分摊
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_asset_depreciation")
public class FeeAssetDepreciation extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 计提月份 yyyy-MM
     */
    private String accrualMonth;

    /**
     * 月折旧额
     */
    private BigDecimal amount;

    /**
     * 分摊项目(可空=公司级)
     */
    private Long projectId;

    /**
     * 项目分摊比例
     */
    private BigDecimal shareRatio;
}
