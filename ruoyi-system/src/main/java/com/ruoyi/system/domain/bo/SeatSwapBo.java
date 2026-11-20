package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Boss 两人互换座位业务对象（T28.2，小微版 M28）
 * <p>Boss 将两条已占座记录的座位互换（含预置人员）。
 * <p>★服务端校验：活动未锁定 / 两条占座记录均属于本活动 / 两条记录不是同一条。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatSwapBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 占座记录A（FK→activity_seat_assignment.id） */
    @NotNull(message = "占座记录A不能为空", groups = {AddGroup.class})
    private Long assignmentIdA;

    /** 占座记录B（FK→activity_seat_assignment.id） */
    @NotNull(message = "占座记录B不能为空", groups = {AddGroup.class})
    private Long assignmentIdB;

    /** 备注 */
    private String remark;
}
