package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户协议同意留痕视图对象 sys_user_agreement_log（T14.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysUserAgreementLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 微信 openid */
    private String openid;

    /** 协议类型：privacy/user */
    private String agreementType;

    /** 同意的版本号 */
    private String version;

    /** 来源IP */
    private String clientIp;

    /** 同意时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date agreeTime;
}
