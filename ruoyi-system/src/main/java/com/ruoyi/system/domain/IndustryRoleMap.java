package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行业角色映射对象 industry_role_map（T15.2/T15.10）
 * <p>业务角色显示名（桌游老板/助教/顾客/配送员/教练/技师）→ 标准角色 code（sys_role.role_key：boss/salesman/customer）。
 * <p>系统级共享表（无 tenant_id，纳入 {@code MybatisPlusConfig.IGNORE_TENANT_TABLES}）；
 * 表仅含 create_time/update_time（无 create_by/update_by/del_flag），故不继承 BaseEntity（与 ProjectMember 同策略）。
 * uk_industry_bizrole(industry_code,biz_role_name) 保证同行业业务角色名唯一。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@TableName("industry_role_map")
public class IndustryRoleMap implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 行业编码（FK→industry_template.industry_code）
     */
    private String industryCode;

    /**
     * 业务角色显示名（桌游老板/助教/顾客/配送员/教练/技师）
     */
    private String bizRoleName;

    /**
     * 映射到的标准角色 code（sys_role.role_key：boss/salesman/customer）
     */
    private String roleCode;

    /**
     * 是否该行业默认启用角色（0否 1是）
     */
    private Integer defaultFlag;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
