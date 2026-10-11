package com.ruoyi.system.service.wx.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.wx.IWxMiniappService;
import com.ruoyi.system.service.wx.WxCode2SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 微信小程序集成实现（T13.1 code2session）
 * <p>基于 Hutool HTTP 直连微信 jscode2session 接口（无需引入微信 SDK）；
 * appid/secret 取自 {@code wx.miniapp.*} 配置，未配置时抛 ServiceException（一键登录降级，账号密码登录不受影响）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class WxMiniappServiceImpl implements IWxMiniappService {

    /** 微信 jscode2session 接口 */
    private static final String JSCODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 微信获取接口调用凭据 access_token（T15.8 getPhoneNumber 前置） */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    /** 微信获取手机号接口（T15.8 一键登录） */
    private static final String GET_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /** 微信小程序码生成接口（scene 模式，数量无限制） */
    private static final String WXACODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    /** 请求超时（毫秒） */
    private static final int TIMEOUT_MS = 10000;

    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

    /** access_token 简单内存缓存（生产建议用 Redis 分布式缓存；V1 单实例够用） */
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAt;

    @Override
    public WxCode2SessionResult code2session(String jsCode) {
        if (StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            throw new ServiceException("微信小程序未配置 appid/secret（wx.miniapp.*），无法使用一键登录");
        }
        if (StrUtil.isBlank(jsCode)) {
            throw new ServiceException("微信登录凭证 code 不能为空");
        }
        String url = JSCODE2SESSION_URL + "?appid=" + appid + "&secret=" + secret
            + "&js_code=" + jsCode + "&grant_type=authorization_code";
        String body;
        try {
            body = HttpUtil.get(url, TIMEOUT_MS);
        } catch (Exception e) {
            log.error("[WxMiniapp] code2session 请求异常", e);
            throw new ServiceException("微信登录服务暂不可用，请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(body);
        WxCode2SessionResult result = new WxCode2SessionResult();
        result.setOpenid(json.getStr("openid"));
        result.setSessionKey(json.getStr("session_key"));
        result.setUnionid(json.getStr("unionid"));
        result.setErrcode(json.getInt("errcode"));
        result.setErrmsg(json.getStr("errmsg"));
        if (!result.isSuccess()) {
            // session_key 属敏感凭据，仅服务端留存，绝不下发前端或写日志
            log.error("[WxMiniapp] code2session 失败 errcode={} errmsg={}", result.getErrcode(), result.getErrmsg());
            throw new ServiceException("微信登录失败：" + StrUtil.nullToDefault(result.getErrmsg(), "未知错误"));
        }
        log.info("[WxMiniapp] code2session 成功 openid={}", result.getOpenid());
        return result;
    }

    @Override
    public String getPhoneNumber(String phoneCode) {
        if (StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            throw new ServiceException("微信小程序未配置 appid/secret（wx.miniapp.*），无法使用手机号一键登录");
        }
        if (StrUtil.isBlank(phoneCode)) {
            throw new ServiceException("微信手机号授权 code 不能为空");
        }
        String accessToken = getAccessToken();
        String url = GET_PHONE_URL + "?access_token=" + accessToken;
        String body;
        try {
            body = HttpRequest.post(url)
                .contentType("application/json")
                .body(JSONUtil.createObj().set("code", phoneCode).toString())
                .timeout(TIMEOUT_MS)
                .execute().body();
        } catch (Exception e) {
            log.error("[WxMiniapp] getPhoneNumber 请求异常", e);
            throw new ServiceException("微信手机号解密服务暂不可用，请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(body);
        Integer errcode = json.getInt("errcode");
        if (errcode == null || errcode != 0) {
            log.error("[WxMiniapp] getPhoneNumber 失败 errcode={} errmsg={}", errcode, json.getStr("errmsg"));
            throw new ServiceException("微信手机号解密失败：" + StrUtil.nullToDefault(json.getStr("errmsg"), "未知错误"));
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        String phoneNumber = phoneInfo != null ? phoneInfo.getStr("purePhoneNumber") : null;
        if (StrUtil.isBlank(phoneNumber) && phoneInfo != null) {
            phoneNumber = phoneInfo.getStr("phoneNumber");
        }
        if (StrUtil.isBlank(phoneNumber)) {
            throw new ServiceException("微信未返回有效手机号");
        }
        // 手机号为敏感信息，日志脱敏
        log.info("[WxMiniapp] getPhoneNumber 成功 phone={}", phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        return phoneNumber;
    }

    /**
     * 获取接口调用凭据 access_token（简单内存缓存，提前 5 分钟过期避免边界失效）。
     */
    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (StrUtil.isNotBlank(cachedAccessToken) && now < accessTokenExpireAt) {
            return cachedAccessToken;
        }
        String url = ACCESS_TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        String body;
        try {
            body = HttpUtil.get(url, TIMEOUT_MS);
        } catch (Exception e) {
            log.error("[WxMiniapp] access_token 请求异常", e);
            throw new ServiceException("微信 access_token 获取失败，请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(body);
        String token = json.getStr("access_token");
        if (StrUtil.isBlank(token)) {
            log.error("[WxMiniapp] access_token 获取失败 errcode={} errmsg={}", json.getInt("errcode"), json.getStr("errmsg"));
            throw new ServiceException("微信 access_token 获取失败：" + StrUtil.nullToDefault(json.getStr("errmsg"), "未知错误"));
        }
        Integer expiresIn = json.getInt("expires_in");
        cachedAccessToken = token;
        accessTokenExpireAt = now + ((expiresIn != null ? expiresIn : 7200) - 300) * 1000L;
        return cachedAccessToken;
    }

    @Override
    public String generateWxacode(String scene, String page) {
        // ★fail-fast：配置检查
        if (StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            throw new ServiceException("微信小程序未配置 appid/secret（wx.miniapp.*），无法生成小程序码");
        }
        if (StrUtil.isBlank(scene)) {
            throw new ServiceException("scene 参数不能为空");
        }
        // scene ≤ 32 字符（微信限制）
        if (scene.length() > 32) {
            scene = scene.substring(0, 32);
        }
        String accessToken = getAccessToken();
        String url = WXACODE_URL + "?access_token=" + accessToken;
        // 构造请求体
        JSONObject body = JSONUtil.createObj()
            .set("scene", scene)
            .set("width", 430)
            .set("auto_color", true)
            .set("check_path", false);
        if (StrUtil.isNotBlank(page)) {
            body.set("page", page);
        }
        byte[] qrCodeBytes;
        try {
            HttpResponse response = HttpRequest.post(url)
                .contentType("application/json")
                .body(body.toString())
                .timeout(TIMEOUT_MS)
                .execute();
            // ★wxacode.getUnlimited 成功时直接返回 PNG 图片字节流（非 JSON）；
            //  失败时返回 JSON（含 errcode/errmsg），需区分处理
            String contentType = response.header("Content-Type");
            if (contentType != null && contentType.contains("application/json")) {
                JSONObject errJson = JSONUtil.parseObj(response.body());
                Integer errcode = errJson.getInt("errcode");
                log.error("[WxMiniapp] generateWxacode 失败 errcode={} errmsg={}", errcode, errJson.getStr("errmsg"));
                throw new ServiceException("微信小程序码生成失败：" + StrUtil.nullToDefault(errJson.getStr("errmsg"), "未知错误"));
            }
            qrCodeBytes = response.bodyBytes();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WxMiniapp] generateWxacode 请求异常", e);
            throw new ServiceException("微信小程序码生成服务暂不可用，请稍后重试");
        }
        if (qrCodeBytes == null || qrCodeBytes.length == 0) {
            throw new ServiceException("微信小程序码生成返回空数据");
        }
        // 转为 base64 data URL，前端可直接作为 img src 渲染
        String base64 = Base64.getEncoder().encodeToString(qrCodeBytes);
        String dataUrl = "data:image/png;base64," + base64;
        log.info("[WxMiniapp] generateWxacode 成功 scene={} size={}KB", scene, qrCodeBytes.length / 1024);
        return dataUrl;
    }
}
