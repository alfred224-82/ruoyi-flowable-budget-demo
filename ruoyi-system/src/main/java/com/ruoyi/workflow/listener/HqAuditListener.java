package com.ruoyi.workflow.listener;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 总部审核任务监听器
 * <p>
 * 功能：动态分配总部审核人员
 * - 从流程变量中获取总部审核人配置
 * - 支持指定具体用户或角色
 * - 将总部审核人设置为任务审批人
 * </p>
 *
 * @author RuoYi
 * @since 2026/05/31
 */
@Slf4j
@Component(value = "hqAuditListener")
public class HqAuditListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("========== 总部审核分配监听器触发 ==========");
        log.info("事件类型: {}", eventName);
        log.info("任务定义Key: {}", taskDefinitionKey);
        log.info("流程实例ID: {}", processInstanceId);

        try {
            if (!TaskListener.EVENTNAME_CREATE.equals(eventName)) {
                log.warn("总部审核分配监听器只在 create 事件中执行，当前事件: {}", eventName);
                return;
            }

            handleHqAuditAssign(delegateTask);

            log.info("========== 总部审核分配监听器完成 ==========");
        } catch (Exception e) {
            log.error("总部审核分配失败，流程实例ID: {}, 任务Key: {}", processInstanceId, taskDefinitionKey, e);
            throw new RuntimeException("总部审核分配失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理总部审核分配逻辑
     *
     * @param delegateTask 任务委托对象
     */
    private void handleHqAuditAssign(DelegateTask delegateTask) {
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("开始分配总部审核人");

        // 1. 从流程变量中获取总部审核人
        // 优先级：hqAuditor > hqAuditRole > 默认配置
        String hqAuditor = getHqAuditor(delegateTask);

        if (StrUtil.isBlank(hqAuditor)) {
            log.error("未找到总部审核人配置");
            throw new RuntimeException("未找到总部审核人配置，请检查流程变量或系统配置");
        }

        log.info("总部审核人: {}", hqAuditor);

        // 2. 验证审核人是否为有效用户
        validateUser(hqAuditor);

        // 3. 设置任务审批人
        delegateTask.setAssignee(hqAuditor);
        log.info("成功设置总部审核人: {}", hqAuditor);

        // 4. 将审批人信息设置为流程变量，方便后续使用
        delegateTask.setVariable("hqAuditor", hqAuditor);
        delegateTask.setVariable("hqAuditorName", "总部审核员");

        // 5. 更新预算单的当前处理人
        updateBudgetSheetHandler(delegateTask, hqAuditor);
    }

    /**
     * 获取总部审核人
     * <p>
     * 获取策略（按优先级）：
     * 1. 流程变量 hqAuditor（指定具体用户）
     * 2. 流程变量 hqAuditRole（指定角色，取第一个用户）
     * 3. 系统配置的默认总部审核人
     * </p>
     *
     * @param delegateTask 任务委托对象
     * @return 总部审核人账号
     */
    private String getHqAuditor(DelegateTask delegateTask) {
        // 策略1：从流程变量获取指定的审核人
        Object hqAuditorObj = delegateTask.getVariable("hqAuditor");
        if (hqAuditorObj != null && StrUtil.isNotBlank(hqAuditorObj.toString())) {
            log.info("从流程变量获取总部审核人: {}", hqAuditorObj);
            return hqAuditorObj.toString();
        }

        // 策略2：从流程变量获取审核角色，查询该角色的第一个用户
        Object hqAuditRoleObj = delegateTask.getVariable("hqAuditRole");
        if (hqAuditRoleObj != null && StrUtil.isNotBlank(hqAuditRoleObj.toString())) {
            String roleName = hqAuditRoleObj.toString();
            log.info("从流程变量获取总部审核角色: {}", roleName);
            return getUserByRole(roleName);
        }

        // 策略3：使用默认配置（可以从配置表或常量中获取）
        log.info("使用默认总部审核人配置");
        return getDefaultHqAuditor();
    }

    /**
     * 根据角色获取用户
     *
     * @param roleName 角色名称
     * @return 用户账号
     */
    private String getUserByRole(String roleName) {
        try {
            // 通过反射获取 SysRoleMapper
            Class<?> roleMapperClass = Class.forName("com.ruoyi.system.mapper.SysRoleMapper");
            Object roleMapper = SpringUtils.getBean(roleMapperClass);

            // 查询角色列表（使用 QueryWrapper 而不是 LambdaQueryWrapper）
            java.lang.reflect.Method selectListMethod = roleMapperClass.getMethod("selectList",
                com.baomidou.mybatisplus.core.conditions.Wrapper.class);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> roleWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            roleWrapper.eq("role_name", roleName);
            roleWrapper.last("LIMIT 1");

            java.util.List<?> roleList = (java.util.List<?>) selectListMethod.invoke(roleMapper, roleWrapper);

            if (roleList == null || roleList.isEmpty()) {
                log.warn("角色[{}]不存在", roleName);
                return null;
            }

            Object role = roleList.get(0);

            // 获取角色ID
            java.lang.reflect.Method getRoleIdMethod = role.getClass().getMethod("getRoleId");
            Long roleId = (Long) getRoleIdMethod.invoke(role);

            // 查询该角色下的第一个用户
            Class<?> userRoleMapperClass = Class.forName("com.ruoyi.system.mapper.SysUserRoleMapper");
            Object userRoleMapper = SpringUtils.getBean(userRoleMapperClass);

            java.lang.reflect.Method selectListMethod2 = userRoleMapperClass.getMethod("selectList",
                com.baomidou.mybatisplus.core.conditions.Wrapper.class);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> userRoleWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            userRoleWrapper.eq("role_id", roleId);
            userRoleWrapper.last("LIMIT 1");

            java.util.List<?> userRoleList = (java.util.List<?>) selectListMethod2.invoke(userRoleMapper, userRoleWrapper);

            if (userRoleList != null && !userRoleList.isEmpty()) {
                Object userRole = userRoleList.get(0);
                java.lang.reflect.Method getUserIdMethod = userRole.getClass().getMethod("getUserId");
                Long userId = (Long) getUserIdMethod.invoke(userRole);

                // 查询用户信息
                Class<?> userMapperClass = Class.forName("com.ruoyi.system.mapper.SysUserMapper");
                Object userMapper = SpringUtils.getBean(userMapperClass);

                java.lang.reflect.Method selectByIdMethod = userMapperClass.getMethod("selectById", java.io.Serializable.class);
                Object user = selectByIdMethod.invoke(userMapper, userId);

                if (user != null) {
                    java.lang.reflect.Method getUserNameMethod = user.getClass().getMethod("getUserName");
                    return (String) getUserNameMethod.invoke(user);
                }
            }

            log.warn("角色[{}]下没有找到用户", roleName);
            return null;
        } catch (Exception e) {
            log.error("根据角色获取用户失败", e);
            return null;
        }
    }

    /**
     * 获取默认总部审核人
     * <p>
     * 可以从以下位置获取：
     * 1. 系统配置表
     * 2. 固定配置（如 admin）
     * 3. 财务部门负责人
     * </p>
     *
     * @return 默认总部审核人账号
     */
    private String getDefaultHqAuditor() {
        // 方式1：使用固定的管理员账号（示例）
        // return "admin";

        // 方式2：查询财务部门负责人（推荐）
        try {
            Class<?> deptMapperClass = Class.forName("com.ruoyi.system.mapper.SysDeptMapper");
            Object deptMapper = SpringUtils.getBean(deptMapperClass);

            // 查询财务部门（假设部门名称包含"财务"）
            java.lang.reflect.Method selectListMethod = deptMapperClass.getMethod("selectList",
                com.baomidou.mybatisplus.core.conditions.Wrapper.class);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> deptWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            deptWrapper.like("dept_name", "财务");
            deptWrapper.eq("status", "0");
            deptWrapper.last("LIMIT 1");

            java.util.List<?> deptList = (java.util.List<?>) selectListMethod.invoke(deptMapper, deptWrapper);

            if (deptList != null && !deptList.isEmpty()) {
                Object financeDept = deptList.get(0);
                java.lang.reflect.Method getLeaderMethod = financeDept.getClass().getMethod("getLeader");
                String leader = (String) getLeaderMethod.invoke(financeDept);
                if (StrUtil.isNotBlank(leader)) {
                    log.info("使用财务部门负责人作为总部审核人: {}", leader);
                    return leader;
                }
            }

            log.warn("未找到财务部门负责人，使用默认账号 admin");
            return "admin";
        } catch (Exception e) {
            log.error("获取默认总部审核人失败，使用 admin", e);
            return "admin";
        }
    }

    /**
     * 验证用户是否有效
     *
     * @param userName 用户账号
     */
    private void validateUser(String userName) {
        try {
            Class<?> userMapperClass = Class.forName("com.ruoyi.system.mapper.SysUserMapper");
            Object userMapper = SpringUtils.getBean(userMapperClass);

            java.lang.reflect.Method selectCountMethod = userMapperClass.getMethod("selectCount",
                com.baomidou.mybatisplus.core.conditions.Wrapper.class);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> userWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            userWrapper.eq("user_name", userName);
            userWrapper.eq("status", "0");

            Long count = (Long) selectCountMethod.invoke(userMapper, userWrapper);

            if (count == null || count == 0) {
                log.error("总部审核人[{}]不是有效用户或已禁用", userName);
                throw new RuntimeException("总部审核人[" + userName + "]不是有效用户或已禁用");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证用户失败", e);
            throw new RuntimeException("验证总部审核人失败: " + e.getMessage(), e);
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

                // 更新状态为 HQ_Pending（总部审核中）
                java.lang.reflect.Method setStatusMethod = budgetSheet.getClass().getMethod("setStatus", String.class);
                setStatusMethod.invoke(budgetSheet, "HQ_Pending");

                // 保存更新
                java.lang.reflect.Method updateByIdMethod = mapperClass.getMethod("updateById", Object.class);
                updateByIdMethod.invoke(budgetSheetMapper, budgetSheet);

                log.info("已更新预算单处理人为: {}, 状态: HQ_Pending", handler);
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
