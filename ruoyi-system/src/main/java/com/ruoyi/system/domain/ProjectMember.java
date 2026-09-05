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
 * 项目容器成员对象 project_member（T11.2 五枚举角色）
 * <p>role 与 sys_role 解耦：owner/manager/member/participant/signup（见 {@link com.ruoyi.system.domain.enums.ProjectMemberRole}）。
 * uk_project_user 保证同一容器内一个用户仅一条成员记录。
 * 表仅含 create_time/update_time（无 create_by/update_by），故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("project_member")
public class ProjectMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 项目容器ID（FK→project）
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 容器角色：owner/manager/member/participant/signup（与 sys_role 解耦）
     */
    private String role;

    /**
     * 加入时间
     */
    private Date joinTime;

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
