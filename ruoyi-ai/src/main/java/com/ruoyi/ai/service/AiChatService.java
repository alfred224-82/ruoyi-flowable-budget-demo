package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.dto.AiChatRequest;
import com.ruoyi.ai.domain.dto.AiChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话服务接口
 *
 * @author ruoyi
 */
public interface AiChatService {

    /**
     * 同步对话（一次性返回）
     *
     * @param request 对话请求
     * @return AI回复
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * 流式对话（SSE推送）
     *
     * @param request 对话请求
     * @return SSE发射器
     */
    SseEmitter chatStream(AiChatRequest request);

    /**
     * 清除会话
     *
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);
}
