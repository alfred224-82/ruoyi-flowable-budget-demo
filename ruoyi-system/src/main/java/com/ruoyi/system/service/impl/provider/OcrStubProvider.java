package com.ruoyi.system.service.impl.provider;

import com.ruoyi.system.service.IOcrProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * OCR 文字提取桩实现（M18 T18.3，默认 Provider）
 * <p>未接入真实厂商 OCR API 时占位：返回空字符串并记 WARN 日志（等价于图片无文字，OCR 敏感词路自然放行）。
 * <p>接入腾讯云/阿里云 OCR 时新增 @Primary Bean 即可覆盖（见 ComplianceProviderConfig）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
public class OcrStubProvider implements IOcrProvider {

    @Override
    public String extractText(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        log.warn("{\"event\":\"ocr_stub\",\"msg\":\"未接入 OCR API，桩实现返回空文本\"}");
        return "";
    }
}
