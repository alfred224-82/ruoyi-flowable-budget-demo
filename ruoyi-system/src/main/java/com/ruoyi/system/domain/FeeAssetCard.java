package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 资产卡片对象 fee_asset_card
 * 设计文档 Phase 6 Task 6.1
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_asset_card")
public class FeeAssetCard extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 资产编号 */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 资产类型（FIXED/INTANGIBLE） */
    private String assetType;

    /** 归属部门ID */
    private Long deptId;

    /** 关联预算科目 */
    private String budgetSubjectCode;

    /** 原值 */
    private BigDecimal originalValue;

    /** 残值率(%) */
    private BigDecimal residualRate;

    /** 折旧/摊销月数 */
    private Integer usefulLifeMonths;

    /** 启用日期（次月起计提） */
    private Date startDate;

    /** 月折旧/摊销额 */
    private BigDecimal monthlyAmount;

    /** 累计已计提金额 */
    private BigDecimal accumulatedAmount;

    /** 来源报销单ID */
    private Long sourceBillId;

    /** 行业编码 */
    private String industryCode;

    /** 状态（ACTIVE/DISPOSED/FULLY_DEPRECIATED） */
    private String status;
}
