package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 手机号统一登录响应（T15.8）
 * <p>两种互斥形态：
 * <ol>
 *   <li>{@link #needSelectTenant}=true：手机号跨多租户且未指定门店，返回 {@link #tenants} 候选，token 为空，前端弹租户选择页；</li>
 *   <li>{@link #needSelectTenant}=false：登录成功，返回 token + permissions + roles + 按 client_type 过滤的菜单树。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroLoginVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否需要选择租户（多租户未指定门店时为 true，此时不下发 token） */
    private boolean needSelectTenant;

    /** 租户候选列表（needSelectTenant=true 时返回） */
    private List<MicroTenantOptionVo> tenants;

    /** 登录令牌（needSelectTenant=false 时返回） */
    private String token;

    /** 选定/归属租户ID */
    private Long tenantId;

    /** 当前租户下主角色编码（boss/salesman/customer；无 user_tenant_role 的顾客默认 customer） */
    private String roleCode;

    /** 权限标识集合（sys_role_menu 派生，与既有登录一致） */
    private Set<String> permissions;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 菜单/路由树（按 client_type 过滤：mini 仅 mini+both，pc 仅 pc+both） */
    private List<RouterVo> menus;

    /** 当前租户版本档位（micro_pro/micro_lite/basic/advanced/pro，前端据此条件渲染 UI） */
    private String versionType;

}
