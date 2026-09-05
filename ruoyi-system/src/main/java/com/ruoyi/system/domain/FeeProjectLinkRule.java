package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目关联规则对象 fee_project_link_rule（T5.7 / ADR-023）
 * 三层：角色级硬规则 / 大类级软提示 / 项目级
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project_link_rule")
public class FeeProjectLinkRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 作用域(ROLE/CATEGORY/PROJECT)
     */
    private String scope;

    /**
     * 目标（角色标识/大类ID/项目ID）
     */
    private String target;

    /**
     * 规则(REQUIRED必填/OPTIONAL可选/NO_PROMPT不提示)
     */
    private String rule;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
