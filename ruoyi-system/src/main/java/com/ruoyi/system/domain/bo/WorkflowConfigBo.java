package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 工作流配置业务对象
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
public class WorkflowConfigBo {

    /**
     * 主键（编辑时传入）
     */
    private Long id;

    /**
     * 工作流标识
     */
    @NotBlank(message = "工作流标识不能为空")
    private String workflowKey;

    /**
     * 工作流名称
     */
    @NotBlank(message = "工作流名称不能为空")
    private String workflowName;

    /**
     * 流程说明
     */
    private String description;

    /**
     * 审批节点列表
     */
    @NotEmpty(message = "至少配置一个审批节点")
    @Valid
    private List<NodeItem> nodes;

    /**
     * 审批节点项
     */
    @Data
    public static class NodeItem {
        /**
         * 节点顺序
         */
        @NotNull(message = "节点顺序不能为空")
        private Integer nodeSeq;

        /**
         * 节点名称
         */
        @NotBlank(message = "节点名称不能为空")
        private String nodeName;

        /**
         * 审批角色ID
         */
        @NotNull(message = "审批角色不能为空")
        private Long roleId;

        /**
         * 审批方式（ANY/ALL）
         */
        @NotBlank(message = "审批方式不能为空")
        private String approvalType;
    }
}
