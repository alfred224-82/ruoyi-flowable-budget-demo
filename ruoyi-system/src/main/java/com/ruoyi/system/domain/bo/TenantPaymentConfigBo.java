package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 租户收款配置业务对象 tenant_payment_config（T13.6）
 * <p>apiKey 明文入参，服务层加密存储；每租户唯一（uk_tenant）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class TenantPaymentConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（编辑时携带，可空由服务层按租户 upsert） */
    private Long id;

    /** 收款模式：self_merchant/platform_proxy */
    @NotBlank(message = "收款模式不能为空", groups = {AddGroup.class})
    private String mode;

    /** 商户号(自有商户号模式) */
    private String merchantId;

    /** 商户名称 */
    private String merchantName;

    /** 商户API密钥(明文入参，服务层加密存储) */
    private String apiKey;

    /** 结算账户 */
    private String settleAccount;

    /** 状态（0启用 1停用） */
    private String status;
}
