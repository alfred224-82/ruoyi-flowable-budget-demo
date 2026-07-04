package com.ruoyi.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 预算审批流程自动部署器
 * 应用启动时自动部署预算审批流程定义（始终部署最新版本）
 *
 * @author ruoyi
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class BudgetProcessDeployer implements ApplicationRunner {

    private final RepositoryService repositoryService;

    /**
     * 预算审批流程定义Key（从配置文件读取）
     */
    @Value("${budget.process-key}")
    private String processKey;

    public String getProcessKey() {
        return processKey;
    }

    /**
     * 预算审批流程BPMN文件路径
     */
    private static final String BPMN_RESOURCE = "bpmn/budgetProcess.bpmn20.xml";

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 部署流程定义（Flowable会自动递增版本号）
            ClassPathResource resource = new ClassPathResource(BPMN_RESOURCE);
            if (!resource.exists()) {
                log.error("预算审批流程BPMN文件不存在：{}", BPMN_RESOURCE);
                return;
            }

            Deployment deployment = repositoryService.createDeployment()
                .name("预算编制三级审批流程")
                .category("budget")
                .addInputStream(BPMN_RESOURCE, resource.getInputStream())
                .deploy();

            ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

            // 确保流程定义处于激活状态（防止因之前被挂起而无法启动流程实例）
            if (processDef != null && processDef.isSuspended()) {
                repositoryService.activateProcessDefinitionById(processDef.getId(), true, null);
                log.info("预算审批流程定义已激活，版本：{}", processDef.getVersion());
            }

            // 同时激活所有历史版本的流程定义
            List<ProcessDefinition> allDefs = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .suspended()
                .list();
            for (ProcessDefinition pd : allDefs) {
                repositoryService.activateProcessDefinitionById(pd.getId(), true, null);
                log.info("激活历史版本流程定义：{}", pd.getId());
            }

            log.info("预算审批流程部署成功，版本：{}", processDef != null ? processDef.getVersion() : "unknown");
        } catch (Exception e) {
            log.error("预算审批流程部署失败", e);
        }
    }
}
