package com.ruoyi.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI报表配置属性
 *
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.report")
public class ReportProperties {

    /**
     * 是否启用AI报表功能（默认跟随 ai.enabled）
     */
    private boolean enabled = true;

    /**
     * 报表生成最大等待时间（秒）
     */
    private int generateTimeout = 120;

    /**
     * 报表文件存储路径
     */
    private String storagePath = "report-output";

    /**
     * 水印开关
     */
    private boolean watermarkEnabled = true;
}
