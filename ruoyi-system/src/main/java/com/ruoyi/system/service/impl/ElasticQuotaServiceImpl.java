package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.system.domain.FeeElasticQuotaConfig;
import com.ruoyi.system.mapper.FeeElasticQuotaConfigMapper;
import com.ruoyi.system.service.IElasticQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 弹性额度Service实现（T6.1 / FEAT_ELASTIC_QUOTA）
 * 部门大类超支但在弹性阈值内 → 预警放行，超出才拦截（见 BudgetCheckServiceImpl 集成）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticQuotaServiceImpl implements IElasticQuotaService {

    private final FeeElasticQuotaConfigMapper configMapper;
    private final FeatureChecker featureChecker;

    private static final String FEATURE_CODE = "FEAT_ELASTIC_QUOTA";

    @Override
    public List<FeeElasticQuotaConfig> listConfigs() {
        return configMapper.selectList(Wrappers.<FeeElasticQuotaConfig>lambdaQuery()
            .orderByDesc(FeeElasticQuotaConfig::getCategoryId));
    }

    @Override
    public boolean saveConfig(FeeElasticQuotaConfig config) {
        validate(config);
        return configMapper.insert(config) > 0;
    }

    @Override
    public boolean updateConfig(FeeElasticQuotaConfig config) {
        validate(config);
        return configMapper.updateById(config) > 0;
    }

    @Override
    public boolean deleteConfig(Long id) {
        return configMapper.deleteById(id) > 0;
    }

    @Override
    public BigDecimal getThresholdPct(Long categoryId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE)) {
            return null;
        }
        List<FeeElasticQuotaConfig> configs = configMapper.selectList(Wrappers.<FeeElasticQuotaConfig>lambdaQuery()
            .eq(FeeElasticQuotaConfig::getIsEnabled, 1));
        if (configs.isEmpty()) {
            return null;
        }
        // 优先大类级配置，回退全局配置（categoryId 为空）
        FeeElasticQuotaConfig matched = null;
        for (FeeElasticQuotaConfig config : configs) {
            if (categoryId != null && categoryId.equals(config.getCategoryId())) {
                matched = config;
                break;
            }
            if (config.getCategoryId() == null && matched == null) {
                matched = config;
            }
        }
        return matched != null ? matched.getThresholdPct() : null;
    }

    private void validate(FeeElasticQuotaConfig config) {
        if (config.getThresholdPct() == null || config.getThresholdPct().compareTo(BigDecimal.ZERO) < 0) {
            throw new com.ruoyi.common.exception.ServiceException("弹性阈值百分比必须为非负数");
        }
        if (config.getIsEnabled() == null) {
            config.setIsEnabled(1);
        }
    }
}
