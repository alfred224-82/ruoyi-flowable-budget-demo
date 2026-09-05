package com.ruoyi.system.service.wecom;

/**
 * 企微集成抽象（T13.2 自建应用 OAuth + 通讯录同步入口）
 * <p>扩展点模式：默认提供桩实现（模拟 OAuth 换取用户、空通讯录同步），
 * 接入真实企微（构造网页授权链接 / getuserinfo / 通讯录读取）时新增实现并标注 @Primary 覆盖即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IWecomIntegration {

    /**
     * 构造企微自建应用网页授权链接（引导用户跳转授权）
     *
     * @param corpId     企业ID
     * @param agentId    应用ID
     * @param redirectUri 回调地址（须企微可信域名）
     * @param state      透传状态（防CSRF，可携带租户/门店标识）
     * @return 授权链接
     */
    String buildAuthUrl(String corpId, String agentId, String redirectUri, String state);

    /**
     * 以授权 code 换取企微用户身份
     *
     * @param corpId 企业ID
     * @param code   网页授权 code
     * @return 用户信息（桩实现返回模拟成员）
     */
    WecomUserInfo getUserInfo(String corpId, String code);
}
