package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户 × 租户 × 角色关联业务对象 user_tenant_role（T15.9 分配用户）
 * <p>支持单角色分配（{@code roleCode}）与批量分配（{@code roleCodes}）；
 * 分配即写入 user_tenant_role，同租户同用户同角色幂等（uk_tenant_user_role）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class UserTenantRoleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "关联ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 用户ID（sys_user.user_id） */
    @NotNull(message = "用户不能为空", groups = {AddGroup.class})
    private Long userId;

    /** 标准角色编码（单角色分配） */
    private String roleCode;

    /** 所选租户角色映射ID（tenant_role_mapping.id；同 role_code 多业务名时记录具体所选行，如导游/司机） */
    private Long roleMappingId;

    /** 标准角色编码集合（批量分配，优先于 roleCode） */
    private List<String> roleCodes;

    /** 是否主角色（0否 1是） */
    private Integer isPrimary;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 手机号（platform_admin 新增人员入口：按手机号查找或自动创建用户） */
    private String phonenumber;

    /** 登录用户名（platform_admin 新增人员入口：创建用户时设置，用于 PC 端登录） */
    private String userName;

    /** 登录密码（platform_admin 新增人员入口：创建用户时设置，留空则默认 123456） */
    private String password;

    /** 用户昵称（platform_admin 新增人员入口：创建用户时设置） */
    private String nickName;

    /** 描述（platform_admin 新增人员入口：创建用户时写入 remark） */
    private String description;

    /** 头像URL（platform_admin 新增人员入口：创建用户时设置） */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 性别（0男 1女 2未知） */
    private String sex;
}
