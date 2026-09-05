package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 引导态门店绑定业务对象（T13.1 双阶段租户识别 - 第一阶段）
 * <p>用户在门店选择页显式选定门店后，记录 openid→门店关联（userId 空，登录后由 /login 回填）。
 * 【严禁】据此写 TenantContext / 签发会话——绑定关系不授予任何权限，登录仍须账号密码 + 门店归属校验。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class PortalBindBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 微信 openid（依赖 code2session 获取）
     */
    @NotBlank(message = "openid 不能为空")
    private String openid;

    /**
     * 选定门店租户ID
     */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    /**
     * 是否默认门店（0否 1是；多绑定时登录页仍须显式选定）
     */
    private Integer isDefault;
}
