package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 合规协议签署视图对象（T25.5，小微版 M25）
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class ComplianceAgreementVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 租户名称（回填，非表字段） */
    private String tenantName;

    /** 协议版本号 */
    private String agreementVersion;

    /** 协议内容URL */
    private String agreementContentUrl;

    /** 签署人 user_id */
    private Long signerUserId;

    /** 签署人姓名 */
    private String signerName;

    /** 签署时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    /** 签署时IP */
    private String signIp;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // ===== 以下为前端签署检查用字段 =====

    /** 当前用户是否需要签署协议（非表字段，由 Service 回填） */
    private Boolean needSign;

    /** 当前协议版本号（非表字段，由 Service 回填） */
    private String currentVersion;
}
