package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 大类默认拆分比例视图对象 fee_category_split_ratio（T5.14）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeCategorySplitRatioVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 费用大类ID
     */
    private Long categoryId;

    /**
     * 大类编码(冗余)
     */
    private String categoryCode;

    /**
     * 预算科目编码
     */
    private String subjectCode;

    /**
     * 预算科目名称(冗余)
     */
    private String subjectName;

    /**
     * 拆分比例%(同一大类合计=100)
     */
    private BigDecimal ratioPercent;
}
