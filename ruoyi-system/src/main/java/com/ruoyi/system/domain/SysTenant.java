package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 租户信息对象 sys_tenant
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 租户编码（唯一）
     */
    private String tenantCode;

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 所在城市（引导态门店搜索/定位拒绝时城市降级选择，T8.10）
     */
    private String city;

    /**
     * 门店地址（引导态公开展示，T8.10）
     */
    private String address;

    /**
     * 门店经度（定位就近排序，T8.10）
     */
    private BigDecimal longitude;

    /**
     * 门店纬度（定位就近排序，T8.10）
     */
    private BigDecimal latitude;

    /**
     * 门店Logo URL（引导态公开展示，T8.10）
     */
    private String logoUrl;

    /**
     * 门店公开简介（未选定门店时的平台介绍页/公开展示，T8.10）
     */
    private String storeIntro;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 行业
     */
    private String industry;

    /**
     * 分配版本：BASIC/ADVANCED/PROFESSIONAL（兼容存量 ENTERPRISE，T4.1）
     */
    private String edition;

    /**
     * 产品线（T15.25 硬隔离 + T15.29 新建租户级联）：micro(小微版)/enterprise(企业版)。
     * <p>决定「可选角色列表」按 sys_role.tenant_type_scope 过滤的产品线归属。
     */
    private String tenantType;

    /**
     * 版本档位（T15.26 小微版分档）：micro_lite/micro_pro/basic/advanced/pro。
     * <p>★分档差异只体现在 role_template 勾选的菜单范围；lite→pro 升级 = UPDATE 本字段 + 刷新菜单缓存，零迁移。
     * micro_lite 永久免费(仅推广)；micro_pro 收费(全经营)；basic/advanced/pro 为企业线档位。
     */
    private String versionType;

    /**
     * 小程序全局主题key（T15.36 platform_admin 选定后全局换肤）：
     * theme_default/theme_warm/theme_fresh/theme_elegant
     */
    private String appThemeKey;

    /**
     * 业态包编码（biz_bundle.biz_code）：fund_control/boardgame/fresh_food（T1.8/T10.1）
     */
    private String bizCode;

    /**
     * 访问通道：wechat(公众号+小程序)/wecom(企业微信自建应用)（T1.8/T10.1）
     */
    private String accessChannel;

    /**
     * 企业微信 CorpID（FEAT_WECOM，T10.1）
     */
    private String wecomCorpId;

    /**
     * 企业微信自建应用 AgentId（FEAT_WECOM，T10.1）
     */
    private String wecomAgentId;

    /**
     * 企业微信自建应用 Secret（加密存储、接口返回脱敏，T10.1）
     */
    private String wecomSecret;

    /**
     * 微信公众号 AppID（引导态反查租户，不写 TenantContext，T10.1）
     */
    private String mpAppId;

    /**
     * 授权类型：TRIAL/FORMAL
     */
    private String licenseType;

    /**
     * 授权到期日
     */
    private Date expireDate;

    /**
     * 最大用户数
     */
    private Integer maxUsers;

    /**
     * 最大角色数（NULL=不限；BASIC=2/ADVANCED=6/PROFESSIONAL=NULL，T2.5/M2_09）
     */
    private Integer maxRoles;

    /**
     * 功能开关JSON列表
     */
    private String features;

    /**
     * 状态：PENDING/NORMAL/GRACE/DISABLED/EXPIRED
     * <p>GRACE = 到期后 3 天只读宽限期（T14.2）
     */
    private String status;

    /**
     * 只读宽限期截止日（T14.2：到期后 3 天内允许只读访问，NULL=未进入宽限期）
     */
    private Date gracePeriodUntil;

    /**
     * 租户管理员用户ID
     */
    private Long adminUserId;

    /**
     * 隔离模式：SHARED共享表/DEDICATED独立Schema（预留，FEAT_ISOLATED_SCHEMA）
     */
    private String isolationMode;

    /**
     * 备注
     */
    private String remark;
}
