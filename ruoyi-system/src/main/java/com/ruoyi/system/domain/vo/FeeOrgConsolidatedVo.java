package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 集团合并视角视图对象（T7.1 / FEAT_MULTI_ORG）
 * 各组织预算编制汇总 + 集团合计
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgConsolidatedVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 组织ID（null=未挂组织） */
    private Long orgId;

    /** 组织名称 */
    private String orgName;

    /** 预算单数量 */
    private Integer sheetCount;

    /** 预算总额合计 */
    private BigDecimal totalBudget;

    /** 实际总额合计 */
    private BigDecimal totalActual;

    /** 已批准单数量 */
    private Integer approvedCount;

    /** 最新锁定快照版本号（无快照为 null） */
    private Integer latestSnapshotVersion;

    /** 是否集团合计行 */
    private Boolean totalRow;
}
