package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Boss 背景画布加人「候选人分组」视图对象（T26.10，小微版 M26）
 * <p>选人弹窗数据源：按标准角色分组返回租户下可加人的人员（员工=导游/司机 / 顾客=游客），
 * 组名为租户角色显示名（{@code tenant_role_mapping.display_name}，未配置回退通用名）。
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatCandidateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标准角色编码（salesman=员工 / customer=顾客） */
    private String roleCode;

    /** 角色显示名（租户映射，如 导游/司机/游客；未配置回退 员工/顾客） */
    private String roleLabel;

    /** 该角色下候选人员列表 */
    private List<Person> persons;

    /**
     * 候选人员
     */
    @Data
    public static class Person implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 用户ID */
        private Long userId;

        /** 昵称/姓名 */
        private String name;

        /** 头像 URL */
        private String avatar;
    }
}
