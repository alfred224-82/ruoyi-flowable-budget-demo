package com.ruoyi.report.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * AI报表 OkHttp 客户端配置
 *
 * @author ruoyi
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class ReportOkHttpConfig {

    @Bean
    public OkHttpClient reportOkHttpClient() {
        log.info("初始化AI报表 OkHttp 客户端");
        return new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
}
