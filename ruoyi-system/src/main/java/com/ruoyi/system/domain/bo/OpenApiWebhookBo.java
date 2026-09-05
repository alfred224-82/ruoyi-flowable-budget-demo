package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 开放平台Webhook回调注册业务对象 open_api_webhook（T7.8 / FEAT_OPEN_API）
 * <p>新增时 secret 由服务层生成（加密存储，仅创建响应一次性返回明文）；编辑不改 secret。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiWebhookBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "Webhook ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 关联凭证ID */
    @NotNull(message = "关联凭证不能为空", groups = {AddGroup.class})
    private Long credentialId;

    /** 事件类型(APPROVAL_PASSED/APPROVAL_REJECTED/BILL_CREATED/BUDGET_ALERT) */
    @NotBlank(message = "事件类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String eventType;

    /** 回调地址(HTTPS) */
    @NotBlank(message = "回调地址不能为空", groups = {AddGroup.class, EditGroup.class})
    private String callbackUrl;

    /** 是否启用(0否 1是) */
    private Integer isEnabled;

    /** 失败重试次数 */
    private Integer retryTimes;

    /** 备注 */
    private String remark;
}
