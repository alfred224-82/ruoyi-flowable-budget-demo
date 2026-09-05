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
            return;
        }
        bundleCache.remove(bizCode);
        flagCache.remove(bizCode);
        copyCache.remove(bizCode);
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
}
