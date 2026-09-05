package com.ruoyi.system.service.message.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.message.IMessageProvider;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信公众号模板消息 Provider 桩实现（T13.3，默认降级）
 * <p>未接入真实公众号前的可扩展桩：receiver(openid) 为空视为订阅授权不足 → SKIPPED；否则模拟成功并返回占位 messageId。
 * 接入真实实现时新增同 providerName 的 Bean 并标注 @Primary 覆盖即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class WechatTemplateMessageStub implements IMessageProvider {

    @Override
    public String providerName() {
        return WECHAT_TEMPLATE;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "openid 为空或用户未订阅，降级跳过");
        }
        String messageId = "stub-tpl-" + IdUtil.fastSimpleUUID();
        log.info("[WechatTemplateStub] 模拟发送模板消息 bizType={} receiver={} templateId={} → {}",
            context.getBizType(), context.getReceiver(), context.getTemplateId(), messageId);
        return MessageResult.success(providerName(), messageId);
    }
}
