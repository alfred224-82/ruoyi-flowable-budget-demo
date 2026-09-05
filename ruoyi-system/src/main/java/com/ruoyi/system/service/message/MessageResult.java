package com.ruoyi.system.service.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息发送结果（T13.3）
 * <p>result：SUCCESS(成功)/FAILED(失败)/SKIPPED(订阅授权不足等降级跳过，不阻塞主流程)。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class MessageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结果：SUCCESS/FAILED/SKIPPED */
    private String result;

    /** 实际发送的 Provider */
    private String provider;

    /** 第三方消息ID */
    private String messageId;

    /** 失败/跳过原因 */
    private String errorMsg;

    public static MessageResult success(String provider, String messageId) {
        MessageResult r = new MessageResult();
        r.setResult("SUCCESS");
        r.setProvider(provider);
        r.setMessageId(messageId);
        return r;
    }

    public static MessageResult skipped(String provider, String reason) {
        MessageResult r = new MessageResult();
        r.setResult("SKIPPED");
        r.setProvider(provider);
        r.setErrorMsg(reason);
        return r;
    }

    public static MessageResult failed(String provider, String errorMsg) {
        MessageResult r = new MessageResult();
        r.setResult("FAILED");
        r.setProvider(provider);
        r.setErrorMsg(errorMsg);
        return r;
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }
}
