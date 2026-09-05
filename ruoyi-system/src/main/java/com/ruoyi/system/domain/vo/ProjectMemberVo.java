package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目容器成员视图对象 project_member（T11.2）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectMemberVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 项目容器ID */
    private Long projectId;

    /** 用户ID */
    private Long userId;

    /** 用户姓名（关联展示，非本表字段时由服务层回填） */
    private String userName;

    /** 容器角色：owner/manager/member/participant/signup */
    private String role;

    /** 加入时间 */
    private Date joinTime;

    private Date createTime;
}
