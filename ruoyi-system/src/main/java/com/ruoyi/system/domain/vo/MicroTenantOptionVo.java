package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 手机号统一登录-租户候选项（T15.8）
 * <p>手机号跨多租户且未指定 tenantId 时返回，供前端弹出「租户/门店选择页」；用户选定后携 tenantId 二次登录。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroTenantOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private Long tenantId;

    /** 租户/门店名称（sys_tenant.company_name） */
    private String tenantName;

    /** 门店Logo（引导态公开展示） */
    private String logoUrl;

    /** 所在城市（引导态门店搜索/展示） */
    private String city;

    /** 该租户下当前用户的主角色编码（boss/salesman/customer） */
    private String roleCode;

    /** 该租户下角色显示名（tenant_role_mapping.display_name） */
    private String roleName;

    /** 版本档位（micro_lite/micro_pro，T15.36§6.1「我的店铺」列表展示） */
    private String versionType;

    /** 行业编码（sys_tenant.industry，T15.36§6.1「我的店铺」列表行业图标） */
    private String industryCode;

}
