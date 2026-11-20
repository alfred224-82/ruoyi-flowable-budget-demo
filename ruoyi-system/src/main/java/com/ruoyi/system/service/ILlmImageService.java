package com.ruoyi.system.service;

/**
 * LLM 文生图服务接口
 * <p>对接 DashScope 通义万相 API，根据文字描述生成图片并上传至 OSS。
 *
 * @author ruoyi
 */
public interface ILlmImageService {

    /**
     * 根据文字描述生成图片
     *
     * @param description 图片描述（中文/英文均可）
     * @return 生成的图片 OSS URL
     */
    String generateImage(String description);
}
