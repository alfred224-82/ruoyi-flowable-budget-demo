package com.ruoyi.system.service.security.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.service.IImageComplianceService;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.security.ContentCheckResult;
import com.ruoyi.system.service.security.IContentSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 内容安全审核实现（T12.4/T14.4，M18 T18.6 收口为委派实现）
 * <p>★重构说明（M18 T18.6）：删除本地关键词黑名单桩逻辑与"URL 非空即放行"图片桩，
 *   统一委派到 M18 引擎：
 * <ul>
 *   <li>文本审核 → {@link ISensitiveWordEngine#check}（全局底库+租户词，exact/fuzzy/regex 三路匹配）；</li>
 *   <li>图片审核 → {@link IImageComplianceService#checkImageByUrl}（OCR 文字路 + 视觉审核路两路并行）。</li>
 * </ul>
 * <p>保持 {@link IContentSecurity} 签名兼容，T12.4/T14.4 既有调用方零改动；
 *   {@link WechatImgSecCheckSecurity} 的文本审核仍委派本 Bean，故文本路自动收口到 M18 引擎。
 * <p>引擎/图片服务内部已按 FEAT_SENSITIVE_WORD / FEAT_IMAGE_COMPLIANCE 开关降级，开关关闭时直接放行。
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Component("contentSecurityChecker")
public class ContentSecurityBlacklistChecker implements IContentSecurity {

    private static final String PROVIDER = "sensitiveWordEngine";

    private final ISensitiveWordEngine sensitiveWordEngine;
    private final IImageComplianceService imageComplianceService;

    /**
     * @Lazy 注入避免与图片合规服务/引擎潜在循环依赖。
     */
    @Autowired
    public ContentSecurityBlacklistChecker(@Lazy ISensitiveWordEngine sensitiveWordEngine,
                                           @Lazy IImageComplianceService imageComplianceService) {
        this.sensitiveWordEngine = sensitiveWordEngine;
        this.imageComplianceService = imageComplianceService;
    }

    @Override
    public ContentCheckResult checkImage(String scene, String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return ContentCheckResult.error(PROVIDER, "imageUrl is blank");
        }
        Long tenantId = currentTenantId();
        try {
            ImageComplianceResult result = imageComplianceService.checkImageByUrl(imageUrl, tenantId, scene, null);
            if (result != null && !result.isPass()) {
                log.warn("[ContentSecurity] checkImage scene={} 命中图片违规 → REJECT reason={}", scene, result.getFailReason());
                return ContentCheckResult.reject("image:" + StrUtil.nullToEmpty(result.getFailReason()), PROVIDER,
                    StrUtil.maxLength(result.getFailReason(), 500));
            }
            return ContentCheckResult.pass(PROVIDER);
        } catch (Exception e) {
            // 图片检测基础设施异常不阻断既有审核链，记 ERROR 后放行（引擎内部另有开关降级）
            log.error("{\"event\":\"content_security_image_delegate_fail\",\"scene\":\"{}\",\"error\":\"{}\"}", scene, e.getMessage());
            return ContentCheckResult.pass(PROVIDER);
        }
    }

    @Override
    public ContentCheckResult checkText(String scene, String text) {
        if (StrUtil.isBlank(text)) {
            return ContentCheckResult.pass(PROVIDER);
        }
        Long tenantId = currentTenantId();
        try {
            SensitiveWordCheckResult result = sensitiveWordEngine.check(text, tenantId, "text", scene, null);
            if (result != null && !result.isPass()) {
                String hitWords = result.getHitWords().stream()
                    .map(SensitiveWordCheckResult.HitItem::getWord)
                    .collect(Collectors.joining("、"));
                log.warn("[ContentSecurity] checkText scene={} 命中敏感词 [{}] → REJECT", scene, hitWords);
                return ContentCheckResult.reject("sensitiveWord:" + hitWords, PROVIDER, "文本包含违禁关键词");
            }
            return ContentCheckResult.pass(PROVIDER);
        } catch (Exception e) {
            log.error("{\"event\":\"content_security_text_delegate_fail\",\"scene\":\"{}\",\"error\":\"{}\"}", scene, e.getMessage());
            return ContentCheckResult.pass(PROVIDER);
        }
    }

    /**
     * 当前租户ID，平台/无登录上下文回落全局底库(0)。
     */
    private Long currentTenantId() {
        Long tenantId = TenantHelper.getTenantId();
        return tenantId != null ? tenantId : 0L;
    }
}
