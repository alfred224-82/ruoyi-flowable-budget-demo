package com.ruoyi.workflow.listener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.system.domain.BudgetSheet;
import com.ruoyi.system.mapper.BudgetSheetMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预算提交初始化监听器
 * <p>
 * 功能：
 * - 自动生成预算编号（格式：BG-YYYYMM-XXX）
 * - 初始化预算单状态为 Draft
 * - 设置当前处理人
 * - 记录提交时间
 * </p>
 *
 * @author RuoYi
 * @since 2026/05/31
 */
@Slf4j
@Component(value = "budgetInitListener")
public class BudgetInitListener implements ExecutionListener {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BudgetSheetMapper budgetSheetMapper;

    /**
     * 预算单号生成计数器（线程安全）
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("========== 预算初始化监听器触发 ==========");
        log.info("事件类型: {}", eventName);
        log.info("流程实例ID: {}", processInstanceId);

        try {
            if (!ExecutionListener.EVENTNAME_START.equals(eventName)) {
                log.warn("预算初始化监听器只在 start 事件中执行，当前事件: {}", eventName);
                return;
            }

            handleBudgetInit(execution);

            log.info("========== 预算初始化监听器完成 ==========");
        } catch (Exception e) {
            log.error("预算初始化失败，流程实例ID: {}", processInstanceId, e);
            throw new RuntimeException("预算初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理预算初始化逻辑
     *
     * @param execution 执行委托对象
     */
    private void handleBudgetInit(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        log.info("开始初始化预算单数据");

        // 从流程变量中获取业务数据
        Object budgetSheetIdObj = execution.getVariable("budgetSheetId");
        if (budgetSheetIdObj == null) {
            log.warn("未找到预算单ID，跳过初始化");
            return;
        }

        Long budgetSheetId = convertToLong(budgetSheetIdObj);

        // 查询预算单
        BudgetSheet budgetSheet = budgetSheetMapper.selectById(budgetSheetId);
        if (budgetSheet == null) {
            log.error("预算单不存在，ID: {}", budgetSheetId);
            throw new RuntimeException("预算单不存在");
        }

        // 1. 生成预算单号
        String sheetNo = generateSheetNo(budgetSheet);
        budgetSheet.setSheetNo(sheetNo);
        log.info("生成预算单号: {}", sheetNo);

        // 2. 初始化状态
        budgetSheet.setStatus("Draft");
        log.info("设置预算单状态: Draft");

        // 3. 设置当前处理人（从创建者获取）
        String createBy = budgetSheet.getCreateBy();
        if (StrUtil.isNotBlank(createBy)) {
            execution.setVariable("currentHandler", createBy);
            budgetSheet.setCurrentHandler(createBy);
            log.info("设置当前处理人: {}", createBy);
        }

        // 4. 设置流程实例ID
        budgetSheet.setProcessInstanceId(processInstanceId);
        log.info("关联流程实例ID: {}", processInstanceId);

        // 5. 更新预算单
        int updateCount = budgetSheetMapper.updateById(budgetSheet);
        if (updateCount > 0) {
            log.info("预算单初始化成功，单号: {}", sheetNo);
        } else {
            log.error("预算单更新失败，ID: {}", budgetSheetId);
            throw new RuntimeException("预算单更新失败");
        }

        // 6. 将生成的单号设置为流程变量，方便后续使用
        execution.setVariable("sheetNo", sheetNo);
    }

    /**
     * 生成预算单号
     * 格式：BG-YYYYMM-XXX（XXX为当月序号）
     *
     * @param budgetSheet 预算单对象
     * @return 预算单号
     */
    private String generateSheetNo(BudgetSheet budgetSheet) {
        // 获取预算月份，如果没有则使用当前月份
        String budgetMonth = budgetSheet.getBudgetMonth();
        if (StrUtil.isBlank(budgetMonth)) {
            budgetMonth = DateUtil.format(new Date(), "yyyy-MM");
        }

        // 提取年月部分：YYYYMM
        String yearMonth = budgetMonth.replace("-", "").substring(0, 6);

        // 获取当月已存在的预算单数量
        LambdaQueryWrapper<BudgetSheet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(BudgetSheet::getSheetNo, "BG-" + yearMonth);
        long count = budgetSheetMapper.selectCount(queryWrapper);

        // 生成序号（从1开始，3位数字）
        int sequence = (int) (count + 1);
        String sequenceStr = String.format("%03d", sequence);

        // 组装单号：BG-YYYYMM-XXX
        return "BG-" + yearMonth + "-" + sequenceStr;
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
