package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 项目企业任务对象 project_task（T11.3，type=general 容器使用）
 * <p>与活动报名 project_signup 分表：企业任务走本表（指派/进度/截止/附件），
 * 活动/团购报名走 project_signup（人数/名额/候补/签到）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_task")
public class ProjectTask extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 项目容器ID（FK→project）
     */
    private Long projectId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 指派人用户ID
     */
    private Long assigneeUserId;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 截止时间
     */
    private Date deadline;

    /**
     * 附件（JSON数组/URL列表）
     */
    private String attachment;

    /**
     * 状态：PENDING/DOING/DONE/CANCELLED
     */
    private String status;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
