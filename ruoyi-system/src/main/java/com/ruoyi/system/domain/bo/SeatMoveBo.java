package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Boss 移动座位业务对象（T28.2，小微版 M28）
 * <p>Boss 将某条占座记录（含预置人员）从当前座位移动到另一个空闲座位。
 * <p>★服务端校验：活动未锁定 / 占座记录属于本活动 / 目标座位空闲。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatMoveBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 待移动的占座记录ID（FK→activity_seat_assignment.id） */
    @NotNull(message = "占座记录ID不能为空", groups = {AddGroup.class})
    private Long assignmentId;

    /** 目标座位ID（FK→room_seat.id，必须空闲） */
    @NotNull(message = "目标座位ID不能为空", groups = {AddGroup.class})
    private Long toSeatId;

    /** 目标座位号（前端传入，服务端冗余写入） */
    private String toSeatNo;

    /** 备注 */
    private String remark;
}
