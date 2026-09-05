package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 大类默认拆分比例对象 fee_category_split_ratio（T5.14）
 * 财务设置；编制期无历史实绩时按此比例将大类额度拆分到预算科目
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_category_split_ratio")
public class FeeCategorySplitRatio extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
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
