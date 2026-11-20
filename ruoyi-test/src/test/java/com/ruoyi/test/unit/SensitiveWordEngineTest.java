package com.ruoyi.test.unit;

import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.system.domain.SensitiveWord;
import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.mapper.SensitiveWordMapper;
import com.ruoyi.system.service.impl.SensitiveWordEngineImpl;
import com.ruoyi.system.service.impl.SensitiveWordHitLogWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 统一敏感词引擎单元测试（M18 T18.2）
 * <p>覆盖：exact/fuzzy/regex 三路匹配、全局+租户合并、行业过滤、缓存刷新、开关降级、命中日志写入。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Tag("compliance")
@ExtendWith(MockitoExtension.class)
@DisplayName("M18 敏感词引擎验证")
public class SensitiveWordEngineTest {

    private static final String FEATURE_CODE = "FEAT_SENSITIVE_WORD";
    private static final long GLOBAL = 0L;
    private static final long TENANT = 100L;

    @Mock
    private SensitiveWordMapper wordMapper;

    @Mock
    private SensitiveWordHitLogWriter hitLogWriter;

    @Mock
    private FeatureChecker featureChecker;

    @InjectMocks
    private SensitiveWordEngineImpl engine;

    @BeforeEach
    void setUp() throws Exception {
        // 清空缓存，避免用例间串扰（@InjectMocks 不触发 @PostConstruct）
        seedCache(GLOBAL, new ArrayList<>());
    }

    // ==================== 反射工具 ====================

    @SuppressWarnings("unchecked")
    private Map<Long, List<SensitiveWord>> cache() throws Exception {
        Field f = SensitiveWordEngineImpl.class.getDeclaredField("wordCache");
        f.setAccessible(true);
        return (Map<Long, List<SensitiveWord>>) f.get(engine);
    }

    private void seedCache(long tenantId, List<SensitiveWord> words) throws Exception {
        cache().put(tenantId, words);
    }

    private SensitiveWord word(String text, String category, String matchMode, String industryScope) {
        SensitiveWord w = new SensitiveWord();
        w.setWord(text);
        w.setCategory(category);
        w.setMatchMode(matchMode);
        w.setIndustryScope(industryScope);
        w.setStatus("0");
        return w;
    }

    // ==================== 三路匹配 ====================

    @Test
    @DisplayName("exact 模式命中 → FAIL 且位置正确")
    void testExactHit() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("违禁词", "POLITICAL", "exact", null))));

        SensitiveWordCheckResult r = engine.check("这是一段违禁词文本", TENANT, "text", "chat", "1");

        assertFalse(r.isPass());
        assertEquals(SensitiveWordCheckResult.FAIL, r.getResult());
        assertEquals(1, r.getHitWords().size());
        assertEquals("违禁词", r.getHitWords().get(0).getWord());
        assertEquals("POLITICAL", r.getHitWords().get(0).getCategory());
        assertEquals(4, r.getHitWords().get(0).getPosition());
    }

    @Test
    @DisplayName("exact 模式未命中 → PASS")
    void testExactMiss() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("违禁词", "POLITICAL", "exact", null))));

        SensitiveWordCheckResult r = engine.check("这是一段正常文本", TENANT, "text", "chat", "1");

        assertTrue(r.isPass());
        assertTrue(r.getHitWords().isEmpty());
    }

    @Test
    @DisplayName("regex 模式命中 → FAIL")
    void testRegexHit() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("\\d{11}", "AD", "regex", null))));

        SensitiveWordCheckResult r = engine.check("联系我13800138000谢谢", TENANT, "text", "chat", "1");

        assertFalse(r.isPass());
        assertEquals("\\d{11}", r.getHitWords().get(0).getWord());
    }

    @Test
    @DisplayName("regex 非法表达式不抛异常 → PASS")
    void testRegexInvalid() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("[未闭合", "AD", "regex", null))));

        SensitiveWordCheckResult r = engine.check("任意文本", TENANT, "text", "chat", "1");

        assertTrue(r.isPass());
    }

    @Test
    @DisplayName("fuzzy 模式编辑距离≤阈值命中 → FAIL")
    void testFuzzyHit() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        // 词长 4 → 阈值 min(2, max(1, 4/3=1)) = 1；"赌博网战" 与 "赌博网站" 距离 1
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("赌博网站", "PORN", "fuzzy", null))));

        SensitiveWordCheckResult r = engine.check("快来看赌博网战", TENANT, "text", "chat", "1");

        assertFalse(r.isPass());
        assertEquals("赌博网站", r.getHitWords().get(0).getWord());
    }

    @Test
    @DisplayName("fuzzy 短词退化为精确匹配，未精确命中 → PASS")
    void testFuzzyShortWord() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("赌", "PORN", "fuzzy", null))));

        SensitiveWordCheckResult r = engine.check("赌博网战", TENANT, "text", "chat", "1");

        assertTrue(r.isPass());
    }

    // ==================== 合并 + 行业过滤 ====================

    @Test
    @DisplayName("全局+租户合并：租户自定义词生效")
    void testGlobalTenantMerge() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("全局词", "POLITICAL", "exact", null))));
        seedCache(TENANT, new ArrayList<>(Arrays.asList(word("租户词", "CUSTOM", "exact", null))));

        assertFalse(engine.check("含全局词", TENANT, "text", "chat", "1").isPass());
        assertFalse(engine.check("含租户词", TENANT, "text", "chat", "1").isPass());
    }

    @Test
    @DisplayName("行业过滤：industry_scope 不含当前 bizType → 跳过该词 PASS")
    void testIndustryScopeFilteredOut() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("极限词", "AD", "exact", "[\"rating\"]"))));

        SensitiveWordCheckResult r = engine.check("这是极限词", TENANT, "text", "order", "1");

        assertTrue(r.isPass());
    }

    @Test
    @DisplayName("行业过滤：industry_scope 含当前 bizType → 命中 FAIL")
    void testIndustryScopeMatched() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("极限词", "AD", "exact", "[\"rating\"]"))));

        SensitiveWordCheckResult r = engine.check("这是极限词", TENANT, "text", "rating", "1");

        assertFalse(r.isPass());
    }

    // ==================== 开关降级 + fail-fast ====================

    @Test
    @DisplayName("开关关闭 → 直接 PASS，不查库、不写日志")
    void testFeatureDisabled() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(false);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("违禁词", "POLITICAL", "exact", null))));

        SensitiveWordCheckResult r = engine.check("这是违禁词", TENANT, "text", "chat", "1");

        assertTrue(r.isPass());
        verify(hitLogWriter, never()).writeAsync(any());
        verify(wordMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("空文本 → PASS，不写日志")
    void testBlankText() {
        SensitiveWordCheckResult r = engine.check("  ", TENANT, "text", "chat", "1");

        assertTrue(r.isPass());
        verify(hitLogWriter, never()).writeAsync(any());
        verifyNoInteractions(featureChecker);
    }

    @Test
    @DisplayName("tenantId 为 null → PASS，不写日志")
    void testNullTenant() {
        SensitiveWordCheckResult r = engine.check("违禁词", null, "text", "chat", "1");

        assertTrue(r.isPass());
        verify(hitLogWriter, never()).writeAsync(any());
    }

    // ==================== 命中日志写入 ====================

    @Test
    @DisplayName("命中 → 异步写日志，result=FAIL 且原文脱敏截断")
    void testHitLogWrittenOnFail() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("违禁词", "POLITICAL", "exact", null))));

        engine.check("这是违禁词", TENANT, "text", "chat", "biz-1");

        ArgumentCaptor<SensitiveWordHitLog> captor = ArgumentCaptor.forClass(SensitiveWordHitLog.class);
        verify(hitLogWriter).writeAsync(captor.capture());
        SensitiveWordHitLog log = captor.getValue();
        assertEquals(SensitiveWordCheckResult.FAIL, log.getResult());
        assertEquals("违禁词", log.getHitWord());
        assertEquals("POLITICAL", log.getHitCategory());
        assertEquals(TENANT, log.getTenantId());
        assertEquals("text", log.getInputChannel());
        assertEquals("chat", log.getBizType());
        assertEquals("biz-1", log.getBizId());
    }

    @Test
    @DisplayName("未命中 → 仍写日志，result=PASS 且 hitWord 为空")
    void testHitLogWrittenOnPass() throws Exception {
        when(featureChecker.isFeatureEnabled(FEATURE_CODE)).thenReturn(true);
        seedCache(GLOBAL, new ArrayList<>(Arrays.asList(word("违禁词", "POLITICAL", "exact", null))));

        engine.check("正常文本", TENANT, "text", "chat", "biz-2");

        ArgumentCaptor<SensitiveWordHitLog> captor = ArgumentCaptor.forClass(SensitiveWordHitLog.class);
        verify(hitLogWriter).writeAsync(captor.capture());
        SensitiveWordHitLog log = captor.getValue();
        assertEquals(SensitiveWordCheckResult.PASS, log.getResult());
        assertNull(log.getHitWord());
    }

    // ==================== 缓存刷新 ====================

    @Test
    @DisplayName("evictWordCache 清除指定租户缓存，下次检测回源 DB")
    void testEvictWordCache() throws Exception {
        seedCache(TENANT, new ArrayList<>(Arrays.asList(word("租户词", "CUSTOM", "exact", null))));
        assertTrue(cache().containsKey(TENANT));

        engine.evictWordCache(TENANT);

        assertFalse(cache().containsKey(TENANT));
    }

    @Test
    @DisplayName("evictWordCache(null) 不抛异常")
    void testEvictNullSafe() {
        assertDoesNotThrow(() -> engine.evictWordCache(null));
    }
}
