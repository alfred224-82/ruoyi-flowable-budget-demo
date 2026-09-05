package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目预算科目视图对象 fee_project_subject（T5.1）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectSubjectVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 费用大类ID */
    private Long categoryId;
    /** 费用大类名称 */
    private String categoryName;
    /** 预算科目ID */
    private Long budgetSubjectId;
    /** 科目名称 */
    private String subjectName;
    /** 项目级预算金额 */
    private BigDecimal budgetAmount;
    /** 已用金额 */
    private BigDecimal usedAmount;
    /** 剩余金额(计算字段) */
    private BigDecimal remainAmount;
    /** 是否启用 */
    private Integer isActive;
    /** 创建时间 */
    private Date createTime;
}
