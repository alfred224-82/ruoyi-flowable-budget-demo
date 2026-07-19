package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.OverBudgetApproval;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.BudgetCheckResultVo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.OverBudgetApprovalMapper;
import com.ruoyi.system.service.IBudgetCheckService;
import com.ruoyi.system.service.IExpenseBillService;
import com.ruoyi.system.service.IFeeOperationLogService;
import com.ruoyi.system.service.IMappingRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 报销单Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseBillServiceImpl implements IExpenseBillService {

    private final ExpenseBillMapper expenseBillMapper;
    private final IMappingRuleService mappingRuleService;
    private final IBudgetCheckService budgetCheckService;
    private final OverBudgetApprovalMapper overBudgetApprovalMapper;
    private final IFeeOperationLogService feeOperationLogService;

    @Override
    public ExpenseBillVo queryById(Long id) {
        return expenseBillMapper.selectVoById(id);
    }

    @Override
    public Page<ExpenseBillVo> selectPageList(ExpenseBillBo bo, PageQuery pageQuery) {
        Wrapper<ExpenseBill> wrapper = buildQueryWrapper(bo);
        return expenseBillMapper.selectExpenseBillPage(pageQuery.build(), wrapper);
    }

    /**
     * 新增报销单（创建即提交，无草稿状态）
     * 一步完成：创建 → 映射规则匹配 → 预算校验 → 提交/超预算分流
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseBill insertByBo(ExpenseBillBo bo) {
        ExpenseBill bill = BeanCopyUtils.copy(bo, ExpenseBill.class);
        // 生成报销单号
        bill.setBillNo(generateBillNo());
        // 设置当前用户部门
        bill.setDeptId(LoginHelper.getDeptId());
        bill.setApplicantId(LoginHelper.getUserId());
        bill.setApplicantName(LoginHelper.getNickName());
        bill.setAllocationStatus("NONE");
        bill.setIsOverBudget(0);

        // 兜底：如果前端未传报销年月，从费用日期自动推导（格式 YYYY-MM）
        if (StrUtil.isBlank(bill.getExpensePeriod()) && bill.getExpenseDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            bill.setExpensePeriod(sdf.format(bill.getExpenseDate()));
        }

        // 1. 映射规则匹配
        MappingRuleVo rule = mappingRuleService.matchRule(
            bill.getItemCategory(), bill.getPurposeTag(), bill.getDeptId());

        // 2. 填充系统计算字段
        bill.setCalcBudgetSubject(rule.getBudgetSubject());
        bill.setCalcAccountingClass(rule.getAccountingClass());
        bill.setIsFixedAsset(rule.getIsFixedAsset());
        bill.setExpenditureType(rule.getIsFixedAsset() != null && rule.getIsFixedAsset() == 1
            ? "CAPITAL" : "OPERATING");

        // 2.1 固定资产校验：使用年限必填
        if (bill.getIsFixedAsset() != null && bill.getIsFixedAsset() == 1) {
            if (bill.getUsefulLife() == null || bill.getUsefulLife() <= 0) {
                throw new ServiceException("该报销单属于固定资产，必须填写使用年限（年）");
            }
        }

        // 3. 预算校验（含超预算分流）——使用报销年月而非当前日期
        int budgetYear;
        int budgetMonth;
        if (StrUtil.isNotBlank(bill.getExpensePeriod()) && bill.getExpensePeriod().length() >= 7) {
            String[] parts = bill.getExpensePeriod().split("-");
            budgetYear = Integer.parseInt(parts[0]);
            budgetMonth = Integer.parseInt(parts[1]);
        } else {
            LocalDate now = LocalDate.now();
            budgetYear = now.getYear();
            budgetMonth = now.getMonthValue();
        }
        BudgetCheckResultVo checkResult = budgetCheckService.checkBudget(
            bill.getDeptId(), bill.getCalcBudgetSubject(), bill.getAmount(),
            budgetYear, budgetMonth);

        if (!checkResult.getPassed()) {
            if (checkResult.getOverBudget() != null && checkResult.getOverBudget()) {
                // 超预算：创建超预算审批单，单据状态变为 OVER_BUDGET_PENDING
                bill.setStatus("OVER_BUDGET_PENDING");
                expenseBillMapper.insert(bill);

                OverBudgetApproval approval = new OverBudgetApproval();
                approval.setBillId(bill.getId());
                approval.setBillNo(bill.getBillNo());
                approval.setBudgetSubject(bill.getCalcBudgetSubject());
                approval.setOriginalBudget(checkResult.getTotalBudget());
                approval.setUsedAmount(checkResult.getUsedAmount());
                approval.setRequestAmount(bill.getAmount());
                approval.setShortageAmount(checkResult.getShortageAmount());
                approval.setStatus("PENDING");
                overBudgetApprovalMapper.insert(approval);

                bill.setIsOverBudget(1);
                bill.setOverBudgetApprovalId(approval.getId());
                expenseBillMapper.updateById(bill);

                feeOperationLogService.log("EXPENSE", "SUBMIT", bill.getId(), bill.getBillNo(),
                    null, "NEW", "OVER_BUDGET_PENDING", null);
                return bill;
            } else {
                // 无预算或其他原因 → 阻断
                throw new ServiceException(checkResult.getMessage());
            }
        }

        // 4. 正常提交
        bill.setStatus("SUBMITTED");
        expenseBillMapper.insert(bill);

        // 写操作日志
        feeOperationLogService.log("EXPENSE", "SUBMIT", bill.getId(), bill.getBillNo(),
            null, "NEW", "SUBMITTED", null);

        return bill;
    }

    /**
     * 构建查询条件
     */
    private Wrapper<ExpenseBill> buildQueryWrapper(ExpenseBillBo bo) {
        return Wrappers.<ExpenseBill>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getItemCategory()), ExpenseBill::getItemCategory, bo.getItemCategory())
            .eq(StrUtil.isNotBlank(bo.getPurposeTag()), ExpenseBill::getPurposeTag, bo.getPurposeTag())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ExpenseBill::getStatus, bo.getStatus());
    }

    /**
     * 生成报销单号
     */
    private String generateBillNo() {
        return "EB-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
