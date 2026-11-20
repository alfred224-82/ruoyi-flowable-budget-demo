package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 游客密码登录请求体（备用方案：短信通道不可用时使用用户名+密码登录）
 *
 * @author ruoyi
 * @date 2026-10-26
 */
@Data
public class VisitorPasswordLoginBo {

    /** 手机号（作为登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** scene 参数（t/a/u/p 编码） */
    @NotBlank(message = "scene 参数不能为空")
    private String scene;
}
