package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户 × 租户 × 角色关联对象 user_tenant_role（T15.1，v1.13 小微版新增）
 * <p>支持「一人多租户、一租户多角色」：手机号为主键身份模型（一个手机号 = 一个 sys_user，跨端跨租户唯一）。
 * <p>租户级表（含 tenant_id + idx_tenant_id），由多租户插件自动行级隔离；
 * 登录跨租户查询时由 {@link com.ruoyi.common.helper.TenantHelper} 显式忽略过滤（与 user_tenant_bind 同策略，不入 IGNORE_TENANT_TABLES）。
 * <p>uk_tenant_user_role(tenant_id,user_id,role_code) 保证同租户同用户同角色仅一条。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_tenant_role")
public class UserTenantRole extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（sys_user.user_id，手机号为主键）
     */
    private Long userId;

    /**
     * 标准角色编码（sys_role.role_key：boss/salesman/customer/...）
     */
    private String roleCode;

    /**
     * 所选租户角色映射ID（tenant_role_mapping.id；同 role_code 多业务名时区分导游/司机，空则回退首个显示名）
     */
    private Long roleMappingId;

    /**
     * 是否该租户下主角色（0否 1是；多角色时默认视图）
     */
    private Integer isPrimary;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
