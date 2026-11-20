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
 * 微信客服消息 Provider（T13.3）
 * <p>客服消息需 48h 内有用户互动会话；基于 Hutool HTTP 直连微信客服消息接口；
 * appid/secret 与公众号模板消息共用 {@code wx.official.*} 配置。
 * <p>access_token 缓存至 Redis（有效期 2h），与模板消息共用同一公众号。
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Component
public class WechatKfMessageProvider implements IMessageProvider {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send";
    private static final String REDIS_TOKEN_KEY = "wx:official:access_token";
    private static final int TIMEOUT_MS = 10_000;

    @Value("${wx.official.appid:}")
    private String appid;

    @Value("${wx.official.secret:}")
    private String secret;

    @Override
    public String providerName() {
        return WECHAT_KF;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "openid 为空，无可用客服会话，降级跳过");
        }
        if (StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            return MessageResult.failed(providerName(), "微信公众号未配置 appid/secret（wx.official.*）");
        }
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return MessageResult.failed(providerName(), "获取微信 access_token 失败");
            }
            JSONObject body = buildRequestBody(context);
            String resp = HttpUtil.post(SEND_URL + "?access_token=" + accessToken, body.toString(), TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            int errcode = json.getInt("errcode", -1);
            if (errcode == 0) {
                log.info("[WechatKf] 客服消息发送成功 receiver={}", context.getReceiver());
                return MessageResult.success(providerName(), "kf-" + System.currentTimeMillis());
            }
            String errmsg = json.getStr("errmsg", "未知错误");
            log.warn("[WechatKf] 客服消息发送失败 receiver={} errcode={} errmsg={}", context.getReceiver(), errcode, errmsg);
            return MessageResult.failed(providerName(), "errcode=" + errcode + " " + errmsg);
        } catch (Exception e) {
            log.warn("[WechatKf] 客服消息发送异常 receiver={}", context.getReceiver(), e);
            return MessageResult.failed(providerName(), StrUtil.maxLength(e.getMessage(), 500));
        }
    }

    private JSONObject buildRequestBody(MessageContext context) {
        JSONObject body = new JSONObject();
        body.set("touser", context.getReceiver());
        body.set("msgtype", "text");
        JSONObject text = new JSONObject();
        // 标题 + 正文拼接（客服消息无模板，纯文本）
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
     * 获取公众号 access_token（与模板消息共用 Redis 缓存）
     */
    private String getAccessToken() {
        String cached = RedisUtils.getCacheObject(REDIS_TOKEN_KEY);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String url = TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        try {
            String resp = HttpUtil.get(url, TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            String token = json.getStr("access_token");
            if (StrUtil.isNotBlank(token)) {
                RedisUtils.setCacheObject(REDIS_TOKEN_KEY, token, Duration.ofMinutes(110));
                return token;
            }
            log.error("[WechatKf] 获取 access_token 失败 resp={}", resp);
        } catch (Exception e) {
            log.error("[WechatKf] 获取 access_token 异常", e);
        }
        return null;
    }
}
