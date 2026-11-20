package com.ruoyi.system.service;

/**
 * 语音 ASR 转写服务提供者接口（M20 T18 增强）
 * <p>将语音文件 URL 转换为文本，供合规校验使用。
 * <p>配置 ai.api-key 后由 {@link com.ruoyi.system.service.impl.DashScopeVoiceAsrProvider} 提供真实转写；
 *   未配置时由 {@link com.ruoyi.system.service.impl.StubVoiceAsrProvider} 降级返回空文本。
 *
 * @author ruoyi
 * @date 2026-09-30
 */
public interface IVoiceAsrProvider {

    /**
     * 将语音文件 URL 转写为文本。
     *
     * @param voiceUrl 语音文件 OSS URL
     * @param tenantId 租户ID（用于日志追踪）
     * @param activityId 活动ID（用于日志追踪）
     * @return 转写后的文本（空字符串表示转写失败或无内容）
     */
    String transcribe(String voiceUrl, Long tenantId, Long activityId);
}
