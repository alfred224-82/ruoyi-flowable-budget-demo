package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.TenantBizConfig;
import com.ruoyi.system.domain.vo.TenantBizConfigVo;

/**
 * 租户业态单店覆盖Mapper接口（T1.7 / T10.4）
 * 租户级表，含 tenant_id，由 TenantLineInnerInterceptor 自动隔离
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface TenantBizConfigMapper extends BaseMapperPlus<TenantBizConfigMapper, TenantBizConfig, TenantBizConfigVo> {
}
