package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 活动座位占用 activity_seat_assignment（T26.8 / T28.1，小微版 M26 / M28）
 * <p>UNIQUE(activity_id, seat_id) 保证一座位一人；seat_id 允许 NULL 表示预留未指派。
 * <p>★assignment_type 值域：
 * <ul>
 *   <li>PRESET=预置人员（导游/司机，由 room_seat.is_preset=1 标记，橙色渲染）</li>
 *   <li>BOSS_ASSIGN=Boss 指派（M28 T28.2）</li>
 *   <li>CUSTOMER_PICK=顾客自选（T26.7）</li>
 * </ul>
 * <p>★locked 字段活动锁定后=1，禁止改动（联动 M28 T28.1）；顾客端选座接口锁定后拒绝。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_seat_assignment")
public class ActivitySeatAssignment extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 占座类型：预置人员 */
    public static final String TYPE_PRESET = "PRESET";
    /** 占座类型：Boss 指派 */
    public static final String TYPE_BOSS_ASSIGN = "BOSS_ASSIGN";
    /** 占座类型：顾客自选 */
    public static final String TYPE_CUSTOMER_PICK = "CUSTOMER_PICK";

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID（FK→promotion_activity.id）
     */
    private Long activityId;

    /**
     * 座位ID（FK→room_seat.id；NULL=预留未指派）
     */
    private Long seatId;

    /**
     * 座位号冗余（便于查询与前端展示，与 room_seat.seat_no 一致）
     */
    private String seatNo;

    /**
     * 占座人 user_id（FK→sys_user.user_id；NULL=预留但未指派）
     */
    private Long userId;

    /**
     * PRESET=预置人员 / BOSS_ASSIGN=Boss 指派 / CUSTOMER_PICK=顾客自选
     */
    private String assignmentType;

    /**
     * 占座时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date assignTime;

    /**
     * 活动锁定后=1，禁止改动（M28 T28.1）
     */
    private Integer locked;

    /**
     * 备注
     */
    private String remark;
}
