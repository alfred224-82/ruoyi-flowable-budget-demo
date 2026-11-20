package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 顾客移动自己座位业务对象（T28.3，小微版 M28）
 * <p>顾客仅可移动【自己】的座位到另一个空闲座位。
 * <p>★服务端校验：活动未锁定 / from 座位占座人 == 当前登录用户 / 目标座位空闲。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatMoveMineBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 当前座位ID（本人所占，FK→room_seat.id） */
    @NotNull(message = "当前座位ID不能为空", groups = {AddGroup.class})
    private Long fromSeatId;

    /** 目标座位ID（FK→room_seat.id，必须空闲） */
    @NotNull(message = "目标座位ID不能为空", groups = {AddGroup.class})
    private Long toSeatId;

    /** 目标座位号（前端传入，服务端冗余写入） */
    private String toSeatNo;
}
