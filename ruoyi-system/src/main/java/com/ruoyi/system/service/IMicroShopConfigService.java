package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.MicroShopConfigBo;
import com.ruoyi.system.domain.vo.MicroShopConfigVo;

/**
 * 小微版店铺管理配置服务（T15.24，老板 PC）
 * <p>营业时间/收款方式说明/通知方式设置持久化到 {@code tenant_biz_config.module_overrides} JSON，
 * 复用 {@link ITenantBizConfigService}（单店差异统一走该表，无独立建表）；按当前登录租户读写。
 * <p>★V1 零支付合规：收款方式说明仅提示文案，不绑定支付账户。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IMicroShopConfigService {

    /**
     * 读取当前租户店铺配置（无配置时各字段为 null）
     */
    MicroShopConfigVo getConfig();

    /**
     * 合并保存店铺配置（仅覆盖 bo 中非 null 字段）
     *
     * @return 影响行数
     */
    int saveConfig(MicroShopConfigBo bo);

    /**
     * 保存营业时间（micro:shop:hours）
     */
    int saveBusinessHours(String businessHours);

    /**
     * 保存收款方式说明（micro:shop:payInfo，★仅提示文案）
     */
    int savePayInfo(String payInfo);

    /**
     * 保存通知方式设置（micro:notify:config）
     */
    int saveNotifyConfig(String notifyConfig);
}
