package com.ruoyi.system.service;

import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;

/**
 * 统一敏感词引擎（M18 T18.2，三路 text/voice_ocr/image_ocr 共用一套引擎一套词库）
 * <p>全局底库(tenant_id=0) + 租户自定义词合并检测；exact/fuzzy/regex 三路匹配；命中/放行均写命中日志。
 * <p>FEAT_SENSITIVE_WORD 开关关闭时直接放行，不查库、不写日志。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface ISensitiveWordEngine {

    /**
     * 敏感词检测主入口。
     *
     * @param text         待检测文本（非空）
     * @param tenantId     租户ID（非空，用于合并全局底库+租户词）
     * @param inputChannel 输入渠道：text/voice_ocr/image_ocr（非空）
     * @param bizType      业务类型：rating/activity/poster/voice/chat（可空）
     * @param bizId        业务主键（可空）
     * @return 检测结果（PASS/FAIL + 命中详情 + 耗时）
     */
    SensitiveWordCheckResult check(String text, Long tenantId, String inputChannel, String bizType, String bizId);

    /**
     * 刷新指定租户词库缓存（增删改后主动调用）。
     *
     * @param tenantId 租户ID（0=全局底库）
     */
    void evictWordCache(Long tenantId);
}
