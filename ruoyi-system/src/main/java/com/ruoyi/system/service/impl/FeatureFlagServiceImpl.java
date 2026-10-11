package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.system.domain.FeatureFlag;
import com.ruoyi.system.domain.vo.FeatureFlagVo;
import com.ruoyi.system.mapper.FeatureFlagMapper;
import com.ruoyi.system.service.IFeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 功能开关服务实现（T10.3 表驱动开关）
 * <p><b>表驱动为唯一真源</b>（v1.28 T10.3 缺口封闭）：feature_flag 表为权威数据，
 * {@link EditionFeatureConfig} 常量仅作为初始化参考与表未就绪时的兑底：
 * <ul>
 *   <li>表存在数据：<b>以表为准</b>取该版本对应列（basic/advanced/pro_enabled=1）且 status=0 的 FEAT_* 开关，
 *       并【透传】常量中的 legacy 开关（FEE_* / BUDGET_* 等非 FEAT_ 前缀），保证旧能力不因表驱动而丢失；
 *       同时比对表驱动 FEAT_* 集合与常量 FEAT_* 集合，<b>不一致时告警</b>（表多/表少都记），
 *       便于 platform_admin 及时回写或修正常量；</li>
 *   <li>表为空（未初始化/迁移中）：回退整套常量集合并告警，系统仍按静态定义运行。</li>
 * </ul>
 * 全量开关本地缓存一份，启停或显式失效时清缓存。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagServiceImpl implements IFeatureFlagService {

    private final FeatureFlagMapper featureFlagMapper;

    /** 全量启用开关缓存（status=0）；null 表示未加载 */
    private final AtomicReference<List<FeatureFlag>> flagsCache = new AtomicReference<>();

    @Override
    public Set<String> getEditionFeatureSet(String edition) {
        String std = EditionFeatureConfig.normalizeEdition(edition);
        Set<String> constantSet = EditionFeatureConfig.getFeaturesByEdition(std);
    
        List<FeatureFlag> flags = loadFlags();
        if (flags.isEmpty()) {
            // 表未初始化：回退常量兑底并告警，避免能力全部丢失
            log.warn("[FeatureFlag] feature_flag 表无启用数据，版本 {} 回退 EditionFeatureConfig 常量兑底", std);
            return constantSet;
        }
    
        // 常量中的 legacy 开关（非 FEAT_ 前缀）透传，保证历史能力不受表驱动影响
        Set<String> result = new HashSet<>();
        Set<String> constantFeatOnly = new HashSet<>();
        for (String code : constantSet) {
            if (!isFeatCode(code)) {
                result.add(code);
            } else {
                constantFeatOnly.add(code);
            }
        }
        // 表驱动的 FEAT_* 开关：以表为准，按版本对应列取值
        Set<String> tableFeatOnly = new HashSet<>();
        for (FeatureFlag flag : flags) {
            if (StrUtil.isBlank(flag.getCode())) {
                continue;
            }
            if (enabledForEdition(flag, std)) {
                result.add(flag.getCode());
                if (isFeatCode(flag.getCode())) {
                    tableFeatOnly.add(flag.getCode());
                }
            }
        }
        // T10.3 告警：表与常量 FEAT_* 集合不一致时以表为准并记录差异
        warnIfDrift(std, tableFeatOnly, constantFeatOnly);
        return Collections.unmodifiableSet(result);
    }
    
    /**
     * T10.3 表驱动告警：比对表与常量的 FEAT_* 集合，不一致时输出告警日志
     * <p>不抛异常、不阻断业务，仅供 platform_admin 及时回写常量或修正表数据。
     */
    private void warnIfDrift(String stdEdition, Set<String> tableFeatOnly, Set<String> constantFeatOnly) {
        if (tableFeatOnly.equals(constantFeatOnly)) {
            return;
        }
        Set<String> onlyInTable = new HashSet<>(tableFeatOnly);
        onlyInTable.removeAll(constantFeatOnly);
        Set<String> onlyInConstant = new HashSet<>(constantFeatOnly);
        onlyInConstant.removeAll(tableFeatOnly);
        log.warn("[FeatureFlag] 表与常量不一致（版本={}），已以表为准。表多出={}，常量多出={}。"
                + "请确认是否需同步回写 EditionFeatureConfig 常量或修正 feature_flag 表。",
            stdEdition, onlyInTable, onlyInConstant);
    }

    @Override
    public List<FeatureFlagVo> listFlags() {
        return featureFlagMapper.selectVoList(
            Wrappers.<FeatureFlag>lambdaQuery()
                .orderByAsc(FeatureFlag::getFlagGroup)
                .orderByAsc(FeatureFlag::getCode));
    }

    @Override
    public int toggleStatus(Long id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("开关ID不能为空");
        }
        FeatureFlag entity = new FeatureFlag();
        entity.setId(id);
        entity.setStatus(status);
        int rows = featureFlagMapper.updateById(entity);
        evictCache();
        return rows;
    }

    @Override
    public void evictCache() {
        flagsCache.set(null);
    }

    /**
     * 加载全部启用开关（status=0），带本地缓存
     */
    private List<FeatureFlag> loadFlags() {
        List<FeatureFlag> cached = flagsCache.get();
        if (cached != null) {
            return cached;
        }
        List<FeatureFlag> flags = featureFlagMapper.selectList(
            Wrappers.<FeatureFlag>lambdaQuery().eq(FeatureFlag::getStatus, "0"));
        if (flags == null) {
            flags = Collections.emptyList();
        }
        flagsCache.set(flags);
        return flags;
    }

    /**
     * 是否为设计文档 5.1 定义的新开关（FEAT_ 前缀）；legacy 开关（FEE_* / BUDGET_*）透传不参与表驱动
     */
    private boolean isFeatCode(String code) {
        return code != null && code.startsWith("FEAT_");
    }

    /**
     * 判断开关在指定（已归一化）版本下是否默认开启
     */
    private boolean enabledForEdition(FeatureFlag flag, String stdEdition) {
        Integer enabled;
        switch (stdEdition) {
            case EditionFeatureConfig.EDITION_PROFESSIONAL:
                enabled = flag.getProEnabled();
                break;
            case EditionFeatureConfig.EDITION_ADVANCED:
                enabled = flag.getAdvancedEnabled();
                break;
            case EditionFeatureConfig.EDITION_MICRO:
                enabled = flag.getMicroEnabled();
                break;
            case EditionFeatureConfig.EDITION_BASIC:
            default:
                enabled = flag.getBasicEnabled();
                break;
        }
        return enabled != null && enabled == 1;
    }
}
