package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.vo.BudgetCheckResultVo;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IBudgetCheckService;
import com.ruoyi.system.service.IElasticQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 预算校验Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetCheckServiceImpl implements IBudgetCheckService {

    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;
    private final ExpenseBillMapper expenseBillMapper;
    // M6 T6.1 弹性额度（FEAT_ELASTIC_QUOTA）：部门大类超支阈值内预警放行，开关关闭时内部返回 null 自动退化
    private final IElasticQuotaService elasticQuotaService;

    @Override
    public BudgetCheckResultVo checkBudget(Long deptId, String budgetSubjectCode, BigDecimal amount,
                                            Integer budgetYear, Integer budgetMonth) {
        BudgetCheckResultVo result = new BudgetCheckResultVo();

        // 1. 查询预算编制金额（按部门+期间隔离）
        BigDecimal totalBudget = budgetPreparationDetailMapper.sumBudgetBySubjectAndPeriod(
            budgetSubjectCode, deptId, budgetYear, budgetMonth);
        if (totalBudget == null) {
            totalBudget = BigDecimal.ZERO;
        }
        // 2. 查询已使用金额（该部门 + 该科目已审批通过的报销单合计）
        BigDecimal usedAmount = expenseBillMapper.sumApprovedAmountByDeptAndSubject(deptId, budgetSubjectCode);
        if (usedAmount == null) {
            usedAmount = BigDecimal.ZERO;
        }
        // 3. 剩余预算 = 编制金额 - 已使用
        BigDecimal remaining = totalBudget.subtract(usedAmount);

        result.setTotalBudget(totalBudget);
        result.setUsedAmount(usedAmount);
        result.setRemaining(remaining);
        result.setThisAmount(amount);

        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            // 场景3：该科目无预算 → 阻断
            result.setPassed(false);
            result.setOverBudget(false);
            result.setMessage("该科目尚未编制预算，需先联系财务编制");
        } else if (remaining.compareTo(amount) < 0) {
            // T6.1 弹性额度：部门大类超支但在弹性阈值内 → 预警放行（不拦截）
            BigDecimal elasticPct = elasticQuotaService.getThresholdPct(null);
            if (elasticPct != null && totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal elasticLimit = totalBudget.multiply(
                    BigDecimal.ONE.add(elasticPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
                if (usedAmount.add(amount).compareTo(elasticLimit) <= 0) {
                    result.setPassed(true);
                    result.setOverBudget(false);
                    result.setMessage("部门额度已超支但在弹性阈值(+" + elasticPct + "%)内，预警放行。含弹性剩余：" + elasticLimit.subtract(usedAmount));
                    log.warn("弹性额度预警放行: deptId={}, subject={}, pct={}%, 本次={}",
                        deptId, budgetSubjectCode, elasticPct, amount);
                    return result;
                }
            }
            // 场景2：预算不足 → 触发超预算审批流程
            result.setPassed(false);
            result.setOverBudget(true);
            BigDecimal shortage = amount.subtract(remaining);
            result.setShortageAmount(shortage);
            result.setMessage("预算余额不足，剩余：" + remaining + "，本次申请：" + amount + "，缺口：" + shortage + "。单据将提交至上级审批人进行审批。");
        } else {
            // 场景1：预算充足 → 正常通过
            result.setPassed(true);
            result.setOverBudget(false);
            result.setMessage("预算充足，剩余：" + remaining.subtract(amount));
        }
        return result;
    }
}
