package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流审批节点配置表 fee_workflow_node_config
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
@TableName("fee_workflow_node_config")
public class FeeWorkflowNodeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 所属工作流ID
     */
    private Long workflowId;

    /**
     * 节点顺序（从1开始）
     */
    private Integer nodeSeq;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 审批角色ID
     */
    private Long roleId;

    /**
     * 角色标识（冗余）
     */
    private String roleKey;

    /**
     * 角色名称（冗余）
     */
    private String roleName;

    /**
     * 审批方式（ANY单人审批/ALL会签）
     */
    private String approvalType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
