package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ActivitySeatOperationLog;
import com.ruoyi.system.domain.vo.SeatOperationLogVo;

/**
 * 活动座位操作日志 Mapper（T26.8 / T28.4，小微版 M26 / M28）
 * <p>BE 使用 {@code @Aspect} 切面自动记录（M28 T28.4），避免每个 Service 方法手写日志代码。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface ActivitySeatOperationLogMapper extends BaseMapperPlus<ActivitySeatOperationLogMapper, ActivitySeatOperationLog, SeatOperationLogVo> {
}
