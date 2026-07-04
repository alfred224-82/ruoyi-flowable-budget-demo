package com.ruoyi.ai.domain.dto;

import lombok.Data;

/**
 * AI对话响应DTO
 *
 * @author ruoyi
 */
@Data
public class AiChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI回复内容
     */
    private String reply;

    /**
     * 是否结束
     */
    private boolean finished;

    public static AiChatResponse of(String sessionId, String reply) {
        AiChatResponse response = new AiChatResponse();
        response.setSessionId(sessionId);
        response.setReply(reply);
        response.setFinished(true);
        return response;
    }
}
