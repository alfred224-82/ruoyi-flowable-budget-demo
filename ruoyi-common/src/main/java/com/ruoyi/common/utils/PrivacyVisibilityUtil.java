package com.ruoyi.common.utils;

import com.ruoyi.common.enums.PrivacyLevel;

/**
 * 隐私可见性判断工具（T31.5，设计方案 §四 隐私分级原则）
 * <p>按「当前角色 + 目标角色 + 信息级别」返回可见/隐藏/脱敏三种结果。
 * <p>核心规则：
 * <ol>
 *   <li>顾客之间互相不可见（L4 顾客个人信息对顾客角色隐藏）</li>
 *   <li>不给自己打电话（当前登录者本人行跳过电话图标）</li>
 *   <li>员工联系方式按开关（staff_contact_public.show_phone / show_wechat，默认关）</li>
 *   <li>L1 门店信息对所有人可见</li>
 *   <li>L2 业务信息对相关顾客可见（系统规则控制）</li>
 * </ol>
 * <p>三原则：默认收紧、可控释放、最小够用。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public class PrivacyVisibilityUtil {

    /** 可见性结果枚举 */
    public enum Visibility {
        /** 完全可见 */
        VISIBLE,
        /** 隐藏（不显示） */
        HIDDEN,
        /** 脱敏显示（如手机号 138****4521） */
        DESENSITIZED
    }

    /** 角色常量（与前端 roles.js ROLES 对齐） */
    public static final String ROLE_BOSS = "boss";
    public static final String ROLE_SALESMAN = "salesman";
    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_PLATFORM_ADMIN = "platform_admin";

    private PrivacyVisibilityUtil() {
        // 工具类禁止实例化
    }

    /**
     * 判断信息可见性（通用入口）
     *
     * @param currentRole    当前登录者角色（boss/salesman/customer）
     * @param targetRole     目标用户角色（被查看者的角色）
     * @param level          信息隐私级别
     * @param isSelf         是否为本人（当前登录者查看自己的信息）
     * @param staffShowPhone 员工联系方式公开开关：show_phone（仅 targetRole=salesman 时有效）
     * @param staffShowWechat 员工联系方式公开开关：show_wechat（仅 targetRole=salesman 时有效）
     * @return 可见性结果
     */
    public static Visibility checkVisibility(String currentRole, String targetRole,
                                             PrivacyLevel level, boolean isSelf,
                                             boolean staffShowPhone, boolean staffShowWechat) {
        if (level == null || currentRole == null) {
            return Visibility.HIDDEN; // fail-safe：缺失参数默认隐藏
        }

        switch (level) {
            case L1_STORE:
                // 门店信息对所有人可见
                return Visibility.VISIBLE;

            case L2_BUSINESS:
                // 业务信息：老板/员工完全可见，顾客可见（座位占用只显示"已占"不显示姓名头像）
                if (ROLE_CUSTOMER.equals(currentRole)) {
                    return Visibility.DESENSITIZED;
                }
                return Visibility.VISIBLE;

            case L3_PERSONAL_CONTACT:
                // 员工联系方式：按开关控制
                return checkL3Contact(currentRole, targetRole, isSelf, staffShowPhone, staffShowWechat);

            case L4_CUSTOMER_INFO:
                // 顾客个人信息：仅老板与员工可见，顾客之间互相不可见
                return checkL4CustomerInfo(currentRole, targetRole, isSelf);

            default:
                return Visibility.HIDDEN;
        }
    }

    /**
     * 简化版：判断电话图标是否可见（前端最常用场景）
     * <p>规则：① 本人不显示 ② 顾客对顾客不显示 ③ 员工按开关 ④ 老板对所有人显示
     *
     * @param currentRole    当前登录者角色
     * @param targetRole     目标用户角色
     * @param isSelf         是否为本人
     * @param staffShowPhone 员工 show_phone 开关
     * @return true=显示电话图标
     */
    public static boolean isPhoneVisible(String currentRole, String targetRole,
                                         boolean isSelf, boolean staffShowPhone) {
        // 规则②：不给自己打电话
        if (isSelf) return false;
        // 老板对所有角色可见
        if (ROLE_BOSS.equals(currentRole) || ROLE_PLATFORM_ADMIN.equals(currentRole)) return true;
        // 员工（salesman）视角
        if (ROLE_SALESMAN.equals(currentRole)) {
            // 看顾客：可见（L4 对员工开放）
            if (ROLE_CUSTOMER.equals(targetRole)) return true;
            // 看同事/老板：可见
            return true;
        }
        // 顾客视角
        if (ROLE_CUSTOMER.equals(currentRole)) {
            // 看顾客：不可见（顾客之间互相不可见）
            if (ROLE_CUSTOMER.equals(targetRole)) return false;
            // 看员工：按开关
            if (ROLE_SALESMAN.equals(targetRole)) return staffShowPhone;
            // 看老板/门店：可见（L1 门店联系方式）
            return true;
        }
        return false;
    }

    /**
     * 判断微信号是否可见
     *
     * @param currentRole     当前登录者角色
     * @param targetRole      目标用户角色
     * @param isSelf          是否为本人
     * @param staffShowWechat 员工 show_wechat 开关
     * @return true=显示微信入口
     */
    public static boolean isWechatVisible(String currentRole, String targetRole,
                                          boolean isSelf, boolean staffShowWechat) {
        if (isSelf) return false;
        if (ROLE_BOSS.equals(currentRole) || ROLE_PLATFORM_ADMIN.equals(currentRole)) return true;
        if (ROLE_SALESMAN.equals(currentRole)) return true;
        if (ROLE_CUSTOMER.equals(currentRole)) {
            if (ROLE_CUSTOMER.equals(targetRole)) return false;
            if (ROLE_SALESMAN.equals(targetRole)) return staffShowWechat;
            return true;
        }
        return false;
    }

    /**
     * 手机号脱敏（前三后四）：13812345678 → 138****5678
     *
     * @param phone 完整手机号
     * @return 脱敏后的手机号；空值或长度不足返回原值
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 姓名脱敏（保留姓，名用 * 替代）：张伟 → 张*，欧阳修 → 欧阳*
     *
     * @param name 完整姓名
     * @return 脱敏后的姓名
     */
    public static String desensitizeName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        // 复姓（两字姓）判断
        if (name.length() >= 3 && isCompoundSurname(name.substring(0, 2))) {
            return name.substring(0, 2) + "*";
        }
        return name.substring(0, 1) + "*";
    }

    // ==================== 内部方法 ====================

    /**
     * L3 个人联系方式可见性判断
     */
    private static Visibility checkL3Contact(String currentRole, String targetRole,
                                             boolean isSelf, boolean showPhone, boolean showWechat) {
        // 本人始终可见自己的联系方式
        if (isSelf) return Visibility.VISIBLE;
        // 老板/平台管理员对所有员工联系方式可见
        if (ROLE_BOSS.equals(currentRole) || ROLE_PLATFORM_ADMIN.equals(currentRole)) {
            return Visibility.VISIBLE;
        }
        // 员工之间互相可见（同事协作需要）
        if (ROLE_SALESMAN.equals(currentRole) && ROLE_SALESMAN.equals(targetRole)) {
            return Visibility.VISIBLE;
        }
        // 顾客看员工：按开关
        if (ROLE_CUSTOMER.equals(currentRole) && ROLE_SALESMAN.equals(targetRole)) {
            return (showPhone || showWechat) ? Visibility.VISIBLE : Visibility.HIDDEN;
        }
        // 其他情况默认隐藏
        return Visibility.HIDDEN;
    }

    /**
     * L4 顾客个人信息可见性判断
     */
    private static Visibility checkL4CustomerInfo(String currentRole, String targetRole, boolean isSelf) {
        // 本人始终可见
        if (isSelf) return Visibility.VISIBLE;
        // 老板/员工对顾客信息可见
        if (ROLE_BOSS.equals(currentRole) || ROLE_SALESMAN.equals(currentRole)
            || ROLE_PLATFORM_ADMIN.equals(currentRole)) {
            return Visibility.VISIBLE;
        }
        // 顾客之间互相不可见（核心隐私规则）
        if (ROLE_CUSTOMER.equals(currentRole) && ROLE_CUSTOMER.equals(targetRole)) {
            return Visibility.HIDDEN;
        }
        // 顾客看老板/员工：可见（L1 门店信息层面）
        return Visibility.VISIBLE;
    }

    /** 常见复姓判断 */
    private static boolean isCompoundSurname(String twoChar) {
        // 简化判断：常见复姓列表
        String[] compoundSurnames = {"欧阳", "司马", "诸葛", "上官", "夏侯", "皇甫",
            "尉迟", "公孙", "令狐", "长孙", "慕容", "司徒", "司空", "东方", "独孤", "南宫"};
        for (String s : compoundSurnames) {
            if (s.equals(twoChar)) return true;
        }
        return false;
    }
}
