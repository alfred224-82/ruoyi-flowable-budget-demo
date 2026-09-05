package com.ruoyi.common.core.domain.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 小程序登录对象（T13.1 双阶段租户识别）
 * <p>必须显式携带选定门店 tenantId，禁止按 openid 自动猜测租户。
 *
 * @author ruoyi
 */
@Data
public class XcxLoginBody {

    /**
     * wx.login 返回的临时登录凭证 code
     */
    @NotBlank(message = "{xcx.code.not.blank}")
    private String xcxCode;

    /**
     * 选定门店租户ID（必填）：双阶段租户识别要求显式选定门店
     */
    @NotNull(message = "请先选择门店后再使用微信一键登录")
    private Long tenantId;
}
