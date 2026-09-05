package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeatureFlagVo;

import java.util.List;
import java.util.Set;

/**
 * 功能开关服务（T10.3 表驱动开关）
 * <p>开关从 {@code EditionFeatureConfig} 静态集合迁到 feature_flag 表；表为准、常量为兜底
 * （表缺失时启用常量并告警）。platform_admin 可启停开关，但不可改版本归属以免串版本。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IFeatureFlagService {

    /**
     * 获取指定版本的默认开启开关集（表驱动 FEAT_* ∪ 常量兜底 legacy FEE_* / BUDGET_*）
     *
     * @param edition 版本编码（大小写/长短码均可，内部归一化）
     * @return 该版本默认开启的开关编码集合
     */
    Set<String> getEditionFeatureSet(String edition);

    /**
     * 全部开关列表（管理页面用）
     */
    List<FeatureFlagVo> listFlags();

    /**
     * 启停开关（platform_admin；仅改 status，不改版本归属）
     *
     * @param id     开关ID
     * @param status 目标状态（0启用 1停用）
     */
    int toggleStatus(Long id, String status);

    /**
     * 失效开关缓存
     */
    void evictCache();
}
