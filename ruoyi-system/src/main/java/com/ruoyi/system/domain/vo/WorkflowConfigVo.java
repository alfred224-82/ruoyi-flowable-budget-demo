package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流配置视图对象
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
public class WorkflowConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String workflowKey;

    private String workflowName;

    private String description;

    private Integer nodeCount;

    private String deployId;

    private String procDefId;

    private String status;

    private Date createTime;

    private Date updateTime;

    /**
     * 节点列表（嵌套）
     */
    private List<WorkflowNodeConfigVo> nodes;
}
