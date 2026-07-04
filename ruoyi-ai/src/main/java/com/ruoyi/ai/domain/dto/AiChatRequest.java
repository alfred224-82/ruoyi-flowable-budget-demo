package com.ruoyi.ai.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI对话请求DTO
 *
 * @author ruoyi
 */
@Data
public class AiChatRequest {

    /**
     * 会话ID（用于保持上下文，为空则创建新会话）
     */
    private String sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 关联的预算单ID（可选，用于上下文查询）
     */
    private Long sheetId;

    /**
     * 是否使用流式响应
     */
    private boolean stream = false;
}
