package com.ruoyi.common.core.domain.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 手机号统一登录请求体（T15.8）
 * <p>PC 短信验证码登录 / 小程序 wx.login + getPhoneNumber 一键登录共用同一入口。
 * <p>★方法签名<b>无 appid 参数</b>：appid/登录渠道不参与身份权限判定，其校验在网关/拦截器单独完成；
 * 菜单端过滤唯一依据是 {@link #clientType}（前端壳明确传入），禁止靠 appid 反查 client_type。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroLoginBody {

    /**
     * 手机号（唯一身份锚点，sys_user.phonenumber 业务唯一键）
     * <p>pc/both 端必填；mini 端可空（由 {@link #phoneCode} 经 wx.getPhoneNumber 解密回填）。
     */
    private String phonenumber;

    /**
     * 短信验证码（clientType=pc/both 必填并校验；clientType=mini 由网关完成 wx 手机号校验后可空）
     */
    private String smsCode;

    /**
     * wx.login 返回的临时登录凭证 code（小程序可选：用于换取 openid 回填 user_tenant_bind，不参与身份判定）
     */
    private String xcxCode;

    /**
     * wx.getPhoneNumber 回调返回的手机号授权 code（T15.8 小程序一键登录，clientType=mini）：
     * 后端据此直连微信 getuserphonenumber 解密为明文 phonenumber；与 phonenumber 二选一（mini 端优先用此字段）。
     */
    private String phoneCode;

    /**
     * 选定门店租户ID（可选）：场景带入（扫码 share_scene 解析）或用户在租户选择页二次提交时携带；
     * 为空且手机号跨多租户时，接口返回租户候选列表引导选择，不直接下发 token。
     */
    private Long tenantId;

    /**
     * 客户端类型（pc/mini/both，前端壳传入）：菜单端过滤唯一依据，禁止由 appid 推导。
     */
    @NotBlank(message = "客户端类型不能为空")
    private String clientType;

}
