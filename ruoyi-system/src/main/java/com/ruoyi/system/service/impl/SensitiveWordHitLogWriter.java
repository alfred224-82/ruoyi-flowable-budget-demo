package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.mapper.SensitiveWordHitLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 敏感词命中日志异步写入器（M18 T18.2）
 * <p>独立 Bean 以使 {@code @Async} 生效（避免自调用失效）；PASS/FAIL 均写入。
 * <p>异步线程无登录上下文，tenantId 由调用方显式赋值；写日志异常不外抛（不阻塞主流程）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordHitLogWriter {

    private final SensitiveWordHitLogMapper hitLogMapper;

    /**
     * 异步写入命中日志。
     *
     * @param entity 命中日志实体（非空，tenantId 已显式赋值）
     */
    @Async("threadPoolTaskExecutor")
    public void writeAsync(SensitiveWordHitLog entity) {
        if (entity == null) {
            return;
        }
        try {
            hitLogMapper.insert(entity);
        } catch (Exception e) {
            // 命中日志写入失败不影响主检测流程，仅记 ERROR 结构化日志
            log.error("{\"event\":\"sensitive_word_hit_log_write_fail\",\"tenantId\":{},\"channel\":\"{}\",\"bizType\":\"{}\",\"error\":\"{}\"}",
                entity.getTenantId(), entity.getInputChannel(), entity.getBizType(), e.getMessage());
        }
    }
}
