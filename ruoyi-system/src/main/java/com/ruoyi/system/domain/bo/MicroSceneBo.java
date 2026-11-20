package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 扫码 scene 解析请求体（T29.3 员工/顾客扫码带租户直接登录）
 * <p>{@code POST /micro/login/resolveScene} 入参：二维码 scene 字符串
 * （编码 {@code t={tenantId}&s={storeId}&r={role}}），服务端解析并校验合法性后返回门店上下文。
 * <p>匿名端点（登录前），仅做只读校验，绝不写 TenantContext / 签发会话。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class MicroSceneBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 二维码 scene 字符串（前端 decodeURIComponent 后传入）
     */
    @NotBlank(message = "scene 不能为空")
    private String scene;
}
