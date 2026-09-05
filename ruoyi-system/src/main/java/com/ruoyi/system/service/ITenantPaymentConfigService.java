package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.TenantPaymentConfigBo;
import com.ruoyi.system.domain.vo.TenantPaymentConfigVo;

/**
 * 租户收款配置服务接口（T13.6 主体独立与收款分离）
 * <p>mode=self_merchant 收款直达自有商户号；mode=platform_proxy 平台代收后按虚拟账户分账（分账见 M14）。
 * apiKey 加密存储、接口返回脱敏；每租户唯一（uk_tenant）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ITenantPaymentConfigService {

    /** 收款模式：自有商户号（收款直达） */
    String MODE_SELF_MERCHANT = "self_merchant";
    /** 收款模式：平台代收（按虚拟账户分账） */
    String MODE_PLATFORM_PROXY = "platform_proxy";

    /**
     * 查询当前租户收款配置（apiKey 脱敏），无则返回 null
     */
    TenantPaymentConfigVo getConfig();

    /**
     * 保存（新增或更新）当前租户收款配置（apiKey 加密存储，幂等 upsert）
     *
     * @return 影响行数
     */
    int saveConfig(TenantPaymentConfigBo bo);

    /**
     * 取当前租户收款模式（供 M14 分账/结算判定），无配置默认 platform_proxy
     */
    String getMode();
}
