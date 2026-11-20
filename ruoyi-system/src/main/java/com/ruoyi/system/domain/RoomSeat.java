package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 房间座位点 room_seat（T26.1，小微版 M26）
 * <p>坐标以背景图百分比 0~1 存储，跨端一致；UNIQUE(layout_id, seat_no) 保证同布局下座位号唯一。
 * <p>★预置人员（导游/司机）由 is_preset=1 标记，橙色渲染，不参与顾客选座（T26.7）。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_seat")
public class RoomSeat extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 预置角色：导游 */
    public static final String PRESET_GUIDE = "guide";
    /** 预置角色：司机 */
    public static final String PRESET_DRIVER = "driver";
    /** 预置角色：助理 */
    public static final String PRESET_ASSISTANT = "assistant";

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属布局ID（FK→room_seat_layout.id） */
    private Long layoutId;

    /** 座位号（如 A1 / 1 / 前排-左1） */
    private String seatNo;

    /** X 坐标百分比 0~1 */
    private BigDecimal posXPercent;

    /** Y 坐标百分比 0~1 */
    private BigDecimal posYPercent;

    /** 是否预置人员（导游/司机）：0=否 1=是 */
    private Integer isPreset;

    /** 预置角色：guide/driver/assistant */
    private String presetRole;

    /** 预置人员 user_id */
    private Long presetUserId;

    /** 备注 */
    private String remark;
}
