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
 * 部门经理审核任务监听器
 * <p>
 * 功能：动态分配部门经理为审批人
 * - 从预算单中获取组织ID（deptId）
 * - 查询该部门的负责人（leader）
 * - 将负责人设置为任务审批人
 * </p>
 *
 * @author RuoYi
 * @since 2026/05/31
 */
@Slf4j
@Component(value = "deptManagerAssignListener")
public class DeptManagerAssignListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("========== 部门经理分配监听器触发 ==========");
        log.info("事件类型: {}", eventName);
        log.info("任务定义Key: {}", taskDefinitionKey);
        log.info("流程实例ID: {}", processInstanceId);

        try {
            if (!TaskListener.EVENTNAME_CREATE.equals(eventName)) {
                log.warn("部门经理分配监听器只在 create 事件中执行，当前事件: {}", eventName);
                return;
            }

            handleDeptManagerAssign(delegateTask);

            log.info("========== 部门经理分配监听器完成 ==========");
        } catch (Exception e) {
            log.error("部门经理分配失败，流程实例ID: {}, 任务Key: {}", processInstanceId, taskDefinitionKey, e);
            throw new RuntimeException("部门经理分配失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理部门经理分配逻辑
     *
     * @param delegateTask 任务委托对象
     */
    private void handleDeptManagerAssign(DelegateTask delegateTask) {
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("开始分配部门经理审批人");

        // 1. 从流程变量中获取部门ID
        Object deptIdObj = delegateTask.getVariable("orgId");
        if (deptIdObj == null) {
            log.warn("未找到组织ID，尝试从当前登录用户获取部门ID");
            // 如果流程变量中没有，可以尝试从其他来源获取
            deptIdObj = delegateTask.getVariable("deptId");
        }

        if (deptIdObj == null) {
            log.error("未找到部门ID，无法分配部门经理");
            throw new RuntimeException("未找到部门ID，无法分配部门经理");
        }

        Long deptId = convertToLong(deptIdObj);
        log.info("部门ID: {}", deptId);

        // 2. 获取 Mapper
        SysDeptMapper sysDeptMapper = SpringUtils.getBean(SysDeptMapper.class);

        // 3. 查询部门信息
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            log.error("部门不存在，ID: {}", deptId);
            throw new RuntimeException("部门不存在，ID: " + deptId);
        }

        // 4. 获取部门负责人
        String leader = dept.getLeader();
        if (StrUtil.isBlank(leader)) {
            log.error("部门[{}]未设置负责人", dept.getDeptName());
            throw new RuntimeException("部门[" + dept.getDeptName() + "]未设置负责人");
        }

        log.info("部门负责人: {}", leader);

        // 5. 验证负责人是否为有效用户
        SysUserMapper sysUserMapper = SpringUtils.getBean(SysUserMapper.class);
        long userCount = sysUserMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ruoyi.common.core.domain.entity.SysUser>()
                .eq(com.ruoyi.common.core.domain.entity.SysUser::getUserName, leader)
                .eq(com.ruoyi.common.core.domain.entity.SysUser::getStatus, "0")
        );

        if (userCount == 0) {
            log.error("部门负责人[{}]不是有效用户或已禁用", leader);
            throw new RuntimeException("部门负责人[" + leader + "]不是有效用户或已禁用");
        }

        // 6. 设置任务审批人
        delegateTask.setAssignee(leader);
        log.info("成功设置任务审批人: {}", leader);

        // 7. 将审批人设置为流程变量，方便后续使用
        delegateTask.setVariable("deptManager", leader);
        delegateTask.setVariable("deptManagerName", dept.getDeptName() + "-负责人");
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

