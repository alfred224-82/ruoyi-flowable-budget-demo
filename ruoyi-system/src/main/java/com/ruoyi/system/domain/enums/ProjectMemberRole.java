package com.ruoyi.system.domain.enums;

/**
 * 项目容器成员角色五枚举（T11.2）
 * <p>与系统角色 sys_role 完全解耦：容器内权限由本枚举决定，不复用全局角色。
 * 存量 fee_project_member 映射：MANAGER→manager / MEMBER→member / VIEWER→member（只读）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public enum ProjectMemberRole {

    /** 拥有者（发起老板/店长，唯一，可解散容器、转让） */
    OWNER("owner", "拥有者"),

    /** 管理者（店长/项目经理，可编辑容器、管理成员与任务） */
    MANAGER("manager", "管理者"),

    /** 普通成员（参与协作，可查看与提交任务） */
    MEMBER("member", "成员"),

    /** 参与者（活动/团购参与者，只读容器 + 报名） */
    PARTICIPANT("participant", "参与者"),

    /** 报名者（仅报名登记，最小权限，游客报名回填后归属此角色） */
    SIGNUP("signup", "报名者");

    private final String code;
    private final String label;

    ProjectMemberRole(String code, String label) {
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
     * 按 code 解析枚举，非法值回落 MEMBER（只读安全默认）
     */
    public static ProjectMemberRole of(String code) {
        if (code != null) {
            for (ProjectMemberRole role : values()) {
                if (role.code.equalsIgnoreCase(code)) {
                    return role;
                }
            }
        }
        return MEMBER;
    }

    /**
     * 是否具备容器管理权限（owner/manager）
     */
    public boolean canManage() {
        return this == OWNER || this == MANAGER;
    }
}
