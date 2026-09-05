package com.ruoyi.system.service.wx;

/**
 * 微信小程序集成抽象（T13.1 双阶段租户识别 - code2session）
 * <p>扩展点模式：默认提供基于 Hutool HTTP 的真实实现（{@code WxMiniappServiceImpl}），
 * 需在 application.yml 配置 {@code wx.miniapp.appid/secret}；未配置时调用抛 ServiceException，
 * 此时小程序仍可用账号密码 /login 登录（向后兼容）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IWxMiniappService {

    /**
     * 小程序 code2session：以 wx.login 返回的临时凭证 jsCode 换取 openid/session_key/unionid
     *
     * @param jsCode wx.login 返回的 code
     * @return 会话结果（含 openid；配置缺失或微信返回错误时抛 ServiceException）
     */
    WxCode2SessionResult code2session(String jsCode);
}
