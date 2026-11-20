package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 游客扫码登录请求体（M31 T31.4/T31.5）
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
public class VisitorLoginBo {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    /** 图片验证码（可选，防刷增强） */
    private String captchaCode;

    /** 图片验证码 UUID */
    private String captchaUuid;

    /** scene 参数（t/a/u/p 编码） */
    @NotBlank(message = "scene 参数不能为空")
    private String scene;
}
