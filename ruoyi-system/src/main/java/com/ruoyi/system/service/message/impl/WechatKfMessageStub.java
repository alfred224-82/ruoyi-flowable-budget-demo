package com.ruoyi.system.service.message.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.message.IMessageProvider;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信客服消息 Provider 桩实现（T13.3，默认降级）
 * <p>客服消息需 48h 内有互动会话，桩实现仅校验 receiver 非空后模拟成功；
 * 接入真实实现时新增同 providerName 的 Bean 并标注 @Primary 覆盖即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class WechatKfMessageStub implements IMessageProvider {

    @Override
    public String providerName() {
        return WECHAT_KF;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "openid 为空，无可用客服会话，降级跳过");
        }
        String messageId = "stub-kf-" + IdUtil.fastSimpleUUID();
        log.info("[WechatKfStub] 模拟发送客服消息 bizType={} receiver={} → {}",
            context.getBizType(), context.getReceiver(), messageId);
        return MessageResult.success(providerName(), messageId);
    }
}
