package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.dto.AiChatRequest;
import com.ruoyi.ai.domain.dto.AiChatResponse;
import com.ruoyi.ai.service.AiChatService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

/**
 * AI对话式报表生成控制器
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class AiChatController extends BaseController {

    private final AiChatService aiChatService;

    /**
     * 同步对话（一次性返回完整回复）
     */
    @PostMapping
    public R<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.chat(request);
        return R.ok(response);
    }

    /**
     * 流式对话（SSE推送，逐字返回）
     */
    @PostMapping("/stream")
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chatStream(request);
    }

    /**
     * 清除会话记忆
     */
    @DeleteMapping("/session/{sessionId}")
    public R<Void> clearSession(@PathVariable String sessionId) {
        aiChatService.clearSession(sessionId);
        return R.ok();
    }

    /**
     * 检查AI服务是否可用
     */
    @GetMapping("/status")
    public R<Boolean> status() {
        return R.ok(true);
    }
}
