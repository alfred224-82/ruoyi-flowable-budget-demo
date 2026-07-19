package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 映射规则业务对象 fee_mapping_rule
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MappingRuleBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 规则编码
     */
    @NotBlank(message = "规则编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String ruleCode;

    /**
     * 物资小类编码
     */
    @NotBlank(message = "物资小类编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @NotBlank(message = "用途标签编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String purposeTag;

    /**
     * 目标预算科目编码
     */
    @NotBlank(message = "目标预算科目编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String budgetSubject;

    /**
     * 归属会计大类
     */
    @NotBlank(message = "归属会计大类不能为空", groups = {AddGroup.class, EditGroup.class})
    private String accountingClass;

    /**
     * 是否固定资产(0否 1是)
     */
    private Integer isFixedAsset;

    /**
     * 优先级(数值越大优先级越高)
     */
    private Integer priority;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;
}
