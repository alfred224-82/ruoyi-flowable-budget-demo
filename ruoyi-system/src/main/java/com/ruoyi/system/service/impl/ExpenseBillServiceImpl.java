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
import com.ruoyi.system.domain.vo.PurposeTagVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.OverBudgetApprovalMapper;
import com.ruoyi.system.service.IBudgetCheckService;
import com.ruoyi.system.service.IExpenseBillService;
import com.ruoyi.system.service.IFeeOperationLogService;
import com.ruoyi.system.service.IMappingRuleService;
import com.ruoyi.system.service.IPurposeTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
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
    private final IPurposeTagService purposeTagService;
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
        // 初始状态为草稿
        bill.setStatus("DRAFT");
        bill.setAllocationStatus("NONE");
        bill.setIsOverBudget(0);
        bill.setIsFixedAsset(0);
        bill.setExpenditureType("OPERATING");

        expenseBillMapper.insert(bill);
        return bill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseBill submitBill(Long id) {
        ExpenseBill bill = expenseBillMapper.selectById(id);
        if (bill == null) {
            throw new ServiceException("报销单不存在");
        }
        if (!"DRAFT".equals(bill.getStatus())) {
            throw new ServiceException("只有草稿状态可以提交");
        }

        // 1. 用途标签校验：项目交付 → 项目编号必填
        PurposeTagVo tag = purposeTagService.queryByCode(bill.getPurposeTag());
        if (tag != null && tag.getRequireProject() == 1
            && StrUtil.isBlank(bill.getProjectCode())) {
            throw new ServiceException("用途标签为「项目交付」时，项目编号必填");
        }

        // 2. 映射规则匹配
        MappingRuleVo rule = mappingRuleService.matchRule(
            bill.getItemCategory(), bill.getPurposeTag(), bill.getDeptId());

        // 3. 填充系统计算字段
        bill.setCostCenterCode(rule.getCostCenterGroup());
        bill.setCalcBudgetSubject(rule.getBudgetSubject());
        bill.setCalcAccountingClass(rule.getAccountingClass());
        bill.setIsFixedAsset(rule.getIsFixedAsset());
        bill.setExpenditureType(rule.getIsFixedAsset() == 1 ? "CAPITAL" : "OPERATING");

        // 4. 预算校验（含超预算分流）
        BudgetCheckResultVo checkResult = budgetCheckService.checkBudget(
            bill.getDeptId(), bill.getCalcBudgetSubject(), bill.getAmount(),
            LocalDate.now().getYear(), LocalDate.now().getMonthValue());

        if (!checkResult.getPassed()) {
            if (checkResult.getOverBudget() != null && checkResult.getOverBudget()) {
                // 超预算：创建超预算审批单，单据状态变为 OVER_BUDGET_PENDING
                OverBudgetApproval approval = new OverBudgetApproval();
                approval.setBillId(bill.getId());
                approval.setCostCenterGroup(bill.getCostCenterCode());
                approval.setBudgetSubject(bill.getCalcBudgetSubject());
                approval.setOriginalBudget(checkResult.getTotalBudget());
                approval.setUsedAmount(checkResult.getUsedAmount());
                approval.setRequestAmount(bill.getAmount());
                approval.setShortageAmount(checkResult.getShortageAmount());
                approval.setStatus("PENDING");
                overBudgetApprovalMapper.insert(approval);

                bill.setIsOverBudget(1);
                bill.setOverBudgetApprovalId(approval.getId());
                bill.setStatus("OVER_BUDGET_PENDING");
                expenseBillMapper.updateById(bill);
                return bill;
            } else {
                // 无预算或其他原因 → 阻断
                throw new ServiceException(checkResult.getMessage());
            }
        }

        // 5. 更新状态
        bill.setStatus("SUBMITTED");
        expenseBillMapper.updateById(bill);

        // 写操作日志
        feeOperationLogService.log("EXPENSE", "SUBMIT", bill.getId(), bill.getBillNo(),
            null, "DRAFT", "SUBMITTED", null);

        return bill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        // 只能删除草稿状态的单据
        for (Long id : ids) {
            ExpenseBill bill = expenseBillMapper.selectById(id);
            if (bill != null && !"DRAFT".equals(bill.getStatus())) {
                throw new ServiceException("只能删除草稿状态的报销单");
            }
            // 写操作日志
            if (bill != null) {
                feeOperationLogService.log("EXPENSE", "DELETE", bill.getId(), bill.getBillNo(),
                    null, "DRAFT", "DELETED", null);
            }
        }
        return expenseBillMapper.deleteBatchIds(ids) > 0;
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
