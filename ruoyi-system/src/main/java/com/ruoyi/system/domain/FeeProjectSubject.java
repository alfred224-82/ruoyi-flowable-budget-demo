package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 项目预算科目对象 fee_project_subject（T5.1）
 * 审批通过生成并汇入大类
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project_subject")
public class FeeProjectSubject extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 费用大类ID
     */
    private Long categoryId;

    /**
     * 费用大类名称
     */
    private String categoryName;

    /**
     * 预算科目ID
     */
    private Long budgetSubjectId;

    /**
     * 科目名称(可覆盖)
     */
    private String subjectName;

    /**
     * 项目级预算金额
     */
    private BigDecimal budgetAmount;

    /**
     * 已用金额
     */
    private BigDecimal usedAmount;

    /**
     * 是否启用
     */
    private Integer isActive;
}
