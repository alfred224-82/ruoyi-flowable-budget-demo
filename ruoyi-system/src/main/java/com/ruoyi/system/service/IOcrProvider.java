package com.ruoyi.system.service;

/**
 * OCR 文字提取 Provider（M18 T18.3，可插拔，默认桩实现 OcrStubProvider）
 * <p>未来接入腾讯云/阿里云 OCR 只需新增 @Primary Bean 覆盖桩实现。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IOcrProvider {

    /**
     * 提取图片中的文字。
     *
     * @param imageBytes 图片字节（非空）
     * @return 识别出的文字（无文字或未接入时返回空字符串，不返回 null）
     */
    String extractText(byte[] imageBytes);
}
