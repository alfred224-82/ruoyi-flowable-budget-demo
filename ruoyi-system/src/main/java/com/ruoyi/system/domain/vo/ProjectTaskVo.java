package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目企业任务视图对象 project_task（T11.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 项目容器ID */
    private Long projectId;

    /** 任务名称 */
    private String taskName;

    /** 指派人用户ID */
    private Long assigneeUserId;

    /** 进度百分比（0-100） */
    private Integer progress;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 附件（JSON数组/URL列表） */
    private String attachment;

    /** 状态：PENDING/DOING/DONE/CANCELLED */
    private String status;

    private Date createTime;

    private Date updateTime;
}
