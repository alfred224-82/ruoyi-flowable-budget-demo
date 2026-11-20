package com.ruoyi.system.domain.enums;

/**
 * 预约订单业务状态枚举 booking_order.order_status（T15.5/T15.12，小微版）
 * <p>状态流转：待服务 → 已完成 / 已取消；与收款状态 {@link BookingPayStatus} 正交。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public enum BookingOrderStatus {

    /** 待服务（预约成功，等待到店） */
    PENDING_SERVICE("PENDING_SERVICE", "待服务"),

    /** 已完成（服务完成） */
    COMPLETED("COMPLETED", "已完成"),

    /** 已取消（顾客/老板取消预约） */
    CANCELLED("CANCELLED", "已取消"),

    /** 爽约（T15.31/T15.32）：预约过期未到店且未主动取消，由 noShowCheckJob/老板标记；★主动取消(CANCELLED)不算爽约 */
    NO_SHOW("NO_SHOW", "爽约");

    private final String code;
    private final String label;

    BookingOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BookingOrderStatus of(String code) {
        if (code != null) {
            for (BookingOrderStatus s : values()) {
                if (s.code.equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return PENDING_SERVICE;
    }
}
