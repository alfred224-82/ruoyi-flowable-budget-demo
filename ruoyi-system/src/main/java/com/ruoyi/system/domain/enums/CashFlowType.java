package com.ruoyi.system.domain.enums;

/**
 * 收支流水类型枚举 cash_flow.flow_type（T15.5/T15.14，小微版）
 * <p>纯记账口径（非支付流水）：income(收入)/expense(支出)。
 * 关联单据类型 related_type：booking(预约收款)/settlement(结算发放)/manual(手动补录)。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public enum CashFlowType {

    /** 收入（订单收款/手动补录收入） */
    INCOME("income", "收入"),

    /** 支出（工资结算/房租/水电/采购/手动补录支出） */
    EXPENSE("expense", "支出");

    /** 关联单据类型：预约订单收款 */
    public static final String RELATED_BOOKING = "booking";
    /** 关联单据类型：人员结算发放 */
    public static final String RELATED_SETTLEMENT = "settlement";
    /** 关联单据类型：手动补录 */
    public static final String RELATED_MANUAL = "manual";

    private final String code;
    private final String label;

    CashFlowType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (CashFlowType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}
