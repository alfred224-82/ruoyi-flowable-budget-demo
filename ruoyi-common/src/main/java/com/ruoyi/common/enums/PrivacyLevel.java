package com.ruoyi.common.enums;

/**
 * 隐私分级枚举（T31.5，设计方案 §四 隐私分级原则）
 * <p>四级分类：
 * <ul>
 *   <li>L1_STORE — 门店信息（名称/地址/客服电话微信），全部顾客可见，默认公开</li>
 *   <li>L2_BUSINESS — 业务信息（活动/座位占用/主持人姓名），相关顾客可见，系统规则控制</li>
 *   <li>L3_PERSONAL_CONTACT — 个人联系方式（员工手机/微信），默认不可见，本人/老板开关控制</li>
 *   <li>L4_CUSTOMER_INFO — 顾客个人信息（顾客手机/姓名/头像），仅员工老板可见，系统规则控制</li>
 * </ul>
 * <p>三原则：默认收紧、可控释放、最小够用。
 * <p>两条配套规则：
 * ① 隐藏必须配套补偿（选座页隐藏姓名 → 分享座位表补偿）
 * ② 隐私与可用不冲突（员工开关全关 → 顾客仍有门店联系方式）
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public enum PrivacyLevel {

    /** L1 门店信息：名称/地址/客服电话微信，全部顾客可见 */
    L1_STORE("L1", "门店信息"),

    /** L2 业务信息：活动/座位占用/主持人姓名，相关顾客可见 */
    L2_BUSINESS("L2", "业务信息"),

    /** L3 个人联系方式：员工手机/微信，默认不可见，由本人/老板开关控制 */
    L3_PERSONAL_CONTACT("L3", "个人联系方式"),

    /** L4 顾客个人信息：顾客手机/姓名/头像，仅员工与老板可见 */
    L4_CUSTOMER_INFO("L4", "顾客个人信息");

    private final String code;
    private final String info;

    PrivacyLevel(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    /**
     * 按 code 解析枚举（未命中返回 null）
     */
    public static PrivacyLevel fromCode(String code) {
        if (code == null) return null;
        for (PrivacyLevel level : values()) {
            if (level.code.equals(code)) return level;
        }
        return null;
    }
}
