package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算科目对象 budget_subject
 *
 * @author ruoyi
 * @date 2026-05-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_subject")
public class BudgetSubject extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 科目编码（唯一，格式：一级1001，二级1001001）
     */
    private String subjectCode;
    /**
     * 科目名称
     */
    private String subjectName;
    /**
     * 父级科目ID（顶级为0）
     */
    private Long parentId;
    /**
     * 父级科目编码（冗余字段，便于查询）
     */
    private String parentCode;
    /**
     * 科目层级（1-一级科目，2-二级科目，3-三级科目）
     */
    private Long level;
    /**
     * 祖级列表（如：0/1001/1001001）
     */
    private String ancestors;
    /**
     * 是否叶子节点（0-否有子科目，1-是无子科目）
     */
    private Integer isLeaf;
    /**
     * 排序号（同级科目内的显示顺序）
     */
    private Long sortOrder;
    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer isActive;
    /**
     * 科目说明
     */
    private String description;
    /**
     * 备注
     */
    private String remark;

    /**
     * 科目类型（INCOME-收入类，COST-成本类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类）
     */
    private String subjectType;

}
