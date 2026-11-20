package com.ruoyi.test.unit;

import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ImageComplianceConfig;
import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.compliance.VisualAuditResult;
import com.ruoyi.system.mapper.ImageComplianceConfigMapper;
import com.ruoyi.system.service.IOcrProvider;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.IVisualAuditProvider;
import com.ruoyi.system.service.impl.ImageComplianceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 图片合规服务单元测试（M18 T18.3）
 * <p>覆盖：上传硬性校验（空/大小/格式）、两路并行、任一 FAIL 即 FAIL、Provider 异常降级、开关降级。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Tag("compliance")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("M18 图片合规服务验证")
public class ImageComplianceServiceTest {

    private static final String FEATURE_CODE = "FEAT_IMAGE_COMPLIANCE";
    private static final long TENANT = 100L;

    @Mock
    private ImageComplianceConfigMapper configMapper;

    @Mock
    private ISensitiveWordEngine sensitiveWordEngine;

    @Mock
    private IVisualAuditProvider visualAuditProvider;

    @Mock
    private IOcrProvider ocrProvider;

    @Mock
    private FeatureChecker featureChecker;

    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @InjectMocks
    private ImageComplianceServiceImpl service;

    @BeforeEach
    void setUp() {
        // 让 supplyAsync 提交的任务在当前线程同步执行，避免 join() 永久阻塞
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(threadPoolTaskExecutor).execute(any(Runnable.class));
    }

    private MultipartFile png(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private MultipartFile validPng() {
        return png("test.png", new byte[]{1, 2, 3, 4});
    }

    private List<ImageComplianceConfig> fullConfigs() {
        List<ImageComplianceConfig> list = new ArrayList<>();
        for (String type : Arrays.asList("porn", "violence", "politics")) {
            ImageComplianceConfig c = new ImageComplianceConfig();
            c.setTenantId(0L);
            c.setViolationType(type);
            c.setEnabled(1);
            c.setConfidenceThreshold(BigDecimal.valueOf(0.85D));
            c.setOcrEnabled(1);
            c.setVisualApiEnabled(1);
            c.setMaxFileSizeMb(5);
            c.setAllowedFormats("jpg,jpeg,png,bmp,webp");
            list.add(c);
        }
        return list;
    }

    // ==================== fail-fast + 硬性校验 ====================

    @Test
    @DisplayName("文件为 null → ServiceException")
    void testNullFile() {
        assertThrows(ServiceException.class, () -> service.checkImage(null, TENANT, "rating", "1"));
    }

    @Test
    @DisplayName("空文件 → ServiceException")
    void testEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(ServiceException.class, () -> service.checkImage(empty, TENANT, "rating", "1"));
    }

    @Test
    @DisplayName("tenantId 为 null → ServiceException")
    void testNullTenant() {
        assertThrows(ServiceException.class, () -> service.checkImage(validPng(), null, "rating", "1"));
    }

    @Test
    @DisplayName("格式不支持 → ServiceException")
    void testUnsupportedFormat() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        MultipartFile gif = png("test.gif", new byte[]{1, 2, 3});

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.checkImage(gif, TENANT, "rating", "1"));
        assertTrue(ex.getMessage().contains("格式"));
    }

    @Test
    @DisplayName("超过大小上限 → ServiceException")
    void testOversizeFile() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        List<ImageComplianceConfig> configs = fullConfigs();
        configs.forEach(c -> c.setMaxFileSizeMb(1));
        when(configMapper.selectEffective(TENANT)).thenReturn(configs);
        byte[] big = new byte[2 * 1024 * 1024];
        MultipartFile file = png("big.png", big);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.checkImage(file, TENANT, "rating", "1"));
        assertTrue(ex.getMessage().contains("大小"));
    }

    // ==================== 开关降级 ====================

    @Test
    @DisplayName("开关关闭 → 直接 PASS，不触发任何 Provider")
    void testFeatureDisabled() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(false);

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertTrue(r.isPass());
        verifyNoInteractions(ocrProvider, visualAuditProvider, sensitiveWordEngine);
    }

    @Test
    @DisplayName("checkImageByUrl 开关关闭 → 直接 PASS")
    void testByUrlFeatureDisabled() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(false);

        ImageComplianceResult r = service.checkImageByUrl("http://x/a.png", TENANT, "rating", "1");

        assertTrue(r.isPass());
    }

    @Test
    @DisplayName("checkImageByUrl URL 为空 → ServiceException")
    void testByUrlBlank() {
        assertThrows(ServiceException.class, () -> service.checkImageByUrl("  ", TENANT, "rating", "1"));
    }

    // ==================== 两路并行 + 任一 FAIL 即 FAIL ====================

    @Test
    @DisplayName("两路均通过 → PASS")
    void testBothPass() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenReturn("正常文字");
        when(sensitiveWordEngine.check(anyString(), eq(TENANT), eq("image_ocr"), anyString(), any()))
            .thenReturn(SensitiveWordCheckResult.pass(1L));
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.pass("ok"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertTrue(r.isPass());
        verify(ocrProvider).extractText(any());
        verify(visualAuditProvider).audit(any(), anyList(), anyDouble());
    }

    @Test
    @DisplayName("OCR 路命中敏感词 → 整体 FAIL")
    void testOcrPathFail() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenReturn("含违禁词");
        List<SensitiveWordCheckResult.HitItem> hits = new ArrayList<>();
        hits.add(new SensitiveWordCheckResult.HitItem("违禁词", "POLITICAL", 1));
        when(sensitiveWordEngine.check(anyString(), eq(TENANT), eq("image_ocr"), anyString(), any()))
            .thenReturn(SensitiveWordCheckResult.fail(hits, 1L));
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.pass("ok"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertFalse(r.isPass());
        assertEquals(ImageComplianceResult.FAIL, r.getResult());
        assertTrue(r.getFailReason().contains("违禁词"));
        assertEquals(1, r.getOcrHits().size());
    }

    @Test
    @DisplayName("视觉路命中违规 → 整体 FAIL")
    void testVisualPathFail() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenReturn("");
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.reject("porn", 0.97D, "命中"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertFalse(r.isPass());
        assertTrue(r.getFailReason().contains("视觉违规"));
        assertEquals(1, r.getVisualHits().size());
        assertEquals("porn", r.getVisualHits().get(0).getViolationType());
    }

    @Test
    @DisplayName("两路均命中 → FAIL 且原因拼接两段")
    void testBothFail() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenReturn("含违禁词");
        List<SensitiveWordCheckResult.HitItem> hits = new ArrayList<>();
        hits.add(new SensitiveWordCheckResult.HitItem("违禁词", "POLITICAL", 1));
        when(sensitiveWordEngine.check(anyString(), eq(TENANT), eq("image_ocr"), anyString(), any()))
            .thenReturn(SensitiveWordCheckResult.fail(hits, 1L));
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.reject("violence", 0.91D, "命中"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertFalse(r.isPass());
        assertTrue(r.getFailReason().contains("违禁词"));
        assertTrue(r.getFailReason().contains("视觉违规"));
    }

    // ==================== Provider 异常降级 ====================

    @Test
    @DisplayName("OCR Provider 抛异常 → 该路降级放行，视觉通过则整体 PASS")
    void testOcrProviderException() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenThrow(new RuntimeException("OCR 服务不可用"));
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.pass("ok"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertTrue(r.isPass());
    }

    @Test
    @DisplayName("视觉 Provider 抛异常 → 该路降级放行，OCR 通过则整体 PASS")
    void testVisualProviderException() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(fullConfigs());
        when(ocrProvider.extractText(any())).thenReturn("");
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenThrow(new RuntimeException("视觉服务超时"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertTrue(r.isPass());
    }

    // ==================== 配置回落 ====================

    @Test
    @DisplayName("无配置记录 → 回落兜底默认三行，两路仍执行")
    void testFallbackDefaultConfigs() {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        when(configMapper.selectEffective(TENANT)).thenReturn(new ArrayList<>());
        when(ocrProvider.extractText(any())).thenReturn("");
        when(visualAuditProvider.audit(any(), anyList(), anyDouble()))
            .thenReturn(VisualAuditResult.pass("ok"));

        ImageComplianceResult r = service.checkImage(validPng(), TENANT, "rating", "1");

        assertTrue(r.isPass());
        verify(visualAuditProvider).audit(any(), anyList(), anyDouble());
    }
}
