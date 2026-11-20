package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色模板对象 role_template（T15.1/T15.7，v1.13 小微版新增）
 * <p>系统级共享表（无 tenant_id，纳入 {@code MybatisPlusConfig.IGNORE_TENANT_TABLES}）；无 del_flag，故继承 {@link BaseEntity} 而非 TenantEntity。
 * <p>按「版本类型 × 标准角色」预置菜单 + 区块配方：
 * <ul>
 *   <li>{@code version_type}：micro(小微版) / basic / advanced / professional(企业版各版本)；</li>
 *   <li>{@code menu_keys}：绑定菜单集合（perms 数组 JSON）；</li>
 *   <li>{@code block_keys}：绑定小程序区块集合（page_block_config.block_key 数组 JSON）。</li>
 * </ul>
 * <p>运行时：role_code + version_type → role_template → menu_keys → 按 sys_menu.client_type 端过滤。
 * 同一 boss 角色在小微版绑小微菜单、在企业版绑企业版菜单（靠 version_type 区分，不新建角色 code）。
 * uk_version_role(version_type,role_code) 保证同版本同角色仅一条配方。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_template")
public class RoleTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 版本类型：micro/basic/advanced/professional
     */
    private String versionType;

    /**
     * 标准角色编码（sys_role.role_key）
     */
    private String roleCode;

    /**
     * 绑定菜单集合（perms 数组 JSON）
     */
    private String menuKeys;

    /**
     * 绑定小程序区块集合（page_block_config.block_key 数组 JSON）
     */
    private String blockKeys;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
