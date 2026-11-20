package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.BizCopy;
import com.ruoyi.system.domain.vo.BizCopyVo;
import com.ruoyi.system.mapper.BizCopyMapper;
import com.ruoyi.system.service.IBizCopyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 业态文案映射服务实现（T10.5 v1.29 文案映射 i18n）
 * <p>本地缓存 key = bizCode + "|" + lang，value = Map&lt;i18n_key, copy_value&gt;；
 * 保存/删除时精确失效对应缓存项；bizCode/lang 为空时清全部缓存。
 * <p>三维合并优先级由调用方（前端 $bizT / Controller）按序尝试：
 * <ol>
 *   <li>租户 module_overrides.copy（TenantBizConfigServiceImpl#getMergedOverrides）</li>
 *   <li>本表 biz_copy(biz_code, lang)（{@link #getCopy}）</li>
 *   <li>biz_bundle.copy（{@link BizBundleServiceImpl#getCopy}，单语言 zh-CN 兜底）</li>
 *   <li>前端硬编码兜底</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizCopyServiceImpl implements IBizCopyService {

    private final BizCopyMapper bizCopyMapper;

    /** 默认业态包编码（与 BizBundleServiceImpl.DEFAULT_BIZ_CODE 对齐） */
    public static final String DEFAULT_BIZ_CODE = "fund_control";
    /** 默认语言编码 */
    public static final String DEFAULT_LANG = "zh-CN";

    /** 文案缓存：bizCode|lang → (i18n_key → copy_value) */
    private final ConcurrentHashMap<String, Map<String, String>> copyCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> getCopy(String bizCode, String lang) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        String language = StrUtil.isNotBlank(lang) ? lang : DEFAULT_LANG;
        String cacheKey = code + "|" + language;
        Map<String, String> cached = copyCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<BizCopy> rows = bizCopyMapper.selectList(
            Wrappers.<BizCopy>lambdaQuery()
                .eq(BizCopy::getBizCode, code)
                .eq(BizCopy::getLang, language)
                .eq(BizCopy::getStatus, "0"));
        Map<String, String> map = new LinkedHashMap<>();
        for (BizCopy row : rows) {
            if (StrUtil.isNotBlank(row.getI18nKey()) && row.getCopyValue() != null) {
                map.put(row.getI18nKey(), row.getCopyValue());
            }
        }
        Map<String, String> result = Collections.unmodifiableMap(map);
        copyCache.put(cacheKey, result);
        return result;
    }

    @Override
    public Map<String, Map<String, String>> getCopyAllLangs(String bizCode) {
        String code = StrUtil.isNotBlank(bizCode) ? bizCode : DEFAULT_BIZ_CODE;
        List<BizCopy> rows = bizCopyMapper.selectList(
            Wrappers.<BizCopy>lambdaQuery()
                .eq(BizCopy::getBizCode, code)
                .eq(BizCopy::getStatus, "0"));
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (BizCopy row : rows) {
            if (StrUtil.isBlank(row.getLang()) || StrUtil.isBlank(row.getI18nKey())) {
                continue;
            }
            result.computeIfAbsent(row.getLang(), k -> new LinkedHashMap<>())
                .put(row.getI18nKey(), row.getCopyValue());
        }
        // 二级 Map 也做不可变包装
        Map<String, Map<String, String>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> e : result.entrySet()) {
            immutable.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    @Override
    public List<String> listLangs() {
        List<BizCopy> rows = bizCopyMapper.selectList(
            Wrappers.<BizCopy>lambdaQuery().select(BizCopy::getLang).groupBy(BizCopy::getLang));
        Set<String> langs = rows.stream()
            .map(BizCopy::getLang)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new ArrayList<>(langs);
    }

    @Override
    public List<BizCopyVo> listCopies(BizCopyVo query) {
        var wrapper = Wrappers.<BizCopy>lambdaQuery();
        if (query != null) {
            if (StrUtil.isNotBlank(query.getBizCode())) {
                wrapper.eq(BizCopy::getBizCode, query.getBizCode());
            }
            if (StrUtil.isNotBlank(query.getLang())) {
                wrapper.eq(BizCopy::getLang, query.getLang());
            }
            if (StrUtil.isNotBlank(query.getI18nKey())) {
                wrapper.like(BizCopy::getI18nKey, query.getI18nKey());
            }
            if (StrUtil.isNotBlank(query.getModule())) {
                wrapper.eq(BizCopy::getModule, query.getModule());
            }
            if (StrUtil.isNotBlank(query.getStatus())) {
                wrapper.eq(BizCopy::getStatus, query.getStatus());
            }
        }
        wrapper.orderByAsc(BizCopy::getBizCode).orderByAsc(BizCopy::getLang).orderByAsc(BizCopy::getModule).orderByAsc(BizCopy::getI18nKey);
        return bizCopyMapper.selectVoList(wrapper);
    }

    @Override
    public int saveCopy(BizCopyVo vo) {
        if (vo == null || StrUtil.isBlank(vo.getBizCode()) || StrUtil.isBlank(vo.getI18nKey()) || StrUtil.isBlank(vo.getLang())) {
            throw new IllegalArgumentException("bizCode/i18nKey/lang 均不能为空");
        }
        if (StrUtil.isBlank(vo.getCopyValue())) {
            throw new IllegalArgumentException("copyValue 不能为空");
        }
        BizCopy exist = bizCopyMapper.selectOne(
            Wrappers.<BizCopy>lambdaQuery()
                .eq(BizCopy::getBizCode, vo.getBizCode())
                .eq(BizCopy::getI18nKey, vo.getI18nKey())
                .eq(BizCopy::getLang, vo.getLang())
                .last("LIMIT 1"));
        BizCopy entity = BeanCopyUtils.copy(vo, BizCopy.class);
        int rows;
        if (exist == null) {
            rows = bizCopyMapper.insert(entity);
        } else {
            entity.setId(exist.getId());
            rows = bizCopyMapper.updateById(entity);
        }
        evictCache(vo.getBizCode(), vo.getLang());
        return rows;
    }

    @Override
    public int batchSave(List<BizCopyVo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int total = 0;
        Set<String> touched = new java.util.HashSet<>();
        for (BizCopyVo vo : list) {
            total += saveCopy(vo);
            touched.add(vo.getBizCode() + "|" + vo.getLang());
        }
        // saveCopy 内部已逐条失效，此处无需重复清缓存
        log.info("[BizCopy] 批量保存 {} 条，涉及 {} 个 (bizCode,lang) 组合", total, touched.size());
        return total;
    }

    @Override
    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        BizCopy exist = bizCopyMapper.selectById(id);
        int rows = bizCopyMapper.deleteById(id);
        if (exist != null) {
            evictCache(exist.getBizCode(), exist.getLang());
        }
        return rows;
    }

    @Override
    public int deleteByBizAndLang(String bizCode, String lang) {
        if (StrUtil.isBlank(bizCode) || StrUtil.isBlank(lang)) {
            return 0;
        }
        int rows = bizCopyMapper.delete(
            Wrappers.<BizCopy>lambdaQuery()
                .eq(BizCopy::getBizCode, bizCode)
                .eq(BizCopy::getLang, lang));
        evictCache(bizCode, lang);
        return rows;
    }

    @Override
    public void evictCache(String bizCode, String lang) {
        if (StrUtil.isBlank(bizCode) && StrUtil.isBlank(lang)) {
            copyCache.clear();
            return;
        }
        if (StrUtil.isBlank(lang)) {
            // 清该 bizCode 全部语言
            copyCache.keySet().removeIf(k -> k.startsWith(bizCode + "|"));
            return;
        }
        if (StrUtil.isBlank(bizCode)) {
            // 清全部 bizCode 的该语言
            copyCache.keySet().removeIf(k -> k.endsWith("|" + lang));
            return;
        }
        copyCache.remove(bizCode + "|" + lang);
    }
}
