package com.ruoyi.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ImageComplianceConfig;
import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.compliance.VisualAuditResult;
import com.ruoyi.system.mapper.ImageComplianceConfigMapper;
import com.ruoyi.system.service.IImageComplianceService;
import com.ruoyi.system.service.IOcrProvider;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.IVisualAuditProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 图片合规服务实现（M18 T18.3）
 * <p>上传硬性校验 → OCR 文字路 + 视觉审核路两路并行（threadPoolTaskExecutor）→ 任一 FAIL 即整体 FAIL。
 * <p>FEAT_IMAGE_COMPLIANCE 开关关闭时跳过检测直接放行。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageComplianceServiceImpl implements IImageComplianceService {

    private final ImageComplianceConfigMapper configMapper;
    private final ISensitiveWordEngine sensitiveWordEngine;
    private final IVisualAuditProvider visualAuditProvider;
    private final IOcrProvider ocrProvider;
    private final FeatureChecker featureChecker;
    /** 并行两路检测线程池（bean 名 threadPoolTaskExecutor，字段名与之一致以按名解析） */
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /** 功能开关：图片合规 */
    private static final String FEATURE_CODE = "FEAT_IMAGE_COMPLIANCE";

    /** 兜底默认：最大文件 5MB */
    private static final int DEFAULT_MAX_SIZE_MB = 5;
    /** 兜底默认：允许格式 */
    private static final String DEFAULT_ALLOWED_FORMATS = "jpg,jpeg,png,bmp,webp";
    /** 兜底默认：置信度阈值 */
    private static final double DEFAULT_THRESHOLD = 0.85D;

    @Override
    public ImageComplianceResult checkImage(MultipartFile file, Long tenantId, String bizType, String bizId) {
        long start = System.currentTimeMillis();
        // ★fail-fast
        if (file == null || file.isEmpty()) {
            throw new ServiceException("图片文件不能为空");
        }
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // 开关降级：关闭时跳过检测直接放行
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE)) {
            return ImageComplianceResult.pass(0L);
        }
        List<ImageComplianceConfig> configs = loadEffectiveConfigs(tenantId);
        // 上传硬性校验（大小/格式）
        validateFile(file, configs);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            log.error("{\"event\":\"image_read_fail\",\"component\":\"image-compliance\",\"bizType\":\"{}\",\"error\":\"{}\"}", bizType, e.getMessage());
            throw new ServiceException("图片读取失败");
        }
        ImageComplianceResult result = doCheck(bytes, tenantId, bizType, bizId, configs);
        result.setCost(System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public ImageComplianceResult checkImageByUrl(String imageUrl, Long tenantId, String bizType, String bizId) {
        long start = System.currentTimeMillis();
        // ★fail-fast
        if (StrUtil.isBlank(imageUrl)) {
            throw new ServiceException("图片URL不能为空");
        }
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // 开关降级
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE)) {
            return ImageComplianceResult.pass(0L);
        }
        List<ImageComplianceConfig> configs = loadEffectiveConfigs(tenantId);
        byte[] bytes;
        try {
            bytes = HttpUtil.downloadBytes(imageUrl);
        } catch (Exception e) {
            // URL 下载失败不阻断业务，记 WARN 后放行（避免网络抖动误拦）
            log.warn("{\"event\":\"image_download_fail\",\"component\":\"image-compliance\",\"url\":\"{}\",\"error\":\"{}\"}", imageUrl, e.getMessage());
            return ImageComplianceResult.pass(System.currentTimeMillis() - start);
        }
        if (bytes == null || bytes.length == 0) {
            log.warn("{\"event\":\"image_download_empty\",\"component\":\"image-compliance\",\"url\":\"{}\"}", imageUrl);
            return ImageComplianceResult.pass(System.currentTimeMillis() - start);
        }
        ImageComplianceResult result = doCheck(bytes, tenantId, bizType, bizId, configs);
        result.setCost(System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 两路并行检测核心。
     */
    private ImageComplianceResult doCheck(byte[] bytes, Long tenantId, String bizType, String bizId,
                                          List<ImageComplianceConfig> configs) {
        boolean ocrEnabled = configs.stream().anyMatch(c -> intTrue(c.getOcrEnabled()));
        List<String> visualTypes = configs.stream()
            .filter(c -> intTrue(c.getEnabled()) && intTrue(c.getVisualApiEnabled()))
            .map(ImageComplianceConfig::getViolationType)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        double threshold = configs.stream()
            .filter(c -> intTrue(c.getEnabled()) && c.getConfidenceThreshold() != null)
            .map(c -> c.getConfidenceThreshold().doubleValue())
            .min(Double::compare)
            .orElse(DEFAULT_THRESHOLD);

        // 路① OCR → 敏感词
        CompletableFuture<SensitiveWordCheckResult> ocrFuture = ocrEnabled
            ? CompletableFuture.supplyAsync(() -> {
                try {
                    String ocrText = ocrProvider.extractText(bytes);
                    if (StrUtil.isBlank(ocrText)) {
                        return SensitiveWordCheckResult.pass(0L);
                    }
                    return sensitiveWordEngine.check(ocrText, tenantId, "image_ocr", bizType, bizId);
                } catch (Exception e) {
                    log.error("{\"event\":\"ocr_path_fail\",\"component\":\"ocr\",\"bizType\":\"{}\",\"error\":\"{}\"}", bizType, e.getMessage());
                    return SensitiveWordCheckResult.pass(0L);
                }
            }, threadPoolTaskExecutor)
            : CompletableFuture.completedFuture(SensitiveWordCheckResult.pass(0L));

        // 路② 视觉审核
        CompletableFuture<VisualAuditResult> visualFuture = !visualTypes.isEmpty()
            ? CompletableFuture.supplyAsync(() -> {
                try {
                    return visualAuditProvider.audit(bytes, visualTypes, threshold);
                } catch (Exception e) {
                    log.error("{\"event\":\"visual_path_fail\",\"component\":\"visual-audit\",\"bizType\":\"{}\",\"error\":\"{}\"}", bizType, e.getMessage());
                    return VisualAuditResult.pass("视觉审核异常降级放行");
                }
            }, threadPoolTaskExecutor)
            : CompletableFuture.completedFuture(VisualAuditResult.pass("未启用视觉审核"));

        // 等待两路完成
        CompletableFuture.allOf(ocrFuture, visualFuture).join();
        SensitiveWordCheckResult ocrResult = ocrFuture.join();
        VisualAuditResult visualResult = visualFuture.join();

        // 任一 FAIL 即整体 FAIL
        boolean ocrFail = ocrResult != null && !ocrResult.isPass();
        boolean visualFail = visualResult != null && !visualResult.isPass();
        if (!ocrFail && !visualFail) {
            return ImageComplianceResult.pass(0L);
        }

        ImageComplianceResult fail = new ImageComplianceResult();
        fail.setResult(ImageComplianceResult.FAIL);
        List<String> reasons = new ArrayList<>();
        if (ocrFail) {
            fail.setOcrHits(ocrResult.getHitWords());
            String words = ocrResult.getHitWords().stream()
                .map(SensitiveWordCheckResult.HitItem::getWord)
                .collect(Collectors.joining("、"));
            reasons.add("图片文字含违禁词：" + words);
        }
        if (visualFail) {
            fail.getVisualHits().add(new ImageComplianceResult.VisualHit(
                visualResult.getViolationType(), visualResult.getConfidence()));
            reasons.add("图片视觉违规：" + visualResult.getViolationType());
        }
        fail.setFailReason("图片未通过合规检查，请更换后重新上传：" + String.join("；", reasons));
        log.warn("{\"event\":\"image_compliance_fail\",\"tenantId\":{},\"bizType\":\"{}\",\"ocrFail\":{},\"visualFail\":{},\"reason\":\"{}\"}",
            tenantId, bizType, ocrFail, visualFail, fail.getFailReason());
        return fail;
    }

    /**
     * 上传硬性校验：大小 ≤ maxFileSizeMb、格式在 allowedFormats 内。
     */
    private void validateFile(MultipartFile file, List<ImageComplianceConfig> configs) {
        int maxSizeMb = configs.stream()
            .filter(c -> c.getMaxFileSizeMb() != null)
            .map(ImageComplianceConfig::getMaxFileSizeMb)
            .max(Integer::compare)
            .orElse(DEFAULT_MAX_SIZE_MB);
        String allowedFormats = configs.stream()
            .map(ImageComplianceConfig::getAllowedFormats)
            .filter(StrUtil::isNotBlank)
            .findFirst()
            .orElse(DEFAULT_ALLOWED_FORMATS);

        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ServiceException("图片大小不能超过 " + maxSizeMb + "MB");
        }
        String ext = FileUtil.extName(StrUtil.nullToEmpty(file.getOriginalFilename())).toLowerCase();
        List<String> allowed = Arrays.stream(allowedFormats.split(","))
            .map(s -> s.trim().toLowerCase())
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toList());
        if (StrUtil.isBlank(ext) || !allowed.contains(ext)) {
            throw new ServiceException("图片格式不支持，仅允许：" + allowedFormats);
        }
    }

    /**
     * 加载生效配置：租户覆盖优先，缺失回落全局默认(tenant_id=0)；无配置时用兜底默认三行。
     */
    private List<ImageComplianceConfig> loadEffectiveConfigs(Long tenantId) {
        List<ImageComplianceConfig> raw;
        try {
            raw = configMapper.selectEffective(tenantId);
        } catch (Exception e) {
            log.error("{\"event\":\"image_config_load_fail\",\"component\":\"image-compliance\",\"tenantId\":{},\"error\":\"{}\"}", tenantId, e.getMessage());
            raw = null;
        }
        if (raw == null || raw.isEmpty()) {
            return defaultConfigs();
        }
        // 按 violationType 去重，租户级（tenant_id!=0）优先
        Map<String, ImageComplianceConfig> byType = new LinkedHashMap<>();
        for (ImageComplianceConfig c : raw) {
            if (StrUtil.isBlank(c.getViolationType())) {
                continue;
            }
            ImageComplianceConfig exist = byType.get(c.getViolationType());
            if (exist == null || (exist.getTenantId() != null && exist.getTenantId() == 0L
                && c.getTenantId() != null && c.getTenantId() != 0L)) {
                byType.put(c.getViolationType(), c);
            }
        }
        return byType.isEmpty() ? defaultConfigs() : new ArrayList<>(byType.values());
    }

    /**
     * 兜底默认三行配置（表未就绪或无数据时使用，全部启用）。
     */
    private List<ImageComplianceConfig> defaultConfigs() {
        List<ImageComplianceConfig> list = new ArrayList<>();
        for (String type : Arrays.asList("porn", "violence", "politics")) {
            ImageComplianceConfig c = new ImageComplianceConfig();
            c.setTenantId(0L);
            c.setViolationType(type);
            c.setEnabled(1);
            c.setConfidenceThreshold(BigDecimal.valueOf(DEFAULT_THRESHOLD));
            c.setOcrEnabled(1);
            c.setVisualApiEnabled(1);
            c.setMaxFileSizeMb(DEFAULT_MAX_SIZE_MB);
            c.setAllowedFormats(DEFAULT_ALLOWED_FORMATS);
            list.add(c);
        }
        return list;
    }

    private boolean intTrue(Integer v) {
        return v != null && v == 1;
    }
}
