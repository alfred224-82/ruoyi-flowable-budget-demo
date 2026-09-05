package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放平台Webhook回调注册对象 open_api_webhook（T7.8 / FEAT_OPEN_API）
 * <p>事件订阅（审批通过/驳回/单据创建/预算预警等）；secret 加密存储，用于回调 HMAC-SHA256 验签。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_api_webhook")
public class OpenApiWebhook extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联凭证ID(open_api_credential) */
    private Long credentialId;

    /** 事件类型(APPROVAL_PASSED/APPROVAL_REJECTED/BILL_CREATED/BUDGET_ALERT) */
    private String eventType;

    /** 回调地址(HTTPS) */
    private String callbackUrl;

    /** 回调签名密钥(AES加密，HMAC-SHA256验签) */
    private String secret;

    /** 是否启用(0否 1是) */
    private Integer isEnabled;

    /** 失败重试次数 */
    private Integer retryTimes;

    /** 备注 */
    private String remark;
}
