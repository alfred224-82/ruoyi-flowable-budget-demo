package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预算归属预览响应对象
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
public class BudgetPreviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否匹配成功
     */
    private Boolean passed;

    /**
     * 预算科目编码
     */
    private String budgetSubject;

    /**
     * 预算科目名称
     */
    private String budgetSubjectName;

    /**
     * 会计大类
     */
    private String accountingClass;

    /**
     * 可用余额
     */
    private BigDecimal remainingBudget;

    /**
     * 是否固定资产(0否 1是)
     */
    private Integer isFixedAsset;

    /**
     * 失败消息
     */
    private String message;
}
