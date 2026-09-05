package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 项目企业任务业务对象 project_task（T11.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectTaskBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "任务ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 项目容器ID */
    @NotNull(message = "项目容器ID不能为空", groups = {AddGroup.class})
    private Long projectId;

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空", groups = {AddGroup.class})
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
}
