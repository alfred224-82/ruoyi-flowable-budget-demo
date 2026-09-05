package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.WorkflowConfigBo;
import com.ruoyi.system.domain.vo.WorkflowConfigVo;

import java.util.List;

/**
 * 工作流配置服务接口
 *
 * @author ruoyi
 * @date 2026-07-24
 */
public interface IWorkflowConfigService {

    /**
     * 查询全部工作流配置（含节点列表）
     */
    List<WorkflowConfigVo> listAll();

    /**
     * 根据workflowKey查询单条配置（含节点）
     */
    WorkflowConfigVo getByKey(String workflowKey);

    /**
     * 保存工作流配置（主表+节点一起保存）
     */
    Boolean saveConfig(WorkflowConfigBo bo);

    /**
     * 部署工作流（根据节点配置动态生成BPMN并部署到Flowable）
     */
    Boolean deployWorkflow(Long workflowId);

    /**
     * 停用工作流（挂起流程定义）
     */
    Boolean disableWorkflow(Long workflowId);

    /**
     * 启用工作流（激活流程定义）
     */
    Boolean enableWorkflow(Long workflowId);

    /**
     * 获取指定工作流当前已部署的流程定义Key（供业务启动流程时使用）
     */
    String getDeployedProcessKey(String workflowKey);
}
