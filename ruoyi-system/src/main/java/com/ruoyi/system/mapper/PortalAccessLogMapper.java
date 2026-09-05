package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.PortalAccessLog;

/**
 * 引导态门户访问审计Mapper接口（T8.8）
 * <p>系统级共享表，已加入 IGNORE_TENANT_TABLES，不做租户隔离；仅追加写入。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface PortalAccessLogMapper extends BaseMapperPlus<PortalAccessLogMapper, PortalAccessLog, PortalAccessLog> {
}
