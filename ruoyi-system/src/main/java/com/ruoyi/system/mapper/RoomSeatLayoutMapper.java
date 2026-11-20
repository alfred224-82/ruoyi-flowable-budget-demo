package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.RoomSeatLayout;
import com.ruoyi.system.domain.vo.RoomSeatLayoutVo;

/**
 * 房间座位布局 Mapper（T26.1，小微版 M26）
 * <p>UNIQUE(tenant_id, room_id) 兜底一房一份；插入冲突由 Service 层捕获并转 ServiceException。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public interface RoomSeatLayoutMapper extends BaseMapperPlus<RoomSeatLayoutMapper, RoomSeatLayout, RoomSeatLayoutVo> {
}
