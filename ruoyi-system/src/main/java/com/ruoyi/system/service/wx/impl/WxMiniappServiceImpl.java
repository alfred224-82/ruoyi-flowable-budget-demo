package com.ruoyi.system.service.wx.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.wx.IWxMiniappService;
import com.ruoyi.system.service.wx.WxCode2SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    /** 请求超时（毫秒） */
    private static final int TIMEOUT_MS = 10000;

    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

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
}
