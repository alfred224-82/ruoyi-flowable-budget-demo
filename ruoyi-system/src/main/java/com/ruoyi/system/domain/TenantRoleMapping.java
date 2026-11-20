package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户角色映射对象 tenant_role_mapping（T15.1/T15.9，v1.13 小微版新增，全版本通用）
 * <p>平台管理员从标准角色集勾选本租户需要的角色 → 改名（display_name，如 salesman→「助教」）→ 启停（status）→ 排序（sort_order）。
 * <p>★平台管理员只能做角色映射，不能新建角色 code、不能改角色权限（菜单/区块由 admin 在 role_template 预置）。
 * <p>租户级表（含 tenant_id），由多租户插件自动行级隔离；uk_tenant_role_code_name(tenant_id,role_code,display_name)
 * 保证同租户同显示名仅一条。★同 role_code 允许并存多个行业业务名行（如旅游 salesman 的「导游」「司机」，M15_31 放宽）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_role_mapping")
public class TenantRoleMapping extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标准角色编码（sys_role.role_key）
     */
    private String roleCode;

    /**
     * 租户内显示名（如 salesman→"助教"、customer→"顾客"）
     */
    private String displayName;

    /**
     * 显示排序
     */
    private Integer sortOrder;

    /**
     * 状态（0启用 1禁用；不用的标准角色直接禁用）
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
