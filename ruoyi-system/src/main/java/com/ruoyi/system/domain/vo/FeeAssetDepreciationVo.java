package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 折旧记录视图对象 fee_asset_depreciation（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeAssetDepreciationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 资产ID */
    private Long assetId;
    /** 资产名称(关联展示) */
    private String assetName;
    /** 计提月份 yyyy-MM */
    private String accrualMonth;
    /** 月折旧额 */
    private BigDecimal amount;
    /** 分摊项目 */
    private Long projectId;
    /** 项目分摊比例 */
    private BigDecimal shareRatio;
    /** 创建时间 */
    private Date createTime;
}
