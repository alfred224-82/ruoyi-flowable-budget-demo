package com.ruoyi.system.service.impl;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.system.service.ILlmImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 文生图服务实现（DashScope 通义万相）
 * <p>流程：提交生图任务 → 轮询任务状态 → 下载生成的图片 → 上传 OSS → 返回 URL。
 * <p>默认使用 wanx2.0-t2i-turbo 模型（成本最低，0.04 元/张，开通百炼后赠送 500 张免费额度）。
 * <p>可通过 ai.image-model 配置项切换模型（如 wanx2.1-t2i-turbo / z-image-turbo）。
 * <p>API Key 复用 ai.api-key 配置项（与 AI 报表共用）。
 *
 * @author ruoyi
 */
@Slf4j
@Service
public class LlmImageServiceImpl implements ILlmImageService {

    /** DashScope 文生图提交任务端点 */
    private static final String SUBMIT_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
    /** DashScope 任务状态查询端点模板 */
    private static final String TASK_URL_TEMPLATE = "https://dashscope.aliyuncs.com/api/v1/tasks/{}";
    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 3000L;
    /** 最大轮询次数（约 120 秒超时） */
    private static final int MAX_POLL_COUNT = 40;
    /** 生成图片尺寸 */
    private static final String IMAGE_SIZE = "1024*1024";

    @Value("${ai.api-key:}")
    private String apiKey;

    /**
     * 文生图模型名称，可通过 ai.image-model 配置覆盖。
     * 可选值：wanx2.0-t2i-turbo（最便宜）、wanx2.1-t2i-turbo（效果更好）、z-image-turbo 等。
     */
    @Value("${ai.image-model:wanx2.0-t2i-turbo}")
    private String imageModel;

    @Override
    public String generateImage(String description) {
        if (StrUtil.isBlank(description)) {
            throw new ServiceException("图片描述不能为空");
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new ServiceException("AI API Key 未配置，请检查 ai.api-key 配置项");
        }

        log.info("{\"event\":\"llm_image_start\",\"component\":\"llm-image\",\"model\":\"{}\",\"description\":\"{}\"}", imageModel, description);
        long startTime = System.currentTimeMillis();

        // 1. 提交生图任务
        String taskId = submitTask(description);
        log.info("{\"event\":\"llm_image_submitted\",\"component\":\"llm-image\",\"taskId\":\"{}\"}", taskId);

        // 2. 轮询任务状态
        String imageUrl = pollTaskResult(taskId);
        if (StrUtil.isBlank(imageUrl)) {
            throw new ServiceException("AI 图片生成超时（已等待约 " + (MAX_POLL_COUNT * POLL_INTERVAL_MS / 1000) + " 秒），请稍后重试");
        }

        // 3. 下载图片并上传到 OSS
        String ossUrl = downloadAndUpload(imageUrl);

        long cost = System.currentTimeMillis() - startTime;
        log.info("{\"event\":\"llm_image_done\",\"component\":\"llm-image\",\"taskId\":\"{}\",\"cost\":{},\"ossUrl\":\"{}\"}",
            taskId, cost, ossUrl);

        return ossUrl;
    }

    /**
     * 提交文生图任务，返回 task_id
     */
    private String submitTask(String description) {
        Map<String, Object> body = Map.of(
            "model", imageModel,
            "input", Map.of("prompt", description),
            "parameters", Map.of("size", IMAGE_SIZE, "n", 1)
        );

        log.info("{\"event\":\"llm_image_submit\",\"component\":\"llm-image\",\"model\":\"{}\"}", imageModel);
        try (HttpResponse response = HttpRequest.post(SUBMIT_URL)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("X-DashScope-Async", "enable")
            .body(JsonUtils.toJsonString(body))
            .timeout(30000)
            .execute()) {

            String responseBody = response.body();
            if (!response.isOk()) {
                log.error("{\"event\":\"llm_image_submit_fail\",\"component\":\"llm-image\",\"model\":\"{}\",\"httpCode\":{},\"body\":\"{}\"}",
                    imageModel, response.getStatus(), responseBody);
                // 提取 API 返回的错误信息，给出更明确的提示
                String errorMsg = extractApiError(responseBody);
                if (StrUtil.isNotBlank(errorMsg)) {
                    throw new ServiceException("AI 图片服务调用失败：" + errorMsg);
                }
                throw new ServiceException("AI 图片服务调用失败（HTTP " + response.getStatus() + "）");
            }

            Dict result = JsonUtils.parseMap(responseBody);
            if (result == null) {
                log.error("{\"event\":\"llm_image_submit_parse_fail\",\"component\":\"llm-image\",\"body\":\"{}\"}", responseBody);
                throw new ServiceException("AI 图片服务响应解析失败");
            }
            // 检查是否有错误码
            String code = result.getStr("code");
            if (StrUtil.isNotBlank(code)) {
                String msg = StrUtil.nullToDefault(result.getStr("message"), "未知错误");
                log.error("{\"event\":\"llm_image_submit_api_error\",\"component\":\"llm-image\",\"code\":\"{}\",\"msg\":\"{}\"}", code, msg);
                throw new ServiceException("AI 图片服务错误：" + msg);
            }
            Dict output = toDict(result.get("output"));
            if (output == null || StrUtil.isBlank(output.getStr("task_id"))) {
                log.error("{\"event\":\"llm_image_no_task_id\",\"component\":\"llm-image\",\"response\":\"{}\"}", responseBody);
                throw new ServiceException("AI 图片服务未返回任务ID");
            }
            return output.getStr("task_id");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{\"event\":\"llm_image_submit_error\",\"component\":\"llm-image\",\"error\":\"{}\"}", e.getMessage(), e);
            throw new ServiceException("AI 图片服务调用异常：" + e.getMessage());
        }
    }

    /**
     * 从 API 错误响应中提取错误信息
     */
    private String extractApiError(String responseBody) {
        try {
            Dict result = JsonUtils.parseMap(responseBody);
            if (result != null) {
                String msg = result.getStr("message");
                if (StrUtil.isNotBlank(msg)) {
                    return msg;
                }
            }
        } catch (Exception ignored) {
            // 解析失败不影响主流程
        }
        return null;
    }

    /**
     * 轮询任务状态，返回生成成功的图片 URL
     */
    private String pollTaskResult(String taskId) {
        String taskUrl = StrUtil.format(TASK_URL_TEMPLATE, taskId);

        for (int i = 0; i < MAX_POLL_COUNT; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("AI 图片生成被中断");
            }

            try (HttpResponse response = HttpRequest.get(taskUrl)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(15000)
                .execute()) {

                String responseBody = response.body();
                if (!response.isOk()) {
                    log.warn("{\"event\":\"llm_image_poll_fail\",\"component\":\"llm-image\",\"taskId\":\"{}\",\"httpCode\":{}}",
                        taskId, response.getStatus());
                    continue;
                }

                Dict result = JsonUtils.parseMap(responseBody);
                if (result == null) continue;
                Dict output = toDict(result.get("output"));
                if (output == null) continue;

                String status = output.getStr("task_status");
                if ("SUCCEEDED".equals(status)) {
                    // 提取图片 URL
                    List<Dict> results = output.getBean("results");
                    if (results != null && !results.isEmpty()) {
                        return results.get(0).getStr("url");
                    }
                    // wanx2.1 可能用 output.results[].url 或 output.result_url
                    return output.getStr("result_url");
                } else if ("FAILED".equals(status)) {
                    String msg = StrUtil.nullToDefault(output.getStr("message"), "未知错误");
                    log.error("{\"event\":\"llm_image_task_fail\",\"component\":\"llm-image\",\"taskId\":\"{}\",\"msg\":\"{}\"}",
                        taskId, msg);
                    throw new ServiceException("AI 图片生成失败：" + msg);
                }
                // PENDING / RUNNING → 继续轮询
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("{\"event\":\"llm_image_poll_error\",\"component\":\"llm-image\",\"taskId\":\"{}\",\"error\":\"{}\"}",
                    taskId, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 下载远程图片并上传到 OSS，返回 OSS URL
     */
    private String downloadAndUpload(String imageUrl) {
        try {
            byte[] imageBytes = cn.hutool.http.HttpUtil.downloadBytes(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new ServiceException("下载 AI 生成的图片失败：响应为空");
            }

            OssClient ossClient = OssFactory.instance();
            UploadResult uploadResult = ossClient.uploadSuffix(imageBytes, ".png", "image/png");
            return uploadResult.getUrl();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{\"event\":\"llm_image_upload_fail\",\"component\":\"llm-image\",\"imageUrl\":\"{}\",\"error\":\"{}\"}",
                imageUrl, e.getMessage(), e);
            throw new ServiceException("图片上传 OSS 失败：" + e.getMessage());
        }
    }

    /**
     * 将 Object 转为 Dict（兼容 Hutool Dict 嵌套解析）
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
