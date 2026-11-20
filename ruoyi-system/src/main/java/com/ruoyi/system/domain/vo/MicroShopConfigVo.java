package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 小微版店铺管理配置视图对象（T15.24，老板 PC）
 * <p>存储于 {@code tenant_biz_config.module_overrides} JSON（键 businessHours/payInfo/notifyConfig），
 * 单店差异一律走该表（禁止 fork 业态包，设计文档 1.8.5），无独立建表。
 * <p>★V1 零支付合规：{@code payInfo} 仅为「到店付/老板收款码」提示文案，不绑定任何支付账户。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroShopConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 营业时间（文案，如「周一至周日 09:00-21:00」） */
    private String businessHours;

    /** 收款方式说明（★仅提示文案：到店付/老板收款码，不绑定支付账户） */
    private String payInfo;

    /** 通知方式设置（文案或 JSON 串，如「结算提醒/预警经短信+小程序通知」） */
    private String notifyConfig;
}
