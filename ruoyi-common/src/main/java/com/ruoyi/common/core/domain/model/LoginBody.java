package com.ruoyi.common.core.domain.model;

import com.ruoyi.common.constant.UserConstants;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录对象
 *
 * @author Lion Li
 */

@Data
public class LoginBody {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH, message = "{user.password.length.valid}")
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 选定门店租户ID（可选）：小程序引导态显式选定门店后传入，登录时校验账号归属，防跨门店串号（T13.1 双阶段租户识别）。
     * PC 端不传则维持原行为（按全局唯一用户名登录，不做门店约束）。
     */
    private Long tenantId;

    /**
     * 微信 openid（可选）：小程序登录成功后用于回填 user_tenant_bind.userId（T13.1 第二阶段）。
     * 依赖 code2session 获取，PC 端及桩阶段不传则跳过回填。
     */
    private String openid;

}
