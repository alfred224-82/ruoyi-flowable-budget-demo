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
 * 活动座位操作日志 activity_seat_operation_log（T26.8 / T28.4，小微版 M26 / M28）
 * <p>所有编排动作留痕：ASSIGN / MOVE / SWAP / LOCK / UNLOCK / COPY_LAYOUT / PRESET / RELEASE。
 * <p>★before_snapshot / after_snapshot 为 JSON 字符串，可回放操作前后状态。
 * <p>★BE 使用 {@code @Aspect} 切面自动记录（M28 T28.4），避免每个 Service 方法手写日志代码。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_seat_operation_log")
public class ActivitySeatOperationLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 操作类型：加人到座位 */
    public static final String OP_ASSIGN = "ASSIGN";
    /** 操作类型：移动座位 */
    public static final String OP_MOVE = "MOVE";
    /** 操作类型：两人互换 */
    public static final String OP_SWAP = "SWAP";
    /** 操作类型：锁定活动 */
    public static final String OP_LOCK = "LOCK";
    /** 操作类型：解锁活动 */
    public static final String OP_UNLOCK = "UNLOCK";
    /** 操作类型：复制布局 */
    public static final String OP_COPY_LAYOUT = "COPY_LAYOUT";
    /** 操作类型：预置人员 */
    public static final String OP_PRESET = "PRESET";
    /** 操作类型：释放座位（顾客取消 / Boss 移除） */
    public static final String OP_RELEASE = "RELEASE";

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
     * ASSIGN/MOVE/SWAP/LOCK/UNLOCK/COPY_LAYOUT/PRESET/RELEASE
     */
    private String operationType;

    /**
     * 操作人 user_id（FK→sys_user.user_id）
     */
    private Long operatorUserId;

    /**
     * 操作人角色：boss/salesman/customer
     */
    private String operatorRole;

    /**
     * 操作前快照 JSON（含 seat_id / user_id / assignment_type）
     */
    private String beforeSnapshot;

    /**
     * 操作后快照 JSON
     */
    private String afterSnapshot;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    /**
     * 备注（如解锁原因）
     */
    private String remark;
}
