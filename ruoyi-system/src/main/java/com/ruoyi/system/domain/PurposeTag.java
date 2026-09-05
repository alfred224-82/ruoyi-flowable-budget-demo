package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用途标签对象 fee_purpose_tag
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_purpose_tag")
public class PurposeTag extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 标签编码
     */
    private String tagCode;

    /**
     * 标签名称
     */
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
