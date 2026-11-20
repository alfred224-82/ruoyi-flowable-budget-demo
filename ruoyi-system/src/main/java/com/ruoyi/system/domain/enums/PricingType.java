package com.ruoyi.system.domain.enums;

/**
 * 服务项目计价方式枚举 service_item.pricing_type（T15.4/T15.11，小微版）
 * <p>通用模型支持所有行业：
 * <ul>
 *   <li>duration — 计时：单价 × 时长（会议室、场地、设备、人员按时）</li>
 *   <li>package — 套餐：基础价 + 超时费 + 超量费，双封顶（车辆全日租、场地半/全日套餐）</li>
 *   <li>times   — 计量：单价 × 数量（车辆按公里、货运按吨、按份）</li>
 *   <li>fixed   — 一口价：固定值，但约束仍要计时/计量（机场接送、婚礼摄影、安装服务）</li>
 *   <li>weight  — 按重量（蔬菜/肉菜：元/斤）</li>
 *   <li>person  — 按人（拼桌/课程：元/人）</li>
 *   <li>portion — 按份（餐食/套餐：元/份）</li>
 * </ul>
 * DB 默认 times；单位（unit）与本枚举解耦，unit 仅用于展示（小时/斤/节/人/次/份）。
 * 模式参数（含时长上限、超时单价等）统一存入 extra_config JSON 列。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public enum PricingType {

    /** 计时：单价 × 时长（会议室、场地、设备、人员按时） */
    DURATION("duration", "计时"),

    /** 套餐：基础价 + 超时费 + 超量费，双封顶（车辆全日租、场地半/全日套餐） */
    PACKAGE("package", "套餐"),

    /** 计量：单价 × 数量（车辆按公里、货运按吨、按份） */
    TIMES("times", "计量"),

    /** 一口价：固定值，但约束仍要计时/计量（机场接送、婚礼摄影、安装服务） */
    FIXED("fixed", "一口价"),

    /** 按重量计价（蔬菜/肉菜：元/斤） */
    WEIGHT("weight", "按重量"),

    /** 按人计价（拼桌/课程：元/人） */
    PERSON("person", "按人"),

    /** 按份计价（餐食/套餐：元/份） */
    PORTION("portion", "按份");

    private final String code;
    private final String label;

    PricingType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 按 code 解析枚举，非法值回落 TIMES（DB 默认计价方式）
     */
    public static PricingType of(String code) {
        if (code != null) {
            for (PricingType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
        }
        return TIMES;
    }

    /**
     * 校验 code 是否为合法计价方式
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (PricingType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}
