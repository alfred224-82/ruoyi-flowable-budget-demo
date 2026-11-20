package com.ruoyi.system.service.message.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.service.message.IMessageProvider;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 企微自建应用消息 Provider（T13.3）
 * <p>基于 Hutool HTTP 直连企微 message/send 接口；
 * corpId/secret/agentId 取自 {@code wecom.app.*} 配置，未配置时返回 FAILED。
 * <p>access_token 缓存至 Redis（有效期 2h，企微官方 7200s）。
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Component
public class WecomAppMessageProvider implements IMessageProvider {

    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";
    private static final String REDIS_TOKEN_KEY = "wecom:app:access_token";
    private static final int TIMEOUT_MS = 10_000;

    @Value("${wecom.app.corpId:}")
    private String corpId;

    @Value("${wecom.app.secret:}")
    private String secret;

    @Value("${wecom.app.agentId:}")
    private String agentId;

    @Override
    public String providerName() {
        return WECOM_APP;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "企微 userid 为空，降级跳过");
        }
        if (StrUtil.isBlank(corpId) || StrUtil.isBlank(secret) || StrUtil.isBlank(agentId)) {
            return MessageResult.failed(providerName(), "企微应用未配置 corpId/secret/agentId（wecom.app.*）");
        }
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return MessageResult.failed(providerName(), "获取企微 access_token 失败");
            }
            JSONObject body = buildRequestBody(context);
            String resp = HttpUtil.post(SEND_URL + "?access_token=" + accessToken, body.toString(), TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            int errcode = json.getInt("errcode", -1);
            if (errcode == 0) {
                String msgId = json.getStr("msgid");
                log.info("[WecomApp] 企微应用消息发送成功 userid={} msgId={}", context.getReceiver(), msgId);
                return MessageResult.success(providerName(), msgId);
            }
            String errmsg = json.getStr("errmsg", "未知错误");
            log.warn("[WecomApp] 企微应用消息发送失败 userid={} errcode={} errmsg={}", context.getReceiver(), errcode, errmsg);
            return MessageResult.failed(providerName(), "errcode=" + errcode + " " + errmsg);
        } catch (Exception e) {
            log.warn("[WecomApp] 企微应用消息发送异常 userid={}", context.getReceiver(), e);
            return MessageResult.failed(providerName(), StrUtil.maxLength(e.getMessage(), 500));
        }
    }

    private JSONObject buildRequestBody(MessageContext context) {
        JSONObject body = new JSONObject();
        body.set("touser", context.getReceiver());
        body.set("msgtype", "text");
        body.set("agentid", Long.parseLong(agentId));
        JSONObject text = new JSONObject();
        StringBuilder content = new StringBuilder();
        if (StrUtil.isNotBlank(context.getTitle())) {
            content.append(context.getTitle()).append("\n");
        }
        if (StrUtil.isNotBlank(context.getContent())) {
            content.append(context.getContent());
        }
        text.set("content", content.toString());
        body.set("text", text);
        return body;
    }

    /**
     * 获取企微 access_token（Redis 缓存 110 分钟）
     */
    private String getAccessToken() {
        String cached = RedisUtils.getCacheObject(REDIS_TOKEN_KEY);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String url = TOKEN_URL + "?corpid=" + corpId + "&corpsecret=" + secret;
        try {
            String resp = HttpUtil.get(url, TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            String token = json.getStr("access_token");
            if (StrUtil.isNotBlank(token)) {
                RedisUtils.setCacheObject(REDIS_TOKEN_KEY, token, Duration.ofMinutes(110));
                return token;
            }
            log.error("[WecomApp] 获取 access_token 失败 resp={}", resp);
        } catch (Exception e) {
            log.error("[WecomApp] 获取 access_token 异常", e);
        }
        return null;
    }
}
