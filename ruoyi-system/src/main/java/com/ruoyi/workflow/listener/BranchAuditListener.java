package com.ruoyi.workflow.listener;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 分公司审核任务监听器
 * <p>
 * 功能：动态分配分公司负责人为审批人
 * - 从预算单中获取组织ID（orgId）
 * - 查询该组织的父级部门（分公司）
 * - 查询分公司负责人（leader）
 * - 将分公司负责人设置为任务审批人
 * </p>
 *
 * @author RuoYi
 * @since 2026/05/31
 */
@Slf4j
@Component(value = "branchAuditListener")
public class BranchAuditListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("========== 分公司审核分配监听器触发 ==========");
        log.info("事件类型: {}", eventName);
        log.info("任务定义Key: {}", taskDefinitionKey);
        log.info("流程实例ID: {}", processInstanceId);

        try {
            if (!TaskListener.EVENTNAME_CREATE.equals(eventName)) {
                log.warn("分公司审核分配监听器只在 create 事件中执行，当前事件: {}", eventName);
                return;
            }

            handleBranchAuditAssign(delegateTask);

            log.info("========== 分公司审核分配监听器完成 ==========");
        } catch (Exception e) {
            log.error("分公司审核分配失败，流程实例ID: {}, 任务Key: {}", processInstanceId, taskDefinitionKey, e);
            throw new RuntimeException("分公司审核分配失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理分公司审核分配逻辑
     *
     * @param delegateTask 任务委托对象
     */
    private void handleBranchAuditAssign(DelegateTask delegateTask) {
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("开始分配分公司审核人");

        // 1. 从流程变量中获取组织ID
        Object orgIdObj = delegateTask.getVariable("orgId");
        if (orgIdObj == null) {
            log.error("未找到组织ID，无法分配分公司审核人");
            throw new RuntimeException("未找到组织ID，无法分配分公司审核人");
        }

        Long orgId = convertToLong(orgIdObj);
        log.info("组织ID: {}", orgId);

        // 2. 获取 Mapper
        SysDeptMapper sysDeptMapper = SpringUtils.getBean(SysDeptMapper.class);

        // 3. 查询当前组织信息
        SysDept currentDept = sysDeptMapper.selectById(orgId);
        if (currentDept == null) {
            log.error("组织不存在，ID: {}", orgId);
            throw new RuntimeException("组织不存在，ID: " + orgId);
        }

        log.info("当前组织: {} (ID: {})", currentDept.getDeptName(), orgId);

        // 4. 查找分公司（父级部门）
        SysDept branchDept = findBranchDepartment(currentDept, sysDeptMapper);
        if (branchDept == null) {
            log.error("未找到分公司层级，当前组织: {}", currentDept.getDeptName());
            throw new RuntimeException("未找到分公司层级，请检查组织架构配置");
        }

        log.info("分公司: {} (ID: {})", branchDept.getDeptName(), branchDept.getDeptId());

        // 5. 获取分公司负责人
        String branchLeader = branchDept.getLeader();
        if (StrUtil.isBlank(branchLeader)) {
            log.error("分公司[{}]未设置负责人", branchDept.getDeptName());
            throw new RuntimeException("分公司[" + branchDept.getDeptName() + "]未设置负责人");
        }

        log.info("分公司负责人: {}", branchLeader);

        // 6. 验证负责人是否为有效用户
        SysUserMapper sysUserMapper = SpringUtils.getBean(SysUserMapper.class);
        long userCount = sysUserMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ruoyi.common.core.domain.entity.SysUser>()
                .eq(com.ruoyi.common.core.domain.entity.SysUser::getUserName, branchLeader)
                .eq(com.ruoyi.common.core.domain.entity.SysUser::getStatus, "0")
        );

        if (userCount == 0) {
            log.error("分公司负责人[{}]不是有效用户或已禁用", branchLeader);
            throw new RuntimeException("分公司负责人[" + branchLeader + "]不是有效用户或已禁用");
        }

        // 7. 设置任务审批人
        delegateTask.setAssignee(branchLeader);
        log.info("成功设置分公司审核人: {}", branchLeader);

        // 8. 将审批人信息设置为流程变量，方便后续使用
        delegateTask.setVariable("branchAuditor", branchLeader);
        delegateTask.setVariable("branchAuditorName", branchDept.getDeptName() + "-负责人");
        delegateTask.setVariable("branchDeptId", branchDept.getDeptId());
        delegateTask.setVariable("branchDeptName", branchDept.getDeptName());

        // 9. 更新预算单的当前处理人
        updateBudgetSheetHandler(delegateTask, branchLeader);
    }

    /**
     * 查找分公司层级（父级部门）
     * <p>
     * 根据组织架构设计，分公司通常是部门的父级。
     * 如果当前部门就是分公司级别，则返回其自身。
     * </p>
     *
     * @param currentDept 当前部门
     * @param sysDeptMapper 部门Mapper
     * @return 分公司部门
     */
    private SysDept findBranchDepartment(SysDept currentDept, SysDeptMapper sysDeptMapper) {
        // 策略1：如果当前部门的 parentId 为 0 或 null，说明它本身就是顶级（可能是分公司）
        if (currentDept.getParentId() == null || currentDept.getParentId() == 0) {
            log.info("当前组织即为顶级组织（分公司）");
            return currentDept;
        }

        // 策略2：查询父级部门
        SysDept parentDept = sysDeptMapper.selectById(currentDept.getParentId());
        if (parentDept == null) {
            log.warn("未找到父级部门，当前部门: {}", currentDept.getDeptName());
            return null;
        }

        // 策略3：判断父级是否为分公司
        // 这里可以根据实际业务规则判断，例如：
        // - 父级的 parentId 为 0（说明父级是顶级）
        // - 或者父级的某个特征字段标识为分公司

        if (parentDept.getParentId() == null || parentDept.getParentId() == 0) {
            // 父级是顶级，认为父级就是分公司
            log.info("父级组织为顶级组织，认定为分公司");
            return parentDept;
        } else {
            // 继续向上查找，直到找到顶级
            log.info("继续向上查找分公司...");
            return findBranchDepartment(parentDept, sysDeptMapper);
        }
    }

    /**
     * 更新预算单的当前处理人
     *
     * @param delegateTask 任务委托对象
     * @param handler 处理人
     */
    private void updateBudgetSheetHandler(DelegateTask delegateTask, String handler) {
        try {
            Object budgetSheetIdObj = delegateTask.getVariable("budgetSheetId");
            if (budgetSheetIdObj == null) {
                log.warn("未找到预算单ID，跳过更新处理人");
                return;
            }

            Long budgetSheetId = convertToLong(budgetSheetIdObj);

            // 通过反射获取 BudgetSheetMapper（避免循环依赖）
            Class<?> mapperClass = Class.forName("com.ruoyi.system.mapper.BudgetSheetMapper");
            Object budgetSheetMapper = SpringUtils.getBean(mapperClass);

            // 查询预算单
            java.lang.reflect.Method selectByIdMethod = mapperClass.getMethod("selectById", java.io.Serializable.class);
            Object budgetSheet = selectByIdMethod.invoke(budgetSheetMapper, budgetSheetId);

            if (budgetSheet != null) {
                // 更新当前处理人
                java.lang.reflect.Method setCurrentHandlerMethod = budgetSheet.getClass().getMethod("setCurrentHandler", String.class);
                setCurrentHandlerMethod.invoke(budgetSheet, handler);

                // 更新状态为 Branch_Pending（分公司审核中）
                java.lang.reflect.Method setStatusMethod = budgetSheet.getClass().getMethod("setStatus", String.class);
                setStatusMethod.invoke(budgetSheet, "Branch_Pending");

                // 保存更新
                java.lang.reflect.Method updateByIdMethod = mapperClass.getMethod("updateById", Object.class);
                updateByIdMethod.invoke(budgetSheetMapper, budgetSheet);

                log.info("已更新预算单处理人为: {}, 状态: Branch_Pending", handler);
            }
        } catch (Exception e) {
            log.error("更新预算单处理人失败", e);
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
