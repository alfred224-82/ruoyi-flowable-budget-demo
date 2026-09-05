package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目成员视图对象 fee_project_member（T5.1）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectMemberVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 用户ID */
    private Long userId;
    /** 用户姓名 */
    private String userName;
    /** 成员角色(MANAGER/MEMBER/VIEWER) */
    private String role;
    /** 投入比例%(专业版成员负载预警) */
    private Integer investRatio;
    /** 是否有效 */
    private Integer isActive;
    /** 创建时间 */
    private Date createTime;
}
