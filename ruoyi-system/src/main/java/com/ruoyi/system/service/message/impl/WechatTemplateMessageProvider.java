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
import java.util.Map;

/**
 * 微信公众号模板消息 Provider（T13.3）
 * <p>基于 Hutool HTTP 直连微信模板消息接口；
 * appid/secret 取自 {@code wx.official.*} 配置，未配置时返回 FAILED（不阻塞主流程）。
 * <p>access_token 缓存至 Redis（有效期 2h，微信官方 7200s）。
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Component
public class WechatTemplateMessageProvider implements IMessageProvider {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";
    private static final String REDIS_TOKEN_KEY = "wx:official:access_token";
    private static final int TIMEOUT_MS = 10_000;

    @Value("${wx.official.appid:}")
    private String appid;

    @Value("${wx.official.secret:}")
    private String secret;

    @Override
    public String providerName() {
        return WECHAT_TEMPLATE;
    }

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null || StrUtil.isBlank(context.getReceiver())) {
            return MessageResult.skipped(providerName(), "openid 为空或用户未订阅，降级跳过");
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
                String msgId = json.getStr("msgid");
                log.info("[WechatTemplate] 模板消息发送成功 receiver={} msgId={}", context.getReceiver(), msgId);
                return MessageResult.success(providerName(), msgId);
            }
            String errmsg = json.getStr("errmsg", "未知错误");
            log.warn("[WechatTemplate] 模板消息发送失败 receiver={} errcode={} errmsg={}", context.getReceiver(), errcode, errmsg);
            return MessageResult.failed(providerName(), "errcode=" + errcode + " " + errmsg);
        } catch (Exception e) {
            log.warn("[WechatTemplate] 模板消息发送异常 receiver={}", context.getReceiver(), e);
            return MessageResult.failed(providerName(), StrUtil.maxLength(e.getMessage(), 500));
        }
    }

    private JSONObject buildRequestBody(MessageContext context) {
        JSONObject body = new JSONObject();
        body.set("touser", context.getReceiver());
        body.set("template_id", context.getTemplateId());
        // 模板数据
        JSONObject data = new JSONObject();
        if (context.getData() != null) {
            for (Map.Entry<String, Object> entry : context.getData().entrySet()) {
                JSONObject item = new JSONObject();
                item.set("value", String.valueOf(entry.getValue()));
                data.set(entry.getKey(), item);
            }
        }
        // 标题和内容作为 first/data 字段（通用模板格式）
        if (StrUtil.isNotBlank(context.getTitle())) {
            JSONObject first = new JSONObject();
            first.set("value", context.getTitle());
            data.set("first", first);
        }
        if (StrUtil.isNotBlank(context.getContent())) {
            JSONObject remark = new JSONObject();
            remark.set("value", context.getContent());
            data.set("remark", remark);
        }
        body.set("data", data);
        return body;
    }

    /**
     * 获取公众号 access_token（Redis 缓存 2h）
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
                // 缓存 110 分钟（略小于官方 7200s，留余量）
                RedisUtils.setCacheObject(REDIS_TOKEN_KEY, token, Duration.ofMinutes(110));
                return token;
            }
            log.error("[WechatTemplate] 获取 access_token 失败 resp={}", resp);
        } catch (Exception e) {
            log.error("[WechatTemplate] 获取 access_token 异常", e);
        }
        return null;
    }
}
