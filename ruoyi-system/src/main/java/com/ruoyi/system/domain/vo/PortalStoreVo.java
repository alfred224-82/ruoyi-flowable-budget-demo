package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 引导态门店公开展示视图对象（T8.10 门店选择/搜索首页）
 * <p>仅暴露「公开展示字段」（名称/城市/地址/经纬度/Logo/简介/业态/通道），
 * 【严禁】包含 contact_phone / wecom_secret / mp_app_id 等敏感凭据——引导态未鉴权，任何人可访问。
 * <p>用户显式选定门店后进入鉴权态，由登录流程签发携带 tenant_id 的 JWT，绝不自动猜测租户。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class PortalStoreVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID（选定后用于登录） */
    private Long tenantId;

    /** 租户编码 */
    private String tenantCode;

    /** 企业/门店名称 */
    private String companyName;

    /** 所在城市 */
    private String city;

    /** 门店地址 */
    private String address;

    /** 门店经度 */
    private BigDecimal longitude;

    /** 门店纬度 */
    private BigDecimal latitude;

    /** 门店Logo URL */
    private String logoUrl;

    /** 门店公开简介 */
    private String storeIntro;

    /** 业态包编码 */
    private String bizCode;

    /** 访问通道：wechat/wecom */
    private String accessChannel;

    /** 距用户定位的距离（公里，就近排序时计算填充；未提供定位为 null） */
    private BigDecimal distanceKm;
}
