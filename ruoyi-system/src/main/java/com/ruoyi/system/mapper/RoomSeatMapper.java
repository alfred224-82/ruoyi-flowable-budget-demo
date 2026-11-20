package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.RoomSeat;
import com.ruoyi.system.domain.vo.RoomSeatVo;

/**
 * 房间座位点 Mapper（T26.1，小微版 M26）
 * <p>UNIQUE(layout_id, seat_no) 兜底同布局下座位号唯一。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public interface RoomSeatMapper extends BaseMapperPlus<RoomSeatMapper, RoomSeat, RoomSeatVo> {
}
