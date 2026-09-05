package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IBudgetPreparationService;
import com.ruoyi.system.service.IDeptBudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门级预算Service实现（T6.1 / FEAT_DEPT_BUDGET）
 * 复用既有预算编制闭环（编制→审核→批准），通过 department_id 区分部门粒度
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptBudgetServiceImpl implements IDeptBudgetService {

    private final BudgetPreparationMapper budgetPreparationMapper;
    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;
    private final BudgetSubjectMapper budgetSubjectMapper;
    private final ExpenseBillMapper expenseBillMapper;
    private final IBudgetPreparationService budgetPreparationService;

    @Override
    public Page<BudgetPreparationVo> deptPageList(Long departmentId, Integer budgetYear, Integer budgetMonth,
                                                  String status, PageQuery pageQuery) {
        return budgetPreparationMapper.selectVoPage(pageQuery.build(),
            Wrappers.<com.ruoyi.system.domain.BudgetPreparation>lambdaQuery()
                .isNotNull(com.ruoyi.system.domain.BudgetPreparation::getDepartmentId)
                .eq(departmentId != null, com.ruoyi.system.domain.BudgetPreparation::getDepartmentId, departmentId)
                .eq(budgetYear != null, com.ruoyi.system.domain.BudgetPreparation::getBudgetYear, budgetYear)
                .eq(budgetMonth != null, com.ruoyi.system.domain.BudgetPreparation::getBudgetMonth, budgetMonth)
                .eq(StrUtil.isNotBlank(status), com.ruoyi.system.domain.BudgetPreparation::getStatus, status)
                .orderByDesc(com.ruoyi.system.domain.BudgetPreparation::getBudgetYear)
                .orderByDesc(com.ruoyi.system.domain.BudgetPreparation::getBudgetMonth));
    }

    @Override
    public Boolean createDeptBudget(BudgetPreparationBo bo) {
        if (bo.getDepartmentId() == null) {
            throw new ServiceException("部门级预算编制必须选择部门");
        }
        return budgetPreparationService.insertByBo(bo);
    }

    @Override
    public List<Map<String, Object>> deptQuotaSummary(Long departmentId, Integer budgetYear, Integer budgetMonth) {
        if (departmentId == null) {
            throw new ServiceException("请选择部门");
        }
        List<BudgetSubject> subjects = budgetSubjectMapper.selectList(Wrappers.<BudgetSubject>lambdaQuery()
            .eq(BudgetSubject::getIsLeaf, 1)
            .eq(BudgetSubject::getIsActive, 1)
            .orderByAsc(BudgetSubject::getSubjectCode));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BudgetSubject subject : subjects) {
            BigDecimal totalBudget = budgetPreparationDetailMapper.sumBudgetBySubjectAndPeriod(
                subject.getSubjectCode(), departmentId, budgetYear, budgetMonth);
            BigDecimal usedAmount = expenseBillMapper.sumApprovedAmountByDeptAndSubject(
                departmentId, subject.getSubjectCode());
            totalBudget = totalBudget != null ? totalBudget : BigDecimal.ZERO;
            usedAmount = usedAmount != null ? usedAmount : BigDecimal.ZERO;
            // 仅展示有编制或有消耗的科目
            if (totalBudget.compareTo(BigDecimal.ZERO) == 0 && usedAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subjectCode", subject.getSubjectCode());
            row.put("subjectName", subject.getSubjectName());
            row.put("totalBudget", totalBudget);
            row.put("usedAmount", usedAmount);
            row.put("remaining", totalBudget.subtract(usedAmount));
            rows.add(row);
        }
        return rows;
    }
}
