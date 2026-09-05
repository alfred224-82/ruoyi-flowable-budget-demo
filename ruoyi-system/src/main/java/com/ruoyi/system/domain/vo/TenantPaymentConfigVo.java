package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户收款配置视图对象 tenant_payment_config（T13.6）
 * <p>apiKey 接口返回一律脱敏；settleAccount 已脱敏展示。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class TenantPaymentConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 收款模式：self_merchant/platform_proxy */
    private String mode;

    /** 商户号 */
    private String merchantId;

    /** 商户名称 */
    private String merchantName;

    /** 商户API密钥(脱敏展示) */
    private String apiKey;

    /** 结算账户(脱敏展示) */
    private String settleAccount;

    /** 状态（0启用 1停用） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
