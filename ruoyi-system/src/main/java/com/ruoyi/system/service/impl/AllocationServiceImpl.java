package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CostAllocation;
import com.ruoyi.system.domain.CostAllocationRule;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.mapper.CostAllocationMapper;
import com.ruoyi.system.mapper.CostAllocationRuleMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 费用分摊Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements IAllocationService {

    private final CostAllocationMapper allocationMapper;
    private final CostAllocationRuleMapper ruleMapper;
    private final ExpenseBillMapper expenseBillMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeMonthlyAllocation(int year, int month) {
        String batch = year + "-" + String.format("%02d", month);
        log.info("开始执行{}月份费用分摊", batch);

        // 1. 查询待分摊单据
        List<ExpenseBill> pendingBills = expenseBillMapper.selectList(
            Wrappers.<ExpenseBill>lambdaQuery()
                .eq(ExpenseBill::getCostCenterCode, "PENDING")
                .eq(ExpenseBill::getStatus, "APPROVED")
                .apply("DATE_FORMAT(expense_date, '%Y-%m') = {0}", batch));

        if (pendingBills.isEmpty()) {
            log.info("无待分摊单据，跳过");
            return;
        }

        // 2. 获取启用的分摊规则
        List<CostAllocationRule> rules = ruleMapper.selectList(
            Wrappers.<CostAllocationRule>lambdaQuery()
                .eq(CostAllocationRule::getStatus, "ENABLED"));

        BigDecimal totalWeight = rules.stream()
            .map(CostAllocationRule::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("分摊规则权重总和为0，请检查配置");
        }

        // 3. 逐笔分摊
        for (ExpenseBill bill : pendingBills) {
            BigDecimal allocatedTotal = BigDecimal.ZERO;
            for (int i = 0; i < rules.size(); i++) {
                CostAllocationRule rule = rules.get(i);
                BigDecimal ratio = rule.getWeight().divide(totalWeight, 4, RoundingMode.HALF_UP);
                BigDecimal allocAmount;
                if (i == rules.size() - 1) {
                    // 最后一个部门取差额，避免精度丢失
                    allocAmount = bill.getAmount().subtract(allocatedTotal);
                } else {
                    allocAmount = bill.getAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                    allocatedTotal = allocatedTotal.add(allocAmount);
                }

                CostAllocation allocation = new CostAllocation();
                allocation.setBillId(bill.getId());
                allocation.setAllocationBatch(batch);
                allocation.setSourceCostCenter("PENDING");
                allocation.setTargetDeptId(rule.getTargetDeptId());
                allocation.setTargetCostCenter(rule.getTargetCostCenter());
                allocation.setAllocAmount(allocAmount);
                allocation.setAllocRatio(ratio);
                allocation.setAllocRuleId(rule.getId());
                allocationMapper.insert(allocation);
            }

            // 更新报销单分摊状态
            bill.setAllocationStatus("ALLOCATED");
            expenseBillMapper.updateById(bill);
        }

        log.info("费用分摊完成，批次：{}，处理单据数：{}", batch, pendingBills.size());
    }
}
