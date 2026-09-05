package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 开放平台API凭证对象 open_api_credential（T7.8 / T13.5，专业版 FEAT_OPEN_API）
 * <p>AppKey 对外，AppSecret 加密存储、接口返回脱敏；HMAC-SHA256 签名鉴权 + 配额限流。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_api_credential")
public class OpenApiCredential extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** AppKey(对外) */
    private String appKey;

    /** AppSecret(加密存储,接口返回脱敏) */
    private String appSecret;

    /** 应用名称 */
    private String appName;

    /** 日调用配额 */
    private Integer quotaDaily;

    /** 月调用配额 */
    private Integer quotaMonthly;

    /** IP白名单(逗号分隔,空=不限) */
    private String ipWhitelist;

    /** 状态(ACTIVE启用/DISABLED停用) */
    private String status;

    /** 过期时间(空=永久有效) */
    private Date expireTime;

    /** 最近调用时间 */
    private Date lastCallTime;

    /** 备注 */
    private String remark;
}
