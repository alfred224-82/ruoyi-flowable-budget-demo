package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流审批节点配置视图对象
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
public class WorkflowNodeConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long workflowId;

    private Integer nodeSeq;

    private String nodeName;

    private Long roleId;

    private String roleKey;

    private String roleName;

    private String approvalType;

    private Date createTime;

    private Date updateTime;
}
