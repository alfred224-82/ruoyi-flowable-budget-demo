package com.ruoyi.workflow.listener;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 分公司网关监听器
 * <p>
 * 功能：在分公司审核后的排他网关中设置流程变量，控制流程走向
 * - 判断分公司审核结果（通过/驳回）
 * - 设置分支条件变量
 * - 更新预算单状态
 * </p>
 *
 * @author RuoYi
 * @since 2026/05/31
 */
@Slf4j
@Component(value = "branchGatewayListener")
public class BranchGatewayListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String processInstanceId = execution.getProcessInstanceId();
        String currentActivityId = execution.getCurrentActivityId();

        log.info("========== 分公司网关监听器触发 ==========");
        log.info("事件类型: {}", eventName);
        log.info("流程实例ID: {}", processInstanceId);
        log.info("当前活动ID: {}", currentActivityId);

        try {
            if (!ExecutionListener.EVENTNAME_START.equals(eventName)) {
                log.warn("分公司网关监听器只在 start 事件中执行，当前事件: {}", eventName);
                return;
            }

            handleBranchGateway(execution);

            log.info("========== 分公司网关监听器完成 ==========");
        } catch (Exception e) {
            log.error("分公司网关处理失败，流程实例ID: {}", processInstanceId, e);
            throw new RuntimeException("分公司网关处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理分公司网关逻辑
     *
     * @param execution 执行委托对象
     */
    private void handleBranchGateway(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        log.info("开始处理分公司网关分支逻辑");

        // 1. 获取分公司审核结果
        Object auditResultObj = execution.getVariable("branchAuditResult");
        if (auditResultObj == null) {
            log.error("未找到分公司审核结果变量");
            throw new RuntimeException("未找到分公司审核结果，请确认审核任务已完成");
        }

        String auditResult = auditResultObj.toString();
        log.info("分公司审核结果: {}", auditResult);

        // 2. 验证审核结果的合法性
        if (!"approve".equalsIgnoreCase(auditResult) && !"reject".equalsIgnoreCase(auditResult)) {
            log.error("无效的审核结果: {}", auditResult);
            throw new RuntimeException("无效的审核结果: " + auditResult + "，应为 approve 或 reject");
        }

        // 3. 设置网关分支条件变量
        boolean isApproved = "approve".equalsIgnoreCase(auditResult);
        execution.setVariable("branchApproved", isApproved);
        execution.setVariable("branchRejected", !isApproved);

        log.info("设置分支变量: branchApproved={}, branchRejected={}", isApproved, !isApproved);

        // 4. 根据审核结果处理不同逻辑
        if (isApproved) {
            handleBranchApproved(execution);
        } else {
            handleBranchRejected(execution);
        }

        log.info("分公司网关分支处理完成");
    }

    /**
     * 处理分公司审核通过的情况
     *
     * @param execution 执行委托对象
     */
    private void handleBranchApproved(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        log.info("========== 分公司审核通过 ==========");

        // 1. 更新预算单状态为 Pending_Review（待总部审核）
        updateBudgetSheetStatus(execution, "Pending_Review", "分公司审核通过，提交总部审核");

        // 2. 记录操作日志
        recordOperationLog(execution, "APPROVE", "分公司审核通过");

        // 3. 清空驳回相关信息
        execution.setVariable("rejectLevel", "None");
        execution.setVariable("rejectReason", null);

        log.info("分公司审核通过，流程将继续流转到总部审核节点");
    }

    /**
     * 处理分公司审核驳回的情况
     *
     * @param execution 执行委托对象
     */
    private void handleBranchRejected(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        log.info("========== 分公司审核驳回 ==========");

        // 1. 获取驳回理由
        Object rejectReasonObj = execution.getVariable("branchRejectReason");
        String rejectReason = rejectReasonObj != null ? rejectReasonObj.toString() : "分公司审核驳回";

        if (StrUtil.isBlank(rejectReason)) {
            rejectReason = "分公司审核驳回";
        }

        log.info("驳回理由: {}", rejectReason);

        // 2. 更新预算单状态为 Pending_Revision（待修改）
        updateBudgetSheetStatus(execution, "Pending_Revision", rejectReason);

        // 3. 设置驳回来源
        execution.setVariable("rejectLevel", "Branch");
        execution.setVariable("rejectReason", rejectReason);

        // 4. 计算截止时间（例如：24小时后）
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.HOUR, 24);
        execution.setVariable("deadlineTime", calendar.getTime());

        // 5. 记录操作日志
        recordOperationLog(execution, "REJECT", "分公司审核驳回: " + rejectReason);

        // 6. 记录驳回历史
        recordRejectHistory(execution, rejectReason);

        log.info("分公司审核驳回，流程将返回到申请人进行修改");
    }

    /**
     * 更新预算单状态
     *
     * @param execution 执行委托对象
     * @param newStatus 新状态
     * @param remark 备注
     */
    private void updateBudgetSheetStatus(DelegateExecution execution, String newStatus, String remark) {
        try {
            Object budgetSheetIdObj = execution.getVariable("budgetSheetId");
            if (budgetSheetIdObj == null) {
                log.warn("未找到预算单ID，跳过状态更新");
                return;
            }

            Long budgetSheetId = convertToLong(budgetSheetIdObj);

            // 通过反射获取 BudgetSheetMapper
            Class<?> mapperClass = Class.forName("com.ruoyi.system.mapper.BudgetSheetMapper");
            Object budgetSheetMapper = SpringUtils.getBean(mapperClass);

            // 查询预算单
            java.lang.reflect.Method selectByIdMethod = mapperClass.getMethod("selectById", java.io.Serializable.class);
            Object budgetSheet = selectByIdMethod.invoke(budgetSheetMapper, budgetSheetId);

            if (budgetSheet != null) {
                // 获取旧状态
                java.lang.reflect.Method getStatusMethod = budgetSheet.getClass().getMethod("getStatus");
                String oldStatus = (String) getStatusMethod.invoke(budgetSheet);

                // 更新状态
                java.lang.reflect.Method setStatusMethod = budgetSheet.getClass().getMethod("setStatus", String.class);
                setStatusMethod.invoke(budgetSheet, newStatus);

                // 更新备注
                if (StrUtil.isNotBlank(remark)) {
                    java.lang.reflect.Method setRemarkMethod = budgetSheet.getClass().getMethod("setRemark", String.class);
                    setRemarkMethod.invoke(budgetSheet, remark);
                }

                // 保存更新
                java.lang.reflect.Method updateByIdMethod = mapperClass.getMethod("updateById", Object.class);
                updateByIdMethod.invoke(budgetSheetMapper, budgetSheet);

                log.info("预算单状态已更新: {} -> {}, 备注: {}", oldStatus, newStatus, remark);
            }
        } catch (Exception e) {
            log.error("更新预算单状态失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 记录操作日志
     *
     * @param execution 执行委托对象
     * @param operationType 操作类型
     * @param operationDesc 操作描述
     */
    private void recordOperationLog(DelegateExecution execution, String operationType, String operationDesc) {
        try {
            Object budgetSheetIdObj = execution.getVariable("budgetSheetId");
            if (budgetSheetIdObj == null) {
                return;
            }

            Long budgetSheetId = convertToLong(budgetSheetIdObj);
            String sheetNo = (String) execution.getVariable("sheetNo");
            String operatorUser = (String) execution.getVariable("branchAuditor");
            String operatorName = (String) execution.getVariable("branchAuditorName");

            // 通过反射获取 BudgetOperationLogMapper
            Class<?> mapperClass = Class.forName("com.ruoyi.system.mapper.BudgetOperationLogMapper");
            Object logMapper = SpringUtils.getBean(mapperClass);

            // 创建操作日志对象
            Class<?> logClass = Class.forName("com.ruoyi.system.domain.BudgetOperationLog");
            Object operationLog = logClass.getDeclaredConstructor().newInstance();

            // 设置属性
            java.lang.reflect.Method setSheetIdMethod = logClass.getMethod("setSheetId", Long.class);
            setSheetIdMethod.invoke(operationLog, budgetSheetId);

            java.lang.reflect.Method setSheetNoMethod = logClass.getMethod("setSheetNo", String.class);
            setSheetNoMethod.invoke(operationLog, sheetNo);

            java.lang.reflect.Method setOperationTypeMethod = logClass.getMethod("setOperationType", String.class);
            setOperationTypeMethod.invoke(operationLog, operationType);

            java.lang.reflect.Method setOperatorUserMethod = logClass.getMethod("setOperatorUser", String.class);
            setOperatorUserMethod.invoke(operationLog, operatorUser);

            java.lang.reflect.Method setOperatorNameMethod = logClass.getMethod("setOperatorName", String.class);
            setOperatorNameMethod.invoke(operationLog, operatorName);

            java.lang.reflect.Method setOperationDescMethod = logClass.getMethod("setOperationDesc", String.class);
            setOperationDescMethod.invoke(operationLog, operationDesc);

            // 获取旧状态和新状态
            java.lang.reflect.Method setOldStatusMethod = logClass.getMethod("setOldStatus", String.class);
            setOldStatusMethod.invoke(operationLog, execution.getVariable("oldStatus"));

            java.lang.reflect.Method setNewStatusMethod = logClass.getMethod("setNewStatus", String.class);
            setNewStatusMethod.invoke(operationLog, execution.getVariable("newStatus"));

            // 保存日志
            java.lang.reflect.Method insertMethod = mapperClass.getMethod("insert", Object.class);
            insertMethod.invoke(logMapper, operationLog);

            log.info("操作日志已记录: {}", operationDesc);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 记录驳回历史
     *
     * @param execution 执行委托对象
     * @param rejectReason 驳回理由
     */
    private void recordRejectHistory(DelegateExecution execution, String rejectReason) {
        try {
            Object budgetSheetIdObj = execution.getVariable("budgetSheetId");
            if (budgetSheetIdObj == null) {
                return;
            }

            Long budgetSheetId = convertToLong(budgetSheetIdObj);
            String sheetNo = (String) execution.getVariable("sheetNo");
            String rejectFromUser = (String) execution.getVariable("branchAuditor");
            String rejectFromName = (String) execution.getVariable("branchAuditorName");
            Object deadlineTimeObj = execution.getVariable("deadlineTime");

            // 通过反射获取 BudgetRejectHistoryMapper
            Class<?> mapperClass = Class.forName("com.ruoyi.system.mapper.BudgetRejectHistoryMapper");
            Object historyMapper = SpringUtils.getBean(mapperClass);

            // 创建驳回历史对象
            Class<?> historyClass = Class.forName("com.ruoyi.system.domain.BudgetRejectHistory");
            Object rejectHistory = historyClass.getDeclaredConstructor().newInstance();

            // 设置属性
            java.lang.reflect.Method setSheetIdMethod = historyClass.getMethod("setSheetId", Long.class);
            setSheetIdMethod.invoke(rejectHistory, budgetSheetId);

            java.lang.reflect.Method setSheetNoMethod = historyClass.getMethod("setSheetNo", String.class);
            setSheetNoMethod.invoke(rejectHistory, sheetNo);

            java.lang.reflect.Method setRejectFromLevelMethod = historyClass.getMethod("setRejectFromLevel", String.class);
            setRejectFromLevelMethod.invoke(rejectHistory, "Branch");

            java.lang.reflect.Method setRejectFromUserMethod = historyClass.getMethod("setRejectFromUser", String.class);
            setRejectFromUserMethod.invoke(rejectHistory, rejectFromUser);

            java.lang.reflect.Method setRejectFromNameMethod = historyClass.getMethod("setRejectFromName", String.class);
            setRejectFromNameMethod.invoke(rejectHistory, rejectFromName);

            java.lang.reflect.Method setRejectToLevelMethod = historyClass.getMethod("setRejectToLevel", String.class);
            setRejectToLevelMethod.invoke(rejectHistory, "Dept");

            java.lang.reflect.Method setRejectReasonMethod = historyClass.getMethod("setRejectReason", String.class);
            setRejectReasonMethod.invoke(rejectHistory, rejectReason);

            java.lang.reflect.Method setDeadlineTimeMethod = historyClass.getMethod("setDeadlineTime", java.util.Date.class);
            if (deadlineTimeObj instanceof java.util.Date) {
                setDeadlineTimeMethod.invoke(rejectHistory, deadlineTimeObj);
            }

            // 保存历史记录
            java.lang.reflect.Method insertMethod = mapperClass.getMethod("insert", Object.class);
            insertMethod.invoke(historyMapper, rejectHistory);

            log.info("驳回历史已记录");
        } catch (Exception e) {
            log.error("记录驳回历史失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 将对象转换为 Long 类型
     *
     * @param obj 对象
     * @return Long 值
     */
    private Long convertToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            return Long.parseLong((String) obj);
        } else {
            throw new IllegalArgumentException("无法转换为Long类型: " + obj.getClass().getName());
        }
    }
}

