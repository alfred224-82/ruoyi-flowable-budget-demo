package com.ruoyi.system.service.wecom.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.wecom.IWecomIntegration;
import com.ruoyi.system.service.wecom.WecomUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 企微集成桩实现（T13.2，默认降级）
 * <p>OAuth：按企微标准拼接授权链接（真实可用），code 换取用户返回模拟成员（不请求企微 API）；
 * 接入真实实现（校验可信域名 / getuserinfo / 通讯录读取）时新增 Bean 并标注 @Primary 覆盖即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class WecomIntegrationStub implements IWecomIntegration {

    /** 企微网页授权入口 */
    private static final String AUTH_BASE = "https://open.weixin.qq.com/connect/oauth2/authorize";

    @Override
    public String buildAuthUrl(String corpId, String agentId, String redirectUri, String state) {
        String redirect = redirectUri == null ? "" : URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String st = StrUtil.isBlank(state) ? "STATE" : state;
        String url = AUTH_BASE + "?appid=" + StrUtil.nullToEmpty(corpId)
            + "&redirect_uri=" + redirect
            + "&response_type=code&scope=snsapi_base&state=" + st
            + "&agentid=" + StrUtil.nullToEmpty(agentId) + "#wechat_redirect";
        log.info("[WecomStub] 构造企微授权链接（桩）：{}", url);
        return url;
    }

    @Override
    public WecomUserInfo getUserInfo(String corpId, String code) {
        WecomUserInfo info = new WecomUserInfo();
        info.setCorpId(corpId);
        if (StrUtil.isBlank(code)) {
            info.setMember(false);
            info.setUserId(null);
            log.warn("[WecomStub] 授权 code 为空，返回游客身份（桩）");
            return info;
        }
        // 桩：以 code 派生模拟 userid，不请求企微 API
        info.setMember(true);
        info.setUserId("stub_" + StrUtil.maxLength(code, 32));
        info.setName("企微成员(桩)");
        log.info("[WecomStub] 模拟换取企微用户（桩）corpId={} code={} → userId={}", corpId, code, info.getUserId());
        return info;
    }
}
