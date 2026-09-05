package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 多账套合并报表行对象（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 * <p>集团视角：每个账套已审批通过报销单的汇总（单据数 + 金额）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class FeeAccountSetReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账套ID（NULL 归入默认账套） */
    private Long accountSetId;

    /** 账套编码 */
    private String accountSetCode;

    /** 账套名称 */
    private String accountSetName;

    /** 已审批单据数 */
    private Long billCount;

    /** 已审批金额合计 */
    private BigDecimal totalAmount;
}
