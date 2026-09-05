package com.ruoyi.system.service.security;

import java.io.Serializable;

/**
 * 内容安全审核结果（T12.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public class ContentCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过 */
    private boolean pass;

    /** 结果码：PASS/REJECT/ERROR */
    private String result;

    /** 命中标签（涉政/涉黄/广告等） */
    private String label;

    /** 审核提供方：wechat_imgSecCheck/wechat_msgSecCheck/stub */
    private String provider;

    /** 第三方原始响应（脱敏） */
    private String rawResponse;

    public ContentCheckResult() {
    }

    public ContentCheckResult(boolean pass, String result, String label, String provider, String rawResponse) {
        this.pass = pass;
        this.result = result;
        this.label = label;
        this.provider = provider;
        this.rawResponse = rawResponse;
    }

    public static ContentCheckResult pass(String provider) {
        return new ContentCheckResult(true, "PASS", null, provider, null);
    }

    public static ContentCheckResult reject(String label, String provider, String rawResponse) {
        return new ContentCheckResult(false, "REJECT", label, provider, rawResponse);
    }

    public static ContentCheckResult error(String provider, String rawResponse) {
        return new ContentCheckResult(false, "ERROR", null, provider, rawResponse);
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
