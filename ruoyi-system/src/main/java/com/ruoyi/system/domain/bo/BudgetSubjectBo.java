package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算科目业务对象 budget_subject
 *
 * @author ruoyi
 * @date 2026-05-30
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BudgetSubjectBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 科目编码（唯一，格式：一级1001，二级1001001）
     */
    @NotBlank(message = "科目编码（唯一，格式：一级1001，二级1001001）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String subjectCode;

    /**
     * 科目名称
     */
    @NotBlank(message = "科目名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String subjectName;

    /**
     * 父级科目ID（顶级为0）
     */
    @NotNull(message = "父级科目ID（顶级为0）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long parentId;

    /**
     * 父级科目编码（冗余字段，便于查询）
     */
    @NotBlank(message = "父级科目编码（冗余字段，便于查询）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String parentCode;

    /**
     * 科目层级（1-一级科目，2-二级科目，3-三级科目）
     */
    @NotNull(message = "科目层级（1-一级科目，2-二级科目，3-三级科目）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long level;

    /**
     * 祖级列表（如：0/1001/1001001）
     */
    @NotBlank(message = "祖级列表（如：0/1001/1001001）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String ancestors;

    /**
     * 是否叶子节点（0-否有子科目，1-是无子科目）
     */
    @NotNull(message = "是否叶子节点（0-否有子科目，1-是无子科目）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer isLeaf;

    /**
     * 排序号（同级科目内的显示顺序）
     */
    @NotNull(message = "排序号（同级科目内的显示顺序）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long sortOrder;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    @NotNull(message = "是否启用（0-禁用，1-启用）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer isActive;

    /**
     * 科目说明
     */
    @NotBlank(message = "科目说明不能为空", groups = { AddGroup.class, EditGroup.class })
    private String description;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;

    /**
     * 科目类型（INCOME-收入类，COST-成本类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类）
     */
    private String subjectType;

}
