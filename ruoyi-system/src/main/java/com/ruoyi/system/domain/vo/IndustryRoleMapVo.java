package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行业角色映射视图对象 industry_role_map（T15.2/T15.10）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class IndustryRoleMapVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 行业编码 */
    private String industryCode;

    /** 业务角色显示名（桌游老板/助教/顾客...） */
    private String bizRoleName;

    /** 映射到的标准角色 code（boss/salesman/customer） */
    private String roleCode;

    /** 是否该行业默认启用角色（0否 1是） */
    private Integer defaultFlag;

    /** 排序 */
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
