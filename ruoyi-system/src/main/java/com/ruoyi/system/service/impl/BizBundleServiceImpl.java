package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.BizBundle;
import com.ruoyi.system.domain.vo.BizBundleVo;
import com.ruoyi.system.mapper.BizBundleMapper;
import com.ruoyi.system.service.IBizBundleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业态包服务实现（T10.2 多业态平台底座）
 * <p>本地缓存业态包与解析后的开关/文案集合，保存或显式失效时清缓存。
 * 系统级共享表 biz_bundle 无 tenant_id，缓存 key 为 bizCode。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizBundleServiceImpl implements IBizBundleService {

    private final BizBundleMapper bizBundleMapper;
    private final ObjectMapper objectMapper;

    /** 默认业态包编码（存量租户回填值，见 M1_06） */
    public static final String DEFAULT_BIZ_CODE = "fund_control";

    /** 业态包缓存：bizCode → 实体（null 值用占位避免穿透） */
    private final ConcurrentHashMap<String, BizBundle> bundleCache = new ConcurrentHashMap<>();
    /** 开关集合缓存：bizCode → feature_flags 解析结果 */
    private final ConcurrentHashMap<String, Set<String>> flagCache = new ConcurrentHashMap<>();
    /** 文案缓存：bizCode → copy 解析结果 */
    private final ConcurrentHashMap<String, Map<String, String>> copyCache = new ConcurrentHashMap<>();
    /** 菜单声明缓存（T2.2）：bizCode → menus 解析后的 menu_id 集合 */
    private final ConcurrentHashMap<String, Set<Long>> menuCache = new ConcurrentHashMap<>();
    /** Schema 区块缓存（T10.2）：bizCode → schema_blocks 解析结果 */
    private final ConcurrentHashMap<String, List<Map<String, Object>>> schemaCache = new ConcurrentHashMap<>();
    /** 引擎策略缓存（T10.2）：bizCode → engine_strategy */
    private final ConcurrentHashMap<String, String> strategyCache = new ConcurrentHashMap<>();

    @Override
    public BizBundle getBundle(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        BizBundle cached = bundleCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = bizBundleMapper.selectOne(
            Wrappers.<BizBundle>lambdaQuery().eq(BizBundle::getBizCode, code));
        if (bundle != null) {
            bundleCache.put(code, bundle);
        } else {
            log.warn("[BizBundle] 业态包 {} 不存在，三维能力计算将跳过业态过滤", code);
        }
        return bundle;
    }

    @Override
    public Set<String> getFeatureFlags(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        Set<String> cached = flagCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = getBundle(code);
        Set<String> flags = bundle == null ? Collections.emptySet() : parseStringSet(bundle.getFeatureFlags());
        flagCache.put(code, flags);
        return flags;
    }

    @Override
    public Map<String, String> getCopy(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        Map<String, String> cached = copyCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = getBundle(code);
        Map<String, String> copy = bundle == null ? Collections.emptyMap() : parseStringMap(bundle.getCopy());
        copyCache.put(code, copy);
        return copy;
    }

    /**
     * T2.2 业态菜单过滤：解析 biz_bundle.menus（JSON 数组，元素为 menu_id）
     * <p>返回空集表示未声明 menus，调用方据此跳过业态过滤。
     */
    @Override
    public Set<Long> getMenuIds(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        Set<Long> cached = menuCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = getBundle(code);
        Set<Long> ids = bundle == null ? Collections.emptySet() : parseLongSet(bundle.getMenus());
        menuCache.put(code, ids);
        return ids;
    }

    /**
     * T10.2 Schema 页面区块声明：解析 biz_bundle.schema_blocks（JSON 数组）
     */
    @Override
    public List<Map<String, Object>> getSchemaBlocks(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        List<Map<String, Object>> cached = schemaCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = getBundle(code);
        List<Map<String, Object>> blocks = bundle == null ? Collections.emptyList() : parseBlockList(bundle.getSchemaBlocks());
        schemaCache.put(code, blocks);
        return blocks;
    }

    /**
     * T10.2 引擎策略 Bean 名：直接读取 biz_bundle.engine_strategy 字符串列
     */
    @Override
    public String getEngineStrategy(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        String cached = strategyCache.get(code);
        if (cached != null) {
            return cached;
        }
        BizBundle bundle = getBundle(code);
        String strategy = bundle == null || bundle.getEngineStrategy() == null ? "" : bundle.getEngineStrategy().trim();
        strategyCache.put(code, strategy);
        return strategy;
    }

    @Override
    public List<BizBundleVo> listBundles() {
        return bizBundleMapper.selectVoList(
            Wrappers.<BizBundle>lambdaQuery().orderByAsc(BizBundle::getBizCode));
    }

    @Override
    public int saveBundle(BizBundleVo vo) {
        if (vo == null || StrUtil.isBlank(vo.getBizCode())) {
            throw new IllegalArgumentException("业态包编码不能为空");
        }
        BizBundle entity = BeanCopyUtils.copy(vo, BizBundle.class);
        BizBundle exist = bizBundleMapper.selectOne(
            Wrappers.<BizBundle>lambdaQuery().eq(BizBundle::getBizCode, vo.getBizCode()));
        int rows;
        if (exist == null) {
            rows = bizBundleMapper.insert(entity);
        } else {
            rows = bizBundleMapper.updateById(entity);
        }
        evictCache(vo.getBizCode());
        return rows;
    }

    @Override
    public void evictCache(String bizCode) {
        if (StrUtil.isBlank(bizCode)) {
            bundleCache.clear();
            flagCache.clear();
            copyCache.clear();
            menuCache.clear();
            schemaCache.clear();
            strategyCache.clear();
            return;
        }
        bundleCache.remove(bizCode);
        flagCache.remove(bizCode);
        copyCache.remove(bizCode);
        menuCache.remove(bizCode);
        schemaCache.remove(bizCode);
        strategyCache.remove(bizCode);
    }

    private Set<String> parseStringSet(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptySet();
        }
        try {
            Set<String> set = objectMapper.readValue(json, new TypeReference<Set<String>>() {});
            return set != null ? set : Collections.emptySet();
        } catch (Exception e) {
            log.warn("[BizBundle] feature_flags JSON 解析失败: {}", json, e);
            return Collections.emptySet();
        }
    }

    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("[BizBundle] copy JSON 解析失败: {}", json, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 解析 menu_id JSON 数组（T2.2）：兼容字符串与数字两种元素形式
     */
    private Set<Long> parseLongSet(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptySet();
        }
        try {
            List<Object> raw = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
            if (raw == null || raw.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Long> ids = new java.util.HashSet<>(raw.size());
            for (Object o : raw) {
                if (o == null) {
                    continue;
                }
                try {
                    ids.add(Long.valueOf(o.toString().trim()));
                } catch (NumberFormatException nfe) {
                    log.warn("[BizBundle] menus 元素非数字，已忽略: {}", o);
                }
            }
            return Collections.unmodifiableSet(ids);
        } catch (Exception e) {
            log.warn("[BizBundle] menus JSON 解析失败: {}", json, e);
            return Collections.emptySet();
        }
    }

    /**
     * 解析 schema_blocks JSON 数组（T10.2）：元素为对象，保留完整结构供前端渲染引擎使用
     */
    private List<Map<String, Object>> parseBlockList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[BizBundle] schema_blocks JSON 解析失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
