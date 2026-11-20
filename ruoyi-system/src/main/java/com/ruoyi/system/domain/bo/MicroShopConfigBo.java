package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * 小微版店铺管理配置业务对象（T15.24，老板 PC）
 * <p>用于合并写入 {@code tenant_biz_config.module_overrides}：非 null 字段才覆盖，null 字段保持原值。
 * <p>★V1 零支付合规：{@code payInfo} 仅提示文案，不绑定支付账户；本对象不含任何支付账户/密钥字段。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroShopConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 营业时间（文案） */
    private String businessHours;

    /** 收款方式说明（★仅提示文案，不绑定支付账户） */
    private String payInfo;

    /** 通知方式设置（文案或 JSON 串） */
    private String notifyConfig;
}
