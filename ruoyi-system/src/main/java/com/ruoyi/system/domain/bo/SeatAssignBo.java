package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Boss 加人到座位业务对象（T28.2，小微版 M28）
 * <p>Boss 将指定人员指派到活动的某个空闲座位（assignment_type=BOSS_ASSIGN）。
 * <p>★服务端校验：活动未锁定 / 目标座位空闲 / 该人员未在本活动重复占座。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatAssignBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 目标座位ID（FK→room_seat.id） */
    @NotNull(message = "座位ID不能为空", groups = {AddGroup.class})
    private Long seatId;

    /** 座位号（前端传入，服务端冗余写入，便于查询与展示） */
    private String seatNo;

    /** 被指派人员 user_id（FK→sys_user.user_id） */
    @NotNull(message = "被指派人员不能为空", groups = {AddGroup.class})
    private Long userId;

    /** 备注 */
    private String remark;
}
