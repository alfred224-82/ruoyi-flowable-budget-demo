package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ActivitySeatAssignment;
import com.ruoyi.system.domain.vo.SeatAssignmentVo;

/**
 * 活动座位占用 Mapper（T26.8，小微版 M26）
 * <p>UNIQUE(activity_id, seat_id) 保证一座位一人；插入冲突由 Service 层捕获并转 ServiceException。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface ActivitySeatAssignmentMapper extends BaseMapperPlus<ActivitySeatAssignmentMapper, ActivitySeatAssignment, SeatAssignmentVo> {
}
