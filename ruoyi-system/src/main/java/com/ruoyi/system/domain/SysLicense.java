package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * License授权对象 sys_license
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("sys_license")
public class SysLicense implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 租户ID（NULL=系统级/平台级，非NULL=租户级，T3.10） */
    private Long tenantId;

    /** 授权编号（LIC-YYYY-NNNNNN） */
    private String licenseId;

    /** 授权类型（TRIAL/FORMAL） */
    private String licenseType;

    /** 客户名称 */
    private String customer;

    /** 版本等级（BASIC/ADVANCED/PROFESSIONAL，兼容存量 ENTERPRISE，T4.1） */
    private String edition;

    /** 试用天数（仅TRIAL有效） */
    private Integer trialDays;

    /** 授权行业列表JSON */
    private String industries;

    /** 最大租户数 */
    private Integer maxTenants;

    /** 功能开关列表JSON */
    private String features;

    /** 机器指纹哈希 */
    private String machineFingerprint;

    /** 签发日期 */
    private Date issuedAt;

    /** 到期日期（NULL=永久） */
    private Date expiresAt;

    /** License版本号（防降级回滚） */
    private Integer licenseVersion;

    /** RSA-SHA256数字签名 */
    private String signature;

    /** 状态（ACTIVE/EXPIRED/REVOKED/TAMPERED） */
    private String status;

    /** 首次激活时间 */
    private Date activatedAt;

    /** 最后校验时间 */
    private Date lastCheckTime;

    private Date createTime;
    private Date updateTime;
}
