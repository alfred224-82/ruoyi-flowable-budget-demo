package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目关联规则视图对象 fee_project_link_rule（T5.7）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectLinkRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 作用域(ROLE/CATEGORY/PROJECT) */
    private String scope;
    /** 目标（角色标识/大类ID/项目ID） */
    private String target;
    /** 规则(REQUIRED/OPTIONAL/NO_PROMPT) */
    private String rule;
    /** 是否启用 */
    private Integer isEnabled;
    /** 创建时间 */
    private Date createTime;
}
