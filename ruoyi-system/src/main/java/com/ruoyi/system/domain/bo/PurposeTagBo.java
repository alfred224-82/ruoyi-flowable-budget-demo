package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用途标签业务对象 fee_purpose_tag
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurposeTagBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 标签编码
     */
    @NotBlank(message = "标签编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String tagCode;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String tagName;

    /**
     * 标签图标
     */
    private String tagIcon;

    /**
     * 默认会计大类
     */
    private String defaultAccountingClass;

    /**
     * 是否要求项目编号(0否 1是)
     */
    private Integer requireProject;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;
}
