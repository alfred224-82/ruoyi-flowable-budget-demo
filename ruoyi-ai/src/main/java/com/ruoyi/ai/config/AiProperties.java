package com.ruoyi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置属性
 *
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 是否启用AI功能
     */
    private boolean enabled = false;

    /**
     * OpenAI API Key
     */
    private String apiKey;

    /**
     * OpenAI API Base URL（兼容第三方API）
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 模型名称
     */
    private String modelName = "gpt-3.5-turbo";

    /**
     * 温度参数（0-2，越高越随机）
     */
    private double temperature = 0.7;

    /**
     * 最大token数
     */
    private int maxTokens = 2048;

    /**
     * 聊天记忆窗口大小（保留最近N条消息）
     */
    private int memoryWindowSize = 20;

    /**
     * 系统提示词
     */
    private String systemPrompt = "你是一个企业全面预算管理系统的AI助手。你可以帮助用户：\n"
        + "1. 分析预算数据（收入、成本、费用、资产、负债、权益各类科目的预算金额）\n"
        + "2. 生成预算报表摘要（按科目类型汇总、差异分析等）\n"
        + "3. 解答预算管理相关问题（审批流程、状态含义等）\n"
        + "4. 提供预算编制建议（基于风控规则和财务最佳实践）\n\n"
        + "请用中文回答，回答要专业、简洁、易懂。涉及金额时使用千位符格式化。";
}
