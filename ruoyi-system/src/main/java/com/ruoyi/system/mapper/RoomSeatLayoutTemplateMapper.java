package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.RoomSeatLayoutTemplate;
import com.ruoyi.system.domain.vo.RoomSeatLayoutTemplateVo;

/**
 * 房间座位布局模板 Mapper（T26.1 / T26.5 / T26.6，小微版 M26）
 * <p>UNIQUE(tenant_id, name) 兜底同租户下模板名唯一；平台默认模板 tenant_id=1。
 * <p>★Boss 查询模板列表时（本租户 + 平台默认）需通过 {@code @TenantIgnore} 显式声明例外，
 *   否则多租户插件会过滤掉 tenant_id=1 的平台默认模板。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public interface RoomSeatLayoutTemplateMapper extends BaseMapperPlus<RoomSeatLayoutTemplateMapper, RoomSeatLayoutTemplate, RoomSeatLayoutTemplateVo> {
}
