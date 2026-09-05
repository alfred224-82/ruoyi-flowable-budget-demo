package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 开放平台API凭证业务对象 open_api_credential（T7.8 / FEAT_OPEN_API）
 * <p>新增时 appKey/appSecret 由服务层生成（appSecret 加密存储，仅创建响应一次性返回明文）；
 * 编辑仅允许改 appName/配额/IP白名单/过期时间/状态/备注（Webhook 为独立表 open_api_webhook，另见 OpenApiWebhookBo）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiCredentialBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "凭证ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 应用名称 */
    @NotBlank(message = "应用名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String appName;

    /** 日调用配额(NULL或<=0表示不限) */
    private Integer quotaDaily;

    /** 月调用配额(NULL或<=0表示不限) */
    private Integer quotaMonthly;

    /** IP白名单(逗号分隔,空=不限) */
    private String ipWhitelist;

    /** 状态(ACTIVE启用/DISABLED停用) */
    private String status;

    /** 过期时间(空=永久有效) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 备注 */
    private String remark;
}
