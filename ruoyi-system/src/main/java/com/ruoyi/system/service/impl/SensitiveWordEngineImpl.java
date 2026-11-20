package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SensitiveWord;
import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.mapper.SensitiveWordMapper;
import com.ruoyi.system.service.ISensitiveWordEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一敏感词引擎实现（M18 T18.2）
 * <p>词库缓存：{@code wordCache} key=tenantId（0=全局底库），value=该租户启用词；@PostConstruct 预热全局底库。
 * <p>check()：fail-fast → 开关降级 → 合并全局+租户词去重（同 word+category 优先租户级）→ 行业过滤 →
 *   exact/fuzzy/regex 三路匹配 → 命中即 FAIL → PASS/FAIL 均异步写命中日志。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordEngineImpl implements ISensitiveWordEngine {

    private final SensitiveWordMapper wordMapper;
    private final SensitiveWordHitLogWriter hitLogWriter;
    private final FeatureChecker featureChecker;

    /** 功能开关：敏感词 */
    private static final String FEATURE_CODE = "FEAT_SENSITIVE_WORD";

    /** 全局底库租户键 */
    private static final long GLOBAL_TENANT = 0L;

    /** 原文脱敏截取长度 */
    private static final int ORIGINAL_TEXT_MAX = 100;

    /** 词库缓存：key=tenantId（0=全局底库），value=该租户启用词列表 */
    private final Map<Long, List<SensitiveWord>> wordCache = new ConcurrentHashMap<>();

    /**
     * 预热全局底库缓存。
     */
    @PostConstruct
    public void warmUp() {
        try {
            List<SensitiveWord> globalWords = loadFromDb(GLOBAL_TENANT);
            wordCache.put(GLOBAL_TENANT, globalWords);
            log.info("[SensitiveWordEngine] 全局底库预热完成，count={}", globalWords.size());
        } catch (Exception e) {
            log.error("{\"event\":\"sensitive_word_warmup_fail\",\"error\":\"{}\"}", e.getMessage());
        }
    }

    @Override
    public SensitiveWordCheckResult check(String text, Long tenantId, String inputChannel, String bizType, String bizId) {
        long start = System.currentTimeMillis();
        // ★fail-fast
        if (StrUtil.isBlank(text)) {
            // 空文本无需检测，直接放行（不写日志）
            return SensitiveWordCheckResult.pass(0L);
        }
        if (tenantId == null) {
            log.warn("{\"event\":\"sensitive_word_check_skip\",\"reason\":\"tenantId_null\",\"channel\":\"{}\"}", inputChannel);
            return SensitiveWordCheckResult.pass(0L);
        }
        if (StrUtil.isBlank(inputChannel)) {
            inputChannel = "text";
        }
        // ★开关降级：关闭时直接放行，不查库、不写日志
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE)) {
            return SensitiveWordCheckResult.pass(0L);
        }

        // 合并全局底库 + 租户词，去重（同 word+category 优先租户级），行业过滤
        List<SensitiveWord> effectiveWords = mergeEffectiveWords(tenantId, bizType);

        // 三路匹配
        SensitiveWordCheckResult.HitItem firstHit = null;
        for (SensitiveWord w : effectiveWords) {
            int position = matchPosition(text, w);
            if (position >= 0) {
                firstHit = new SensitiveWordCheckResult.HitItem(w.getWord(), w.getCategory(), position);
                break;
            }
        }

        long cost = System.currentTimeMillis() - start;
        SensitiveWordCheckResult result;
        if (firstHit != null) {
            List<SensitiveWordCheckResult.HitItem> hits = new ArrayList<>();
            hits.add(firstHit);
            result = SensitiveWordCheckResult.fail(hits, cost);
            log.warn("{\"event\":\"sensitive_word_hit\",\"tenantId\":{},\"channel\":\"{}\",\"bizType\":\"{}\",\"hitWord\":\"{}\",\"category\":\"{}\",\"cost\":{}}",
                tenantId, inputChannel, bizType, firstHit.getWord(), firstHit.getCategory(), cost);
        } else {
            result = SensitiveWordCheckResult.pass(cost);
        }

        // PASS/FAIL 均异步写命中日志
        writeHitLog(text, tenantId, inputChannel, bizType, bizId, firstHit);
        return result;
    }

    @Override
    public void evictWordCache(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        wordCache.remove(tenantId);
        log.info("[SensitiveWordEngine] 词库缓存已刷新 tenantId={}", tenantId);
    }

    /**
     * 合并全局底库 + 租户词，去重（同 word+category 优先租户级），并按 bizType 做行业过滤。
     */
    private List<SensitiveWord> mergeEffectiveWords(Long tenantId, String bizType) {
        List<SensitiveWord> globalWords = wordCache.computeIfAbsent(GLOBAL_TENANT, this::loadFromDb);
        List<SensitiveWord> tenantWords = tenantId == GLOBAL_TENANT
            ? globalWords
            : wordCache.computeIfAbsent(tenantId, this::loadFromDb);

        // 去重：key=word+"|"+category，租户级优先覆盖全局级
        Map<String, SensitiveWord> merged = new LinkedHashMap<>();
        for (SensitiveWord w : globalWords) {
            merged.put(dedupKey(w), w);
        }
        for (SensitiveWord w : tenantWords) {
            merged.put(dedupKey(w), w);
        }

        // 行业过滤：industry_scope 为空=全行业通用；非空=JSON 数组须包含 bizType
        List<SensitiveWord> result = new ArrayList<>(merged.size());
        for (SensitiveWord w : merged.values()) {
            if (matchIndustryScope(w.getIndustryScope(), bizType)) {
                result.add(w);
            }
        }
        return result;
    }

    private String dedupKey(SensitiveWord w) {
        return StrUtil.nullToEmpty(w.getWord()) + "|" + StrUtil.nullToEmpty(w.getCategory());
    }

    /**
     * 行业范围匹配：scope 为空/NULL 全行业通用；否则解析 JSON 数组判断是否含 bizType。
     */
    private boolean matchIndustryScope(String industryScope, String bizType) {
        if (StrUtil.isBlank(industryScope)) {
            return true;
        }
        if (StrUtil.isBlank(bizType)) {
            // 有行业限制但调用方未传 bizType，保守放行（不因过滤而误拦）
            return true;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(industryScope);
            return arr.contains(bizType);
        } catch (Exception e) {
            log.warn("{\"event\":\"industry_scope_parse_fail\",\"scope\":\"{}\",\"error\":\"{}\"}", industryScope, e.getMessage());
            return true;
        }
    }

    /**
     * 按 match_mode 三路匹配，返回命中位置（-1=未命中）。
     */
    private int matchPosition(String text, SensitiveWord w) {
        String word = w.getWord();
        if (StrUtil.isBlank(word)) {
            return -1;
        }
        String mode = StrUtil.blankToDefault(w.getMatchMode(), "exact");
        switch (mode) {
            case "regex":
                return matchRegex(text, word);
            case "fuzzy":
                return matchFuzzy(text, word);
            case "exact":
            default:
                return text.indexOf(word);
        }
    }

    private int matchRegex(String text, String word) {
        try {
            Matcher m = Pattern.compile(word).matcher(text);
            return m.find() ? m.start() : -1;
        } catch (Exception e) {
            log.warn("{\"event\":\"sensitive_word_regex_invalid\",\"word\":\"{}\",\"error\":\"{}\"}", word, e.getMessage());
            return -1;
        }
    }

    /**
     * 模糊匹配：滑动窗口 Levenshtein 编辑距离 ≤ 阈值（词越长阈值越大，上限 2）。
     * <p>短词（<3 字符）退化为全文精确相等匹配（text.equals(word)），避免单字/双字
     * 作为子串在正常文本中高频出现导致误命中。
     */
    private int matchFuzzy(String text, String word) {
        int wordLen = word.length();
        if (wordLen < 3) {
            // 短词退化：仅当整段文本与词完全相同时才命中
            return text.equals(word) ? 0 : -1;
        }
        // 优先精确子串命中
        int exact = text.indexOf(word);
        if (exact >= 0) {
            return exact;
        }
        int threshold = Math.min(2, Math.max(1, wordLen / 3));
        int textLen = text.length();
        for (int winLen = wordLen - threshold; winLen <= wordLen + threshold; winLen++) {
            if (winLen <= 0 || winLen > textLen) {
                continue;
            }
            for (int i = 0; i + winLen <= textLen; i++) {
                String window = text.substring(i, i + winLen);
                if (levenshtein(window, word) <= threshold) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Levenshtein 编辑距离（自实现，避免额外依赖）。
     */
    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /**
     * 从 DB 加载指定租户启用词（status='0'）。
     */
    private List<SensitiveWord> loadFromDb(Long tenantId) {
        LambdaQueryWrapper<SensitiveWord> wrapper = Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, tenantId)
            .eq(SensitiveWord::getStatus, "0");
        List<SensitiveWord> list = wordMapper.selectList(wrapper);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 写命中日志（PASS/FAIL 均写；PASS 时 hitWord 为 NULL，仅记调用事实）。
     */
    private void writeHitLog(String text, Long tenantId, String inputChannel, String bizType, String bizId,
                             SensitiveWordCheckResult.HitItem hit) {
        SensitiveWordHitLog entity = new SensitiveWordHitLog();
        entity.setTenantId(tenantId);
        try {
            entity.setUserId(LoginHelper.getUserId());
        } catch (Exception ignore) {
            // 异步/无登录上下文场景，userId 允许为 null
        }
        entity.setInputChannel(inputChannel);
        entity.setResult(hit != null ? SensitiveWordCheckResult.FAIL : SensitiveWordCheckResult.PASS);
        if (hit != null) {
            entity.setHitWord(hit.getWord());
            entity.setHitCategory(hit.getCategory());
            entity.setHitPosition(hit.getPosition());
        }
        entity.setOriginalText(StrUtil.sub(text, 0, ORIGINAL_TEXT_MAX));
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        hitLogWriter.writeAsync(entity);
    }
}
