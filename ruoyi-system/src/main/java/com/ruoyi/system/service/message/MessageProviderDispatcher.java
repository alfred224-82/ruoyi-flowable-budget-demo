package com.ruoyi.system.service.message;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.message.impl.WechatTemplateMessageStub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息 Provider 分发器（T13.3）
 * <p>按 providerName 注册所有 IMessageProvider；解析优先级：显式 provider &gt; channel 推断 &gt; 默认（微信模板）。
 * channel→默认 Provider 映射：wecom→wecom_app；wechat→wechat_template。
 * 全程不出现 if(租户==...) 分支：新增通道只需新增 Provider Bean。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class MessageProviderDispatcher {

    /** 通道：微信 */
    public static final String CHANNEL_WECHAT = "wechat";
    /** 通道：企业微信 */
    public static final String CHANNEL_WECOM = "wecom";

    private final List<IMessageProvider> providers;

    /** Provider 注册表：providerName → 实现 */
    private final Map<String, IMessageProvider> providerMap = new HashMap<>();

    public MessageProviderDispatcher(List<IMessageProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    public void init() {
        for (IMessageProvider provider : providers) {
            providerMap.put(provider.providerName(), provider);
        }
        log.info("[MessageProvider] 已注册消息 Provider {} 个：{}", providerMap.size(), providerMap.keySet());
    }

    /**
     * 解析目标 Provider：显式 provider 优先，否则按 channel 推断，最终回落微信模板
     */
    public IMessageProvider resolve(MessageContext context) {
        if (context != null && StrUtil.isNotBlank(context.getProvider())) {
            IMessageProvider provider = providerMap.get(context.getProvider());
            if (provider != null) {
                return provider;
            }
            log.warn("[MessageProvider] 指定的 Provider {} 未注册，回落通道推断", context.getProvider());
        }
        String byChannel = inferByChannel(context == null ? null : context.getChannel());
        IMessageProvider provider = providerMap.get(byChannel);
        if (provider != null) {
            return provider;
        }
        IMessageProvider def = providerMap.get(WechatTemplateMessageStub.WECHAT_TEMPLATE);
        if (def == null) {
            throw new IllegalStateException("默认消息 Provider wechat_template 未注册");
        }
        return def;
    }

    /**
     * 发送：解析 Provider 并执行；Provider 异常统一降级为 FAILED，不向上抛出阻塞主流程
     */
    public MessageResult send(MessageContext context) {
        IMessageProvider provider = resolve(context);
        try {
            return provider.send(context);
        } catch (Exception e) {
            log.warn("[MessageProvider] 发送异常降级 provider={}", provider.providerName(), e);
            return MessageResult.failed(provider.providerName(), StrUtil.maxLength(e.getMessage(), 500));
        }
    }

    private String inferByChannel(String channel) {
        if (CHANNEL_WECOM.equalsIgnoreCase(channel)) {
            return IMessageProvider.WECOM_APP;
        }
        return IMessageProvider.WECHAT_TEMPLATE;
    }
}
