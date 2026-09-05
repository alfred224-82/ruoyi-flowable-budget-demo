package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 成员跨项目负载视图对象（T7.5 / FEAT_MEMBER_LOAD）
 * 投入比例合计>100% 提示负载超限（仅提示不拦截）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeMemberLoadVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 参与进行中项目数 */
    private Integer projectCount;

    /** 投入比例合计(%) */
    private Integer totalRatio;

    /** 是否负载超限(合计>100%) */
    private Boolean overLoad;

    /** 参与的项目明细 */
    private List<ProjectPart> projects = new ArrayList<>();

    /**
     * 项目参与明细
     */
    @Data
    public static class ProjectPart implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 项目ID */
        private Long projectId;
        /** 项目名称 */
        private String projectName;
        /** 成员角色 */
        private String role;
        /** 投入比例% */
        private Integer investRatio;
        /** 项目状态 */
        private String projectStatus;
    }
}
