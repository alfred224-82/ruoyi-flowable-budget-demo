package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 自助预约提交业务对象（T21.1/T21.3，小微版）
 * <p>顾客小程序「自助预约」页面提交：选主资源类型（project/room/staff）+ 主资源ID +
 * 可选关联助教/房间 + 预约日期 + 时段。
 * <p>★复用 booking_order 表写入，不新增业务表。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
public class SelfBookingSubmitBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预约类型：project=预约项目 / room=预约房间 / staff=预约助教
     */
    @NotBlank(message = "预约类型不能为空", groups = {AddGroup.class})
    private String bookingType;

    /**
     * 主资源ID（项目/房间=service_item.id，助教=sys_user.user_id）
     */
    @NotNull(message = "主资源不能为空", groups = {AddGroup.class})
    private Long mainResourceId;

    /**
     * 可选关联项目ID列表（非必选，数组；预约助教/房间时可选关联项目）
     */
    private List<Long> optionalProjectIds;

    /**
     * 可选关联助教ID列表（非必选，数组）
     */
    private List<Long> optionalStaffIds;

    /**
     * 可选关联房间ID列表（非必选，数组）
     */
    private List<Long> optionalRoomIds;

    /**
     * 预约日期（yyyy-MM-dd）
     */
    @NotNull(message = "预约日期不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date bookingDate;

    /**
     * 预约时段（如 09:00-11:00 / 14:00-16:00）
     */
    private String bookingTimeSlot;

    /**
     * 服务时长（小时，= 选中时段数；缺省 1）
     */
    private Integer quantity;

    /**
     * 预约人数
     */
    private Integer peopleCount;

    /**
     * 顾客姓名
     */
    private String customerName;

    /**
     * 顾客手机号
     */
    private String customerPhone;

    /**
     * 备注
     */
    private String remark;
}
