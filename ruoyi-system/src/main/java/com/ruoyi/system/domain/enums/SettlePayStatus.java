package com.ruoyi.system.domain.enums;

/**
 * 人员结算发放状态枚举 staff_settlement.pay_status（T15.5/T15.13，小微版）
 * <p>★V1 零支付合规：仅「待发放/已线下发放」两态；结算只记账不打款，
 * 老板线下转账后点「标记已线下发放」→改状态 + 写 cash_flow 支出流水，【不调任何支付/转账/代付接口】。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public enum SettlePayStatus {

    /** 待发放（结算单生成后的初始态，只记账） */
    PENDING("PENDING", "待发放"),

    /** 已线下发放（老板线下转账后标记，写 cash_flow expense） */
    PAID_OFFLINE("PAID_OFFLINE", "已线下发放");

    private final String code;
    private final String label;

    SettlePayStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static SettlePayStatus of(String code) {
        if (code != null) {
            for (SettlePayStatus s : values()) {
                if (s.code.equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return PENDING;
    }
}
