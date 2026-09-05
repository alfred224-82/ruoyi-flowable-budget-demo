package com.ruoyi.system.service.message;

/**
 * 消息 Provider 抽象（T13.3）
 * <p>扩展点模式：微信公众号模板消息 / 微信客服消息 / 企微自建应用消息各一实现，
 * 默认提供桩实现（模拟发送 + 降级 SKIPPED），接入真实通道时新增实现并标注 @Primary 覆盖同名 Bean 即可。
 * <p>分发器按 {@link #providerName()} 注册，业务侧只声明 channel/bizType，不感知具体通道。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IMessageProvider {

    /** 微信模板消息 Provider 名 */
    String WECHAT_TEMPLATE = "wechat_template";
    /** 微信客服消息 Provider 名 */
    String WECHAT_KF = "wechat_kf";
    /** 企微自建应用消息 Provider 名 */
    String WECOM_APP = "wecom_app";

    /**
     * Provider 唯一名（与 biz_message_log.provider 一致）
     */
    String providerName();

    /**
     * 发送消息（失败/授权不足应返回 FAILED/SKIPPED，不抛异常阻塞主流程）
     */
    MessageResult send(MessageContext context);
}
