package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeWorkflowConfig;
import com.ruoyi.system.domain.FeeWorkflowNodeConfig;
import com.ruoyi.system.domain.bo.WorkflowConfigBo;
import com.ruoyi.system.domain.vo.WorkflowConfigVo;
import com.ruoyi.system.domain.vo.WorkflowNodeConfigVo;
import com.ruoyi.system.mapper.FeeWorkflowConfigMapper;
import com.ruoyi.system.mapper.FeeWorkflowNodeConfigMapper;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.IWorkflowConfigService;
import com.ruoyi.system.utils.BpmnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流配置服务实现
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConfigServiceImpl implements IWorkflowConfigService {

    private final FeeWorkflowConfigMapper workflowConfigMapper;
    private final FeeWorkflowNodeConfigMapper nodeConfigMapper;
    private final RepositoryService repositoryService;
    private final ISysRoleService roleService;

    @Override
    public List<WorkflowConfigVo> listAll() {
        List<FeeWorkflowConfig> configs = workflowConfigMapper.selectList(
            Wrappers.<FeeWorkflowConfig>lambdaQuery().orderByAsc(FeeWorkflowConfig::getId));
        List<WorkflowConfigVo> result = new ArrayList<>();
        for (FeeWorkflowConfig config : configs) {
            WorkflowConfigVo vo = workflowConfigMapper.selectVoById(config.getId());
            vo.setNodes(getNodeList(config.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public WorkflowConfigVo getByKey(String workflowKey) {
        FeeWorkflowConfig config = workflowConfigMapper.selectOne(
            Wrappers.<FeeWorkflowConfig>lambdaQuery()
                .eq(FeeWorkflowConfig::getWorkflowKey, workflowKey));
        if (config == null) {
            return null;
        }
        WorkflowConfigVo vo = workflowConfigMapper.selectVoById(config.getId());
        vo.setNodes(getNodeList(config.getId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveConfig(WorkflowConfigBo bo) {
        // 1. 查询或创建主表记录
        FeeWorkflowConfig config;
        if (bo.getId() != null) {
            config = workflowConfigMapper.selectById(bo.getId());
            if (config == null) {
                throw new ServiceException("工作流配置不存在");
            }
        } else {
            // 按workflowKey查找
            config = workflowConfigMapper.selectOne(
                Wrappers.<FeeWorkflowConfig>lambdaQuery()
                    .eq(FeeWorkflowConfig::getWorkflowKey, bo.getWorkflowKey()));
            if (config == null) {
                config = new FeeWorkflowConfig();
                config.setWorkflowKey(bo.getWorkflowKey());
            }
        }

        config.setWorkflowName(bo.getWorkflowName());
        config.setDescription(bo.getDescription());
        config.setNodeCount(bo.getNodes().size());
        // 配置变更后状态重置为DRAFT，需重新部署
        config.setStatus("DRAFT");
        config.setDeployId(null);
        config.setProcDefId(null);

        if (config.getId() == null) {
            workflowConfigMapper.insert(config);
        } else {
            workflowConfigMapper.updateById(config);
        }

        // 2. 删除旧节点
        nodeConfigMapper.delete(Wrappers.<FeeWorkflowNodeConfig>lambdaQuery()
            .eq(FeeWorkflowNodeConfig::getWorkflowId, config.getId()));

        // 3. 批量插入新节点
        for (WorkflowConfigBo.NodeItem item : bo.getNodes()) {
            FeeWorkflowNodeConfig node = new FeeWorkflowNodeConfig();
            node.setWorkflowId(config.getId());
            node.setNodeSeq(item.getNodeSeq());
            node.setNodeName(item.getNodeName());
            node.setRoleId(item.getRoleId());
            node.setApprovalType(item.getApprovalType());

            // 冗余填充角色信息
            SysRole role = roleService.selectRoleById(item.getRoleId());
            if (role != null) {
                node.setRoleKey(role.getRoleKey());
                node.setRoleName(role.getRoleName());
            }

            nodeConfigMapper.insert(node);
        }

        log.info("保存工作流配置成功：workflowKey={}, nodeCount={}", bo.getWorkflowKey(), bo.getNodes().size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deployWorkflow(Long workflowId) {
        // 1. 查询工作流配置
        FeeWorkflowConfig config = workflowConfigMapper.selectById(workflowId);
        if (config == null) {
            throw new ServiceException("工作流配置不存在");
        }

        // 2. 查询节点列表
        List<FeeWorkflowNodeConfig> nodes = nodeConfigMapper.selectList(
            Wrappers.<FeeWorkflowNodeConfig>lambdaQuery()
                .eq(FeeWorkflowNodeConfig::getWorkflowId, workflowId)
                .orderByAsc(FeeWorkflowNodeConfig::getNodeSeq));
        if (nodes.isEmpty()) {
            throw new ServiceException("至少需要配置一个审批节点才能部署");
        }

        // 3. 生成BPMN XML
        String bpmnXml = BpmnGenerator.generateApprovalProcess(
            config.getWorkflowKey(), config.getWorkflowName(), nodes);

        // 4. 如有旧版本流程定义，挂起旧版本
        if (config.getProcDefId() != null) {
            try {
                ProcessDefinition oldDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(config.getProcDefId())
                    .singleResult();
                if (oldDef != null && !oldDef.isSuspended()) {
                    repositoryService.suspendProcessDefinitionById(oldDef.getId(), true, null);
                    log.info("挂起旧版本流程定义：{}", oldDef.getId());
                }
            } catch (Exception e) {
                log.warn("挂起旧版本流程定义失败：{}", e.getMessage());
            }
        }

        // 5. 部署到Flowable
        Deployment deployment = repositoryService.createDeployment()
            .name(config.getWorkflowName())
            .key(config.getWorkflowKey())
            .category("fee")
            .addString(config.getWorkflowKey() + ".bpmn20.xml", bpmnXml)
            .deploy();

        // 6. 查询流程定义ID
        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
            .deploymentId(deployment.getId())
            .singleResult();

        // 7. 更新主表
        config.setDeployId(deployment.getId());
        config.setProcDefId(processDef != null ? processDef.getId() : null);
        config.setStatus("DEPLOYED");
        workflowConfigMapper.updateById(config);

        log.info("工作流部署成功：workflowKey={}, deploymentId={}, procDefId={}",
            config.getWorkflowKey(), deployment.getId(),
            processDef != null ? processDef.getId() : "unknown");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableWorkflow(Long workflowId) {
        FeeWorkflowConfig config = workflowConfigMapper.selectById(workflowId);
        if (config == null) {
            throw new ServiceException("工作流配置不存在");
        }
        if (!"DEPLOYED".equals(config.getStatus())) {
            throw new ServiceException("只有已部署的工作流才能停用");
        }

        // 挂起流程定义
        if (config.getProcDefId() != null) {
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(config.getProcDefId())
                .singleResult();
            if (def != null && !def.isSuspended()) {
                repositoryService.suspendProcessDefinitionById(def.getId(), true, null);
            }
        }

        config.setStatus("DISABLED");
        workflowConfigMapper.updateById(config);
        log.info("工作流已停用：workflowKey={}", config.getWorkflowKey());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableWorkflow(Long workflowId) {
        FeeWorkflowConfig config = workflowConfigMapper.selectById(workflowId);
        if (config == null) {
            throw new ServiceException("工作流配置不存在");
        }
        if (!"DISABLED".equals(config.getStatus())) {
            throw new ServiceException("只有已停用的工作流才能启用");
        }

        // 激活流程定义
        if (config.getProcDefId() != null) {
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(config.getProcDefId())
                .singleResult();
            if (def != null && def.isSuspended()) {
                repositoryService.activateProcessDefinitionById(def.getId(), true, null);
            }
        }

        config.setStatus("DEPLOYED");
        workflowConfigMapper.updateById(config);
        log.info("工作流已启用：workflowKey={}", config.getWorkflowKey());
        return true;
    }

    @Override
    public String getDeployedProcessKey(String workflowKey) {
        FeeWorkflowConfig config = workflowConfigMapper.selectOne(
            Wrappers.<FeeWorkflowConfig>lambdaQuery()
                .eq(FeeWorkflowConfig::getWorkflowKey, workflowKey)
                .eq(FeeWorkflowConfig::getStatus, "DEPLOYED"));
        if (config != null) {
            return config.getWorkflowKey();
        }
        return null;
    }

    /**
     * 获取指定工作流的节点列表
     */
    private List<WorkflowNodeConfigVo> getNodeList(Long workflowId) {
        List<FeeWorkflowNodeConfig> nodes = nodeConfigMapper.selectList(
            Wrappers.<FeeWorkflowNodeConfig>lambdaQuery()
                .eq(FeeWorkflowNodeConfig::getWorkflowId, workflowId)
                .orderByAsc(FeeWorkflowNodeConfig::getNodeSeq));
        List<WorkflowNodeConfigVo> voList = new ArrayList<>();
        for (FeeWorkflowNodeConfig node : nodes) {
            WorkflowNodeConfigVo vo = new WorkflowNodeConfigVo();
            vo.setId(node.getId());
            vo.setWorkflowId(node.getWorkflowId());
            vo.setNodeSeq(node.getNodeSeq());
            vo.setNodeName(node.getNodeName());
            vo.setRoleId(node.getRoleId());
            vo.setRoleKey(node.getRoleKey());
            vo.setRoleName(node.getRoleName());
            vo.setApprovalType(node.getApprovalType());
            vo.setCreateTime(node.getCreateTime());
            vo.setUpdateTime(node.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }
}
