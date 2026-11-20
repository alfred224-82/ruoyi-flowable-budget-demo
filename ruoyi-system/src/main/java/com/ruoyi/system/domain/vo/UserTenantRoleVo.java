package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户 × 租户 × 角色关联视图对象 user_tenant_role（T15.1/T15.9）
 * <p>{@code userName/nickName/phonenumber/roleName} 为服务层批量回填的展示字段（非表列），便于「角色分配」列表直接呈现。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class UserTenantRoleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 标准角色编码（sys_role.role_key） */
    private String roleCode;

    /** 所选租户角色映射ID（tenant_role_mapping.id；展示侧优先取该映射显示名，编辑弹窗回填下拉） */
    private Long roleMappingId;

    /** 是否该租户下主角色（0否 1是） */
    private Integer isPrimary;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 用户账号（展示用，来自 sys_user） */
    private String userName;

    /** 用户昵称（展示用，来自 sys_user） */
    private String nickName;

    /** 头像URL（展示用，来自 sys_user） */
    private String avatar;

    /** 邮箱（展示用，来自 sys_user，编辑弹窗回填） */
    private String email;

    /** 性别（0男 1女 2未知，展示用，来自 sys_user，编辑弹窗回填） */
    private String sex;

    /** 描述（展示用，来自 sys_user.remark，编辑弹窗回填） */
    private String description;

    /** 手机号（展示用，来自 sys_user，@Sensitive 脱敏输出） */
    private String phonenumber;

    /** 租户内角色显示名（展示用，来自 tenant_role_mapping.display_name） */
    private String roleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
