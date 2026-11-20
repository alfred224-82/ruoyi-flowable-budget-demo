package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 合规协议签署 compliance_agreement（T25.5，小微版 M25）
 * <p>租户 Admin 首次登录强制签署，协议版本更新仅新租户需重签。
 * <p>UNIQUE(tenant_id, agreement_version) 防重复签署。
 * <p>注意：此表虽含 tenant_id 但协议由平台 Admin 管理，非租户级隔离场景。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compliance_agreement")
public class ComplianceAgreement extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 协议版本号（如 v1.0，平台方管理）
     */
    private String agreementVersion;

    /**
     * 协议内容URL/OSS路径
     */
    private String agreementContentUrl;

    /**
     * 签署人 user_id
     */
    private Long signerUserId;

    /**
     * 签署人姓名（冗余快照）
     */
    private String signerName;

    /**
     * 签署时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    /**
     * 签署时IP地址
     */
    private String signIp;
}
