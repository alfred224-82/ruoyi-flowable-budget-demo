package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流配置主表 fee_workflow_config
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_workflow_config")
public class FeeWorkflowConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 工作流标识（budget_preparation/expense_approval/budget_adjustment）
     */
    private String workflowKey;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 流程说明
     */
    private String description;

    /**
     * 审批节点数量
     */
    private Integer nodeCount;

    /**
     * 当前已部署的Flowable部署ID
     */
    private String deployId;

    /**
     * 当前流程定义ID
     */
    private String procDefId;

    /**
     * 状态（DRAFT未部署/DEPLOYED已部署/DISABLED已停用）
     */
    private String status;
}
