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
 * 项目统一容器对象 project（T11.1 项目即容器）
 * <p>企业任务(type=general) 与 活动报名(type=activity/team_buy) 共用本容器；
 * 业态差异字段一律进 extra_config JSON（桌游:桌数/时长/主持人；生鲜:产地/保鲜期/配送范围），
 * 不新增物理列、不 fork 业态（设计文档 §1.8.5）。
 * <p>与既有 fee_project 关系：fee_project 为 fund_control 业态下的资金视图，通过 project_id 关联本容器，不合并、不迁移存量。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业态包编码（biz_bundle.biz_code）：fund_control/boardgame/fresh_food
     */
    private String bizCode;

    /**
     * 容器类型：activity(活动)/team_buy(团购)/general(通用/企业任务)
     */
    private String type;

    /**
     * 项目/活动名称
     */
    private String name;

    /**
     * 状态：DRAFT/PUBLISHED/ONGOING/FINISHED/CLOSED/CANCELLED
     */
    private String status;

    /**
     * 负责人（发起老板/店长）用户ID
     */
    private Long ownerUserId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 业态差异字段 JSON（桌游:桌数/时长/主持人；生鲜:产地/保鲜期/配送范围）
     */
    private String extraConfig;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
