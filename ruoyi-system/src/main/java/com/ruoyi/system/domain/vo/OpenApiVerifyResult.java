package com.ruoyi.system.domain.vo;

import com.ruoyi.system.domain.OpenApiCredential;
import lombok.Data;

import java.io.Serializable;

/**
 * 开放平台外部调用鉴权结果（T7.8 / FEAT_OPEN_API）
 * <p>承载是否通过、HTTP 语义响应码（200/401/403/429）、失败原因，
 * 以及通过时命中的凭证（供外部网关按凭证租户做业务数据隔离）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiVerifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过鉴权 */
    private boolean pass;

    /** HTTP 语义响应码(200通过/401签名或AppKey或时间戳/403停用或过期或IP/429超配额) */
    private int code;

    /** 结果消息（失败原因） */
    private String message;

    /** 命中的凭证（通过时非空） */
    private transient OpenApiCredential credential;

    public static OpenApiVerifyResult ok(OpenApiCredential credential) {
        OpenApiVerifyResult r = new OpenApiVerifyResult();
        r.setPass(true);
        r.setCode(200);
        r.setMessage("OK");
        r.setCredential(credential);
        return r;
    }

    public static OpenApiVerifyResult fail(int code, String message) {
        OpenApiVerifyResult r = new OpenApiVerifyResult();
        r.setPass(false);
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
