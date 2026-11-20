package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 顾客选座业务对象（T26.7，小微版 M26）
 * <p>顾客小程序按位置选座：活动ID + 座位ID。
 * <p>★服务端校验：活动未锁定 / 座位空闲 / 当前用户未重复占座。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class SeatPickBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 座位ID（FK→room_seat.id） */
    @NotNull(message = "座位ID不能为空", groups = {AddGroup.class})
    private Long seatId;

    /** 座位号（前端传入，服务端冗余写入，便于查询与展示） */
    private String seatNo;

    /** 备注 */
    private String remark;
}
