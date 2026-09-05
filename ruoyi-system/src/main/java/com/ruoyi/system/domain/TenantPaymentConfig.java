package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户收款配置对象 tenant_payment_config（T13.6 主体独立与收款分离）
 * <p>mode=self_merchant 收款直达自有商户号；mode=platform_proxy 平台代收后按虚拟账户分账（分账见 M14）。
 * apiKey 加密存储、接口返回脱敏；每租户唯一（uk_tenant）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_payment_config")
public class TenantPaymentConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 收款模式：self_merchant/platform_proxy */
    private String mode;

    /** 商户号(自有商户号模式) */
    private String merchantId;

    /** 商户名称 */
    private String merchantName;

    /** 商户API密钥(加密存储,接口返回脱敏) */
    private String apiKey;

    /** 结算账户(脱敏展示) */
    private String settleAccount;

    /** 状态（0启用 1停用） */
    private String status;
}
