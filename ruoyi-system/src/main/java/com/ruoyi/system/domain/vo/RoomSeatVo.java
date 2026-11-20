package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 房间座位点视图对象（T26.1 / T26.4，小微版 M26）
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 所属布局ID */
    private Long layoutId;

    /** 座位号 */
    private String seatNo;

    /** X 坐标百分比 0~1 */
    private BigDecimal posXPercent;

    /** Y 坐标百分比 0~1 */
    private BigDecimal posYPercent;

    /** 是否预置人员：0=否 1=是 */
    private Integer isPreset;

    /** 预置角色：guide/driver/assistant */
    private String presetRole;

    /** 预置人员 user_id */
    private Long presetUserId;

    /** 预置人员昵称（回填，非表字段） */
    private String presetUserName;

    /** 备注 */
    private String remark;
}
