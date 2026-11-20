package com.ruoyi.test.unit;

import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.service.IImageComplianceService;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.security.ContentCheckResult;
import com.ruoyi.system.service.security.impl.ContentSecurityBlacklistChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 内容安全审核委派验证（M18 T18.6 收口）
 * <p>验证 {@link ContentSecurityBlacklistChecker} 的 checkText/checkImage 已委派到 M18 引擎
 *   （ISensitiveWordEngine / IImageComplianceService），而非本地黑名单桩逻辑。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Tag("compliance")
@ExtendWith(MockitoExtension.class)
@DisplayName("M18 T18.6 内容安全委派验证")
public class ContentSecurityBlacklistCheckerDelegationTest {

    private static final String PROVIDER = "sensitiveWordEngine";

    @Mock
    private ISensitiveWordEngine sensitiveWordEngine;

    @Mock
    private IImageComplianceService imageComplianceService;

    @InjectMocks
    private ContentSecurityBlacklistChecker checker;

    // ==================== checkText 委派 ====================

    @Test
    @DisplayName("checkText 命中 → 委派引擎且返回 REJECT")
    void testCheckTextDelegatesAndRejects() {
        List<SensitiveWordCheckResult.HitItem> hits = new ArrayList<>();
        hits.add(new SensitiveWordCheckResult.HitItem("违禁词", "POLITICAL", 0));
        when(sensitiveWordEngine.check(eq("含违禁词"), any(), eq("text"), eq("chat"), isNull()))
            .thenReturn(SensitiveWordCheckResult.fail(hits, 1L));

        ContentCheckResult r = checker.checkText("chat", "含违禁词");

        assertFalse(r.isPass());
        assertEquals("REJECT", r.getResult());
        assertEquals(PROVIDER, r.getProvider());
        assertTrue(r.getLabel().contains("违禁词"));
        verify(sensitiveWordEngine).check(eq("含违禁词"), any(), eq("text"), eq("chat"), isNull());
    }

    @Test
    @DisplayName("checkText 未命中 → 委派引擎且返回 PASS")
    void testCheckTextDelegatesAndPasses() {
        when(sensitiveWordEngine.check(anyString(), any(), eq("text"), anyString(), isNull()))
            .thenReturn(SensitiveWordCheckResult.pass(1L));

        ContentCheckResult r = checker.checkText("chat", "正常文本");

        assertTrue(r.isPass());
        assertEquals(PROVIDER, r.getProvider());
        verify(sensitiveWordEngine).check(eq("正常文本"), any(), eq("text"), eq("chat"), isNull());
    }

    @Test
    @DisplayName("checkText 空文本 → 直接 PASS，不委派引擎")
    void testCheckTextBlank() {
        ContentCheckResult r = checker.checkText("chat", "  ");

        assertTrue(r.isPass());
        verifyNoInteractions(sensitiveWordEngine);
    }

    @Test
    @DisplayName("checkText 引擎抛异常 → 降级放行，不阻断既有审核链")
    void testCheckTextEngineException() {
        when(sensitiveWordEngine.check(anyString(), any(), anyString(), anyString(), isNull()))
            .thenThrow(new RuntimeException("引擎异常"));

        ContentCheckResult r = checker.checkText("chat", "任意文本");

        assertTrue(r.isPass());
    }

    // ==================== checkImage 委派 ====================

    @Test
    @DisplayName("checkImage 命中 → 委派图片服务且返回 REJECT")
    void testCheckImageDelegatesAndRejects() {
        ImageComplianceResult fail = new ImageComplianceResult();
        fail.setResult(ImageComplianceResult.FAIL);
        fail.setFailReason("图片视觉违规：porn");
        when(imageComplianceService.checkImageByUrl(eq("http://x/a.png"), any(), eq("poster"), isNull()))
            .thenReturn(fail);

        ContentCheckResult r = checker.checkImage("poster", "http://x/a.png");

        assertFalse(r.isPass());
        assertEquals("REJECT", r.getResult());
        assertEquals(PROVIDER, r.getProvider());
        verify(imageComplianceService).checkImageByUrl(eq("http://x/a.png"), any(), eq("poster"), isNull());
    }

    @Test
    @DisplayName("checkImage 未命中 → 委派图片服务且返回 PASS")
    void testCheckImageDelegatesAndPasses() {
        when(imageComplianceService.checkImageByUrl(anyString(), any(), anyString(), isNull()))
            .thenReturn(ImageComplianceResult.pass(1L));

        ContentCheckResult r = checker.checkImage("poster", "http://x/a.png");

        assertTrue(r.isPass());
        assertEquals(PROVIDER, r.getProvider());
        verify(imageComplianceService).checkImageByUrl(eq("http://x/a.png"), any(), eq("poster"), isNull());
    }

    @Test
    @DisplayName("checkImage 空 URL → 返回 ERROR，不委派图片服务")
    void testCheckImageBlankUrl() {
        ContentCheckResult r = checker.checkImage("poster", "  ");

        assertFalse(r.isPass());
        assertEquals("ERROR", r.getResult());
        verifyNoInteractions(imageComplianceService);
    }

    @Test
    @DisplayName("checkImage 图片服务抛异常 → 降级放行")
    void testCheckImageServiceException() {
        when(imageComplianceService.checkImageByUrl(anyString(), any(), anyString(), isNull()))
            .thenThrow(new RuntimeException("图片服务异常"));

        ContentCheckResult r = checker.checkImage("poster", "http://x/a.png");

        assertTrue(r.isPass());
    }
}
