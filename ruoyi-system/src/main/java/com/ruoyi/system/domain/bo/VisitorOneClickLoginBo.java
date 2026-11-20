package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 游客一键登录请求体（M35 H5 一键登录取号）
 * <p>spToken 由前端 numberAuth-web-sdk.js 授权页成功后返回（运营商一次性 Token），
 * 后端调 GetPhoneWithToken 换取明文手机号后走游客临时账号签发流程。
 *
 * @author ruoyi
 * @date 2026-10-25
 */
@Data
public class VisitorOneClickLoginBo {

    /** 运营商一键登录 Token（一次性：移动2分钟/电信10分钟/联通30分钟） */
    @NotBlank(message = "spToken 不能为空")
    private String spToken;

    /** scene 参数（t/a/u/p 编码，p 段一键登录场景传 0000 占位） */
    @NotBlank(message = "scene 参数不能为空")
    private String scene;
}
