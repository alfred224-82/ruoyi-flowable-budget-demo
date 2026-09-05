package com.ruoyi.system.service.message.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.message.IMessageProvider;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企微自建应用消息 Provider 桩实现（T13.2 / T13.3，默认降级）
 * <p>企微应用消息以 userid 为接收者，桩实现仅校验 receiver 非空后模拟成功；
 * 接入真实实现（企微 message/send）时新增同 providerName 的 Bean 并标注 @Primary 覆盖即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class WecomAppMessageStub implements IMessageProvider {

    @Override
    public String providerName() {
        return WECOM_APP;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "企微 userid 为空，降级跳过");
        }
        String messageId = "stub-wecom-" + IdUtil.fastSimpleUUID();
        log.info("[WecomAppStub] 模拟发送企微应用消息 bizType={} receiver={} → {}",
            context.getBizType(), context.getReceiver(), messageId);
        return MessageResult.success(providerName(), messageId);
    }
}
