package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 资产视图对象 fee_asset（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeAssetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 资产名称 */
    private String assetName;
    /** 资产分类(ELECTRONIC/FURNITURE/VEHICLE/OFFICE) */
    private String category;
    /** 原值 */
    private BigDecimal originalValue;
    /** 残值 */
    private BigDecimal residualValue;
    /** 使用年限(月) */
    private Integer lifeMonths;
    /** 月折旧额 */
    private BigDecimal monthlyDepreciation;
    /** 已折旧月数 */
    private Integer depreciatedMonths;
    /** 净值 */
    private BigDecimal netValue;
    /** 归属类型(COMPANY/PROJECT) */
    private String attributionType;
    /** 归属项目 */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 状态(ACTIVE/SCRAPPED) */
    private String status;
    /** 创建时间 */
    private Date createTime;
}
