package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 规则灰度命中率比对结果（T7.4 / FEAT_RULE_GRAYSCALE）
 * 新规则灰度期间与上一版本规则的命中率对比
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeRuleGrayscaleCompareVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 比对的规则版本ID */
    private Long versionId;

    /** 比对的规则版本号 */
    private Integer versionNo;

    /** 参与比对的近期单据样本数 */
    private Integer sampleCount;

    /** 灰度分组内的单据数（按灰度百分比分组） */
    private Integer grayscaleSampleCount;

    /** 新版本规则命中率 */
    private BigDecimal newHitRate;

    /** 上一版本规则命中率（无上一版本为 null） */
    private BigDecimal oldHitRate;

    /** 命中率差值（新-旧） */
    private BigDecimal hitRateDiff;
}
