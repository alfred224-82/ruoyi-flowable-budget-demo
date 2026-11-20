package com.ruoyi.system.domain.enums;

/**
 * 预约订单收款状态枚举 booking_order.pay_status（T15.5/T15.12，小微版）
 * <p>★V1 零支付合规：仅「未收款/已收款」两态（全租户统一文案），【无「已支付」在线支付态】；
 * 收款线下完成，老板 PC 标记「已收款」时自动写 cash_flow 收入流水，全系统不调支付 SDK。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public enum BookingPayStatus {

    /** 未收款（顾客提交预约后的初始态，无在线支付；活动合并行恒为此态） */
    PENDING_PAY("PENDING_PAY", "未收款"),

    /** 已收款（老板线下收款后标记，写 cash_flow income） */
    RECEIVED("RECEIVED", "已收款");

    private final String code;
    private final String label;

    BookingPayStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BookingPayStatus of(String code) {
        if (code != null) {
            for (BookingPayStatus s : values()) {
                if (s.code.equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return PENDING_PAY;
    }
}
