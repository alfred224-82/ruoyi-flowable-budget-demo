package com.ruoyi.system.config;

import com.ruoyi.system.service.IOcrProvider;
import com.ruoyi.system.service.IVisualAuditProvider;
import com.ruoyi.system.service.impl.provider.OcrStubProvider;
import com.ruoyi.system.service.impl.provider.VisualAuditStubProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片合规 Provider 装配（M18 T18.3，可插拔扩展点）
 * <p>默认注册视觉审核 / OCR 桩实现；接入腾讯云/阿里云真实 API 时，
 *   只需提供对应接口的 @Primary 或普通 Bean，桩实现即被 @ConditionalOnMissingBean 让位。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Configuration
public class ComplianceProviderConfig {

    @Bean
    @ConditionalOnMissingBean(IVisualAuditProvider.class)
    public IVisualAuditProvider visualAuditProvider() {
        return new VisualAuditStubProvider();
    }

    @Bean
    @ConditionalOnMissingBean(IOcrProvider.class)
    public IOcrProvider ocrProvider() {
        return new OcrStubProvider();
    }
}
