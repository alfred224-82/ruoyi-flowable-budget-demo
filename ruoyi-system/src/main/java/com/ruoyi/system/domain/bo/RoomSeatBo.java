package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 房间座位点业务对象（T26.1 / T26.4，小微版 M26）
 * <p>坐标以背景图百分比 0~1 存储，前端拖拽落位后换算并提交。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（新增座位为空；编辑/双击改编号时回填） */
    private Long id;

    /** 所属布局ID（Service 层批量注入，前端可不传） */
    private Long layoutId;

    /** 座位号（如 A1 / 1 / 前排-左1；UNIQUE(layout_id, seat_no)） */
    @NotBlank(message = "座位号不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = "座位号长度不能超过32")
    private String seatNo;

    /** X 坐标百分比 0~1 */
    @NotNull(message = "X 坐标不能为空", groups = {AddGroup.class, EditGroup.class})
    @DecimalMin(value = "0.0", message = "X 坐标百分比不能小于0")
    @DecimalMax(value = "1.0", message = "X 坐标百分比不能大于1")
    private BigDecimal posXPercent;

    /** Y 坐标百分比 0~1 */
    @NotNull(message = "Y 坐标不能为空", groups = {AddGroup.class, EditGroup.class})
    @DecimalMin(value = "0.0", message = "Y 坐标百分比不能小于0")
    @DecimalMax(value = "1.0", message = "Y 坐标百分比不能大于1")
    private BigDecimal posYPercent;

    /** 是否预置人员（导游/司机）：0=否 1=是；缺省 0 */
    private Integer isPreset;

    /** 预置角色：guide/driver/assistant（isPreset=1 时必填） */
    @Size(max = 20, message = "预置角色长度不能超过20")
    private String presetRole;

    /** 预置人员 user_id（isPreset=1 时可指定；空表示仅占位不指派） */
    private Long presetUserId;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
