package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放平台API凭证视图对象 open_api_credential（T13.5）
 * <p>appSecret 接口返回一律脱敏（仅创建时一次性明文返回，见 OpenApiCredentialBo.plainSecret）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiCredentialVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** AppKey(对外) */
    private String appKey;

    /** AppSecret(脱敏展示) */
    private String appSecret;

    /** 应用名称 */
    private String appName;

    /** 日调用配额 */
    private Integer quotaDaily;

    /** 月调用配额 */
    private Integer quotaMonthly;

    /** IP白名单 */
    private String ipWhitelist;

    /** 状态(ACTIVE启用/DISABLED停用) */
    private String status;

    /** 过期时间(空=永久有效) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 最近调用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCallTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** AppSecret 明文(仅创建/重置时一次性返回，列表/详情脱敏) */
    private String plainSecret;
}
