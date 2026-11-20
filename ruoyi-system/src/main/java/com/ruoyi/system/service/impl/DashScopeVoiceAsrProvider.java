package com.ruoyi.system.service.impl;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.service.IVoiceAsrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 语音 ASR 转写 DashScope 实现（录音文件识别 paraformer-v2）
 * <p>流程：提交异步转写任务 → 轮询任务状态 → 拉取转写结果文本（拼接全通道 text）。
 * <p>API Key 复用 ai.api-key 配置项（与 AI 报表/文生图共用）；模型可通过 ai.asr-model 配置切换。
 * <p>★fail-soft：任一环节失败返回空字符串（调用方记 NO_TRANSCRIPTION 并按合规通过处理），
 *   绝不伪造转写内容（历史桩实现返回固定测试文案导致内容摘要失真的缺陷不再复现）。
 * <p>★仅 ai.api-key 已配置时激活；未配置时由 {@link StubVoiceAsrProvider} 兜底降级。
 *
 * @author ruoyi
 * @date 2026-10-14
 */
@Slf4j
@Service
@ConditionalOnExpression("!'${ai.api-key:}'.isEmpty()")
public class DashScopeVoiceAsrProvider implements IVoiceAsrProvider {

    /** DashScope 录音文件识别提交任务端点 */
    private static final String SUBMIT_URL = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription";
    /** DashScope 任务状态查询端点模板 */
    private static final String TASK_URL_TEMPLATE = "https://dashscope.aliyuncs.com/api/v1/tasks/{}";
    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 2000L;
    /** 最大轮询次数（约 60 秒超时） */
    private static final int MAX_POLL_COUNT = 30;

    @Value("${ai.api-key:}")
    private String apiKey;

    /** 转写模型名称，可通过 ai.asr-model 配置覆盖（paraformer-v2 / paraformer-8k-v2 等） */
    @Value("${ai.asr-model:paraformer-v2}")
    private String asrModel;

    @Override
    public String transcribe(String voiceUrl, Long tenantId, Long activityId) {
        if (StrUtil.isBlank(voiceUrl)) {
            return "";
        }
        long startTime = System.currentTimeMillis();
        log.info("{\"event\":\"voice_asr_start\",\"component\":\"voice-asr\",\"model\":\"{}\",\"tenantId\":{},\"activityId\":{}}",
            asrModel, tenantId, activityId);
        try {
            // ① 提交异步转写任务
            String taskId = submitTask(voiceUrl);
            if (StrUtil.isBlank(taskId)) {
                return "";
            }
            // ② 轮询任务状态，取转写结果 URL
            String transcriptionUrl = pollTranscriptionUrl(taskId);
            if (StrUtil.isBlank(transcriptionUrl)) {
                return "";
            }
            // ③ 拉取转写结果文本
            String text = fetchTranscriptionText(transcriptionUrl);
            log.info("{\"event\":\"voice_asr_done\",\"component\":\"voice-asr\",\"taskId\":\"{}\",\"tenantId\":{},\"cost\":{},\"textLength\":{}}",
                taskId, tenantId, System.currentTimeMillis() - startTime, text.length());
            return text;
        } catch (Exception e) {
            // ★fail-soft：ASR 异常不阻断发布主流程，调用方记 NO_TRANSCRIPTION
            log.warn("{\"event\":\"voice_asr_error\",\"component\":\"voice-asr\",\"tenantId\":{},\"activityId\":{},\"error\":\"{}\"}",
                tenantId, activityId, e.getMessage());
            return "";
        }
    }

    /**
     * 提交录音文件识别异步任务，返回 task_id（失败返回 null）
     */
    private String submitTask(String voiceUrl) {
        Map<String, Object> body = Map.of(
            "model", asrModel,
            "input", Map.of("file_urls", List.of(voiceUrl)),
            "parameters", Map.of("language_hints", List.of("zh"))
        );
        try (HttpResponse response = HttpRequest.post(SUBMIT_URL)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("X-DashScope-Async", "enable")
            .body(JsonUtils.toJsonString(body))
            .timeout(30000)
            .execute()) {

            String responseBody = response.body();
            if (!response.isOk()) {
                log.warn("{\"event\":\"voice_asr_submit_fail\",\"component\":\"voice-asr\",\"httpCode\":{},\"body\":\"{}\"}",
                    response.getStatus(), responseBody);
                return null;
            }
            Dict result = JsonUtils.parseMap(responseBody);
            if (result == null) {
                log.warn("{\"event\":\"voice_asr_submit_parse_fail\",\"component\":\"voice-asr\",\"body\":\"{}\"}", responseBody);
                return null;
            }
            if (StrUtil.isNotBlank(result.getStr("code"))) {
                log.warn("{\"event\":\"voice_asr_submit_api_error\",\"component\":\"voice-asr\",\"code\":\"{}\",\"msg\":\"{}\"}",
                    result.getStr("code"), result.getStr("message"));
                return null;
            }
            Dict output = toDict(result.get("output"));
            if (output == null || StrUtil.isBlank(output.getStr("task_id"))) {
                log.warn("{\"event\":\"voice_asr_no_task_id\",\"component\":\"voice-asr\",\"response\":\"{}\"}", responseBody);
                return null;
            }
            return output.getStr("task_id");
        }
    }

    /**
     * 轮询任务状态，成功后返回转写结果 URL（超时/失败返回 null）
     */
    private String pollTranscriptionUrl(String taskId) {
        String taskUrl = StrUtil.format(TASK_URL_TEMPLATE, taskId);
        for (int i = 0; i < MAX_POLL_COUNT; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            try (HttpResponse response = HttpRequest.get(taskUrl)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(15000)
                .execute()) {

                if (!response.isOk()) {
                    continue;
                }
                Dict result = JsonUtils.parseMap(response.body());
                if (result == null) {
                    continue;
                }
                Dict output = toDict(result.get("output"));
                if (output == null) {
                    continue;
                }
                String status = output.getStr("task_status");
                if ("SUCCEEDED".equals(status)) {
                    List<Dict> results = output.getBean("results");
                    if (results == null || results.isEmpty()) {
                        log.warn("{\"event\":\"voice_asr_no_results\",\"component\":\"voice-asr\",\"taskId\":\"{}\"}", taskId);
                        return null;
                    }
                    return results.get(0).getStr("transcription_url");
                }
                if ("FAILED".equals(status)) {
                    log.warn("{\"event\":\"voice_asr_task_fail\",\"component\":\"voice-asr\",\"taskId\":\"{}\",\"msg\":\"{}\"}",
                        taskId, StrUtil.nullToDefault(output.getStr("message"), "未知错误"));
                    return null;
                }
                // PENDING / RUNNING → 继续轮询
            } catch (Exception e) {
                log.warn("{\"event\":\"voice_asr_poll_error\",\"component\":\"voice-asr\",\"taskId\":\"{}\",\"error\":\"{}\"}",
                    taskId, e.getMessage());
            }
        }
        log.warn("{\"event\":\"voice_asr_timeout\",\"component\":\"voice-asr\",\"taskId\":\"{}\"}", taskId);
        return null;
    }

    /**
     * 拉取转写结果 JSON，拼接全部通道 text（失败返回空字符串）
     */
    private String fetchTranscriptionText(String transcriptionUrl) {
        try (HttpResponse response = HttpRequest.get(transcriptionUrl).timeout(15000).execute()) {
            if (!response.isOk()) {
                log.warn("{\"event\":\"voice_asr_fetch_fail\",\"component\":\"voice-asr\",\"httpCode\":{}}", response.getStatus());
                return "";
            }
            Dict result = JsonUtils.parseMap(response.body());
            if (result == null) {
                return "";
            }
            List<Dict> transcripts = result.getBean("transcripts");
            if (transcripts == null || transcripts.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (Dict transcript : transcripts) {
                String text = transcript.getStr("text");
                if (StrUtil.isNotBlank(text)) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(text);
                }
            }
            return sb.toString();
        }
    }

    /**
     * 将 Object 转为 Dict（兼容 Hutool Dict 嵌套解析，同 LlmImageServiceImpl）
     */
    private Dict toDict(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Dict) return (Dict) obj;
        try {
            return JsonUtils.parseMap(JsonUtils.toJsonString(obj));
        } catch (Exception e) {
            return null;
        }
    }
}
