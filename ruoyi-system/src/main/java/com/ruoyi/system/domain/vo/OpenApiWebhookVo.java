package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放平台Webhook回调注册视图对象 open_api_webhook（T7.8 / FEAT_OPEN_API）
 * <p>secret 接口返回一律脱敏（仅创建/重置时一次性明文返回）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiWebhookVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联凭证ID */
    private Long credentialId;

    /** 关联凭证AppKey（填充） */
    private String appKey;

    /** 关联凭证应用名称（填充） */
    private String appName;

    /** 事件类型 */
    private String eventType;

    /** 回调地址 */
    private String callbackUrl;

    /** 回调签名密钥(脱敏展示) */
    private String secret;

    /** 是否启用(0否 1是) */
    private Integer isEnabled;

    /** 失败重试次数 */
    private Integer retryTimes;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
