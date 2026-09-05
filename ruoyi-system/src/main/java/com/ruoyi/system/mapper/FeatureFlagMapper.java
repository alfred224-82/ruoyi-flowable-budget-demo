package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.FeatureFlag;
import com.ruoyi.system.domain.vo.FeatureFlagVo;

/**
 * 功能开关Mapper接口（T1.7 / T10.3 表驱动）
 * 系统级共享表，已加入 IGNORE_TENANT_TABLES，不做租户隔离
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface FeatureFlagMapper extends BaseMapperPlus<FeatureFlagMapper, FeatureFlag, FeatureFlagVo> {
}
