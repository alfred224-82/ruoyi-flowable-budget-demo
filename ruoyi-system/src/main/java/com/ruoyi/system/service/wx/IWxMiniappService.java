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

    /**
     * 小程序手机号解密（T15.8 一键登录）：以 button open-type=getPhoneNumber 返回的 code
     * 换取用户明文手机号（直连微信 getuserphonenumber 接口，内部自管理 access_token）。
     *
     * @param phoneCode wx.getPhoneNumber 回调返回的 e.detail.code
     * @return 明文手机号（配置缺失或微信返回错误时抛 ServiceException）
     */
    String getPhoneNumber(String phoneCode);

    /**
     * 生成微信小程序码（wxacode.getUnlimited，scene 模式，数量无限制）
     * <p>返回 base64 data URL（data:image/png;base64,...），前端可直接作为 img src 渲染。
     * <p>★不依赖 OSS 上传，适合轻量展示场景（如微信推广码页面）。
     *
     * @param scene 场景值（≤32 字符，如 t=tenantId）
     * @param page  小程序页面路径（可空，默认首页）
     * @return base64 data URL（配置缺失或微信返回错误时抛 ServiceException）
     */
    String generateWxacode(String scene, String page);
}
