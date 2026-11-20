package com.ruoyi.system.domain.enums;

/**
 * 座位编排操作类型枚举（T28.4，小微版 M28）
 * <p>与 {@link com.ruoyi.system.domain.ActivitySeatOperationLog} 的 OP_* 字符串常量一一对应，
 * 用于 {@code @SeatOperation} 切面标注，避免每个 Service 方法手写日志代码。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public enum SeatOperationType {

    /** 加人到座位（Boss 指派 / 顾客自选） */
    ASSIGN("ASSIGN", "加人到座位"),
    /** 移动座位（含预置人员） */
    MOVE("MOVE", "移动座位"),
    /** 两人互换座位 */
    SWAP("SWAP", "互换座位"),
    /** 锁定活动 */
    LOCK("LOCK", "锁定活动"),
    /** 解锁活动 */
    UNLOCK("UNLOCK", "解锁活动"),
    /** 复制布局 */
    COPY_LAYOUT("COPY_LAYOUT", "复制布局"),
    /** 预置人员（导游/司机占座） */
    PRESET("PRESET", "预置人员"),
    /** 释放座位（顾客取消 / Boss 移除） */
    RELEASE("RELEASE", "释放座位");

    /** 落库编码（activity_seat_operation_log.operation_type） */
    private final String code;

    /** 中文描述 */
    private final String label;

    SeatOperationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
