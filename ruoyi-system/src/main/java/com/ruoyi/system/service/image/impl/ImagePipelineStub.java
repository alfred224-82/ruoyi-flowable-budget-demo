package com.ruoyi.system.service.image.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.domain.ImageTask;
import com.ruoyi.system.mapper.ImageTaskMapper;
import com.ruoyi.system.service.image.IImagePipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片处理管线桩实现（T12.3 / T12.7，默认降级）
 * <p>未接入真实渲染服务前的可扩展桩：登记 biz_image_task 后同步生成占位成品 URL 并置 SUCCESS，
 * 打通"上传即返回、成品回写"链路；接入真实图片处理时新增实现并标注 @Primary 覆盖本桩即可。
 * <p>真实实现应改为异步：submit 仅登记 PENDING 任务并返回，由后台任务/回调生成成品后回写 result_url，失败重试与告警。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component("imagePipelineStub")
@RequiredArgsConstructor
public class ImagePipelineStub implements IImagePipeline {

    private final ImageTaskMapper imageTaskMapper;

    @Override
    public Long submit(String taskType, Long themeId, String sourceUrl, String spec) {
        ImageTask task = new ImageTask();
        task.setThemeId(themeId);
        task.setTaskType(taskType);
        task.setSourceUrl(sourceUrl);
        task.setSpec(spec);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        imageTaskMapper.insert(task);

        // 桩：同步模拟生成成品（真实实现应异步处理并回调回写）
        try {
            task.setStatus("PROCESSING");
            imageTaskMapper.updateById(task);
            String resultUrl = render(sourceUrl, spec);
            task.setStatus("SUCCESS");
            task.setResultUrl(resultUrl);
            imageTaskMapper.updateById(task);
            log.info("[ImagePipelineStub] 衍生图任务 {} 生成成功（桩）：{} → {}", task.getId(), sourceUrl, resultUrl);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMsg(StrUtil.maxLength(e.getMessage(), 1000));
            imageTaskMapper.updateById(task);
            log.warn("[ImagePipelineStub] 衍生图任务 {} 生成失败（桩）", task.getId(), e);
        }
        return task.getId();
    }

    /**
     * 桩渲染：以查询参数模拟成品 URL（真实实现应输出实际处理后的 OSS 对象 URL）
     */
    private String render(String sourceUrl, String spec) {
        if (StrUtil.isBlank(sourceUrl)) {
            return null;
        }
        String sep = sourceUrl.contains("?") ? "&" : "?";
        return sourceUrl + sep + "x-render-spec=" + (spec == null ? "" : spec) + "&rendered=stub";
    }
}
