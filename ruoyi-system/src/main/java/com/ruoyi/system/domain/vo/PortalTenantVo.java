package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 引导态门店（租户）视图对象（T13.1 双阶段租户识别 - 引导态）
 * <p>公众号总入口/登录页按 openid 或 mpAppId 反查可进入的门店列表，供用户【显式选定】；
 * 绝不自动猜测或写入 TenantContext，选定后进入鉴权态由 JWT 携带 tenant_id。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class PortalTenantVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private Long tenantId;

    /** 租户编码 */
    private String tenantCode;

    /** 企业/门店名称 */
    private String companyName;

    /** 访问通道：wechat/wecom */
    private String accessChannel;

    /** 业态包编码 */
    private String bizCode;

    /** 是否默认门店（0否 1是） */
    private Integer isDefault;

    /** 租户状态：NORMAL 等 */
    private String status;
}
