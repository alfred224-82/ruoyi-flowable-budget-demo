package com.ruoyi.system.service;

import com.ruoyi.system.domain.FeeElasticQuotaConfig;

import java.math.BigDecimal;
import java.util.List;

/**
 * 弹性额度Service接口（T6.1 / FEAT_ELASTIC_QUOTA）
 * 部门大类超支但在弹性阈值内（如 +10%）→ 自动标记预警放行，超出才拦截
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IElasticQuotaService {

    /**
     * 弹性额度配置列表
     */
    List<FeeElasticQuotaConfig> listConfigs();

    /**
     * 新增弹性额度配置
     */
    boolean saveConfig(FeeElasticQuotaConfig config);

    /**
     * 修改弹性额度配置
     */
    boolean updateConfig(FeeElasticQuotaConfig config);

    /**
     * 删除弹性额度配置
     */
    boolean deleteConfig(Long id);

    /**
     * 获取生效的弹性阈值百分比（如 10 = 允许超支 +10%）
     * 开关关闭或无启用配置返回 null（不放行）；优先匹配大类级，回退全局配置
     *
     * @param categoryId 费用大类ID（可空）
     */
    BigDecimal getThresholdPct(Long categoryId);
}
