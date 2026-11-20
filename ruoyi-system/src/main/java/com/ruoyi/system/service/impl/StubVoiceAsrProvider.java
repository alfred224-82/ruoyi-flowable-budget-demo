package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.IVoiceAsrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * 语音 ASR 转写降级实现（ai.api-key 未配置时激活）
 * <p>★未对接真实 ASR 时返回空转写结果（调用方记 NO_TRANSCRIPTION 并按合规通过处理），
 *   绝不伪造转写内容：历史版本返回固定桌游店测试文案，导致任意租户语音合规记录
 *   内容摘要失真（旅行社租户摘要显示桌游文案），已废弃该行为。
 * <p>配置 ai.api-key 后由 {@link DashScopeVoiceAsrProvider} 提供真实转写。
 *
 * @author ruoyi
 * @date 2026-09-30
 */
@Slf4j
@Service
@ConditionalOnExpression("'${ai.api-key:}'.isEmpty()")
public class StubVoiceAsrProvider implements IVoiceAsrProvider {

    /**
     * 降级实现：返回空字符串，表示转写不可用。
     *
     * @param voiceUrl   语音文件 OSS URL
     * @param tenantId   租户ID
     * @param activityId 活动ID
     * @return 空字符串（调用方记 NO_TRANSCRIPTION）
     */
    @Override
    public String transcribe(String voiceUrl, Long tenantId, Long activityId) {
        log.warn("[StubVoiceAsrProvider] ai.api-key 未配置，ASR 转写不可用：tenantId={}, activityId={}, voiceUrl={}",
            tenantId, activityId, StrUtil.blankToDefault(voiceUrl, ""));
        return "";
    }
}
