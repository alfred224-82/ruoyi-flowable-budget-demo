package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeAssetDepreciation;
import com.ruoyi.system.domain.FeeBadDebt;
import com.ruoyi.system.domain.FeeIncomeConfirmation;
import com.ruoyi.system.domain.FeeLoanInterest;
import com.ruoyi.system.domain.FeePayment;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectChange;
import com.ruoyi.system.domain.FeeProjectMember;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.FeeSalaryRecord;
import com.ruoyi.system.domain.vo.FeeBadDebtVo;
import com.ruoyi.system.domain.vo.FeeDashboardVo;
import com.ruoyi.system.domain.vo.FeeLightOverviewVo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectChangeVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeAssetDepreciationMapper;
import com.ruoyi.system.mapper.FeeBadDebtMapper;
import com.ruoyi.system.mapper.FeeIncomeConfirmationMapper;
import com.ruoyi.system.mapper.FeeLoanInterestMapper;
import com.ruoyi.system.mapper.FeePaymentMapper;
import com.ruoyi.system.mapper.FeeProjectChangeMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectMemberMapper;
import com.ruoyi.system.mapper.FeeProjectSharingMapper;
import com.ruoyi.system.mapper.FeeSalaryRecordMapper;
import com.ruoyi.system.service.IFeeBossDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 老板看板Service实现（T5.10）
 * 数据实时聚合（基础版数据量下内存聚合），四层下钻：公司→大类→项目→明细
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeBossDashboardServiceImpl implements IFeeBossDashboardService {

    private final FeeProjectMapper projectMapper;
    private final FeePaymentMapper paymentMapper;
    private final FeeProjectChangeMapper changeMapper;
    private final FeeProjectSharingMapper sharingMapper;
    private final FeeBadDebtMapper badDebtMapper;
    private final FeeSalaryRecordMapper salaryRecordMapper;
    private final FeeAssetDepreciationMapper depreciationMapper;
    private final FeeLoanInterestMapper loanInterestMapper;
    private final ExpenseBillMapper billMapper;
    private final FeeProjectMemberMapper memberMapper;
    private final FeeIncomeConfirmationMapper incomeMapper;

    @Override
    public FeeDashboardVo getDashboard() {
        FeeDashboardVo vo = new FeeDashboardVo();
        List<FeeProject> projects = projectMapper.selectList(Wrappers.lambdaQuery());

        // 总览（进行中项目口径）
        List<FeeProject> actives = projects.stream()
            .filter(p -> "ACTIVE".equals(p.getStatus())).collect(Collectors.toList());
        vo.setActiveProjectCount(actives.size());
        vo.setTotalContractAmount(sum(actives, FeeProject::getCurrentContractAmount));
        vo.setTotalVerifiedIncome(sum(actives, FeeProject::getVerifiedIncome));
        vo.setTotalPendingIncome(sum(actives, FeeProject::getPendingIncome));
        vo.setTotalExpense(sum(actives, FeeProject::getTotalExpense));
        vo.setTotalActualProfit(vo.getTotalVerifiedIncome().subtract(vo.getTotalExpense()));

        // 待审批事项：立项 + 付款 + 合同变更 + 分成
        int pending = 0;
        pending += projects.stream().filter(p -> "PENDING_APPROVAL".equals(p.getStatus())).count();
        pending += paymentMapper.selectCount(Wrappers.<FeePayment>lambdaQuery().eq(FeePayment::getStatus, "PENDING"));
        pending += changeMapper.selectCount(Wrappers.<FeeProjectChange>lambdaQuery().eq(FeeProjectChange::getChangeStatus, "PENDING"));
        pending += sharingMapper.selectCount(Wrappers.<FeeProjectSharing>lambdaQuery().eq(FeeProjectSharing::getStatus, "PENDING"));
        vo.setPendingApprovalCount(pending);

        // 大类卡片：已审批报销单按会计大类聚合
        List<ExpenseBill> approvedBills = billMapper.selectList(
            Wrappers.<ExpenseBill>lambdaQuery().eq(ExpenseBill::getStatus, "APPROVED"));
        Map<String, List<ExpenseBill>> byCategory = approvedBills.stream()
            .collect(Collectors.groupingBy(b -> StrUtil.blankToDefault(b.getCalcAccountingClass(), "未分类")));
        List<FeeDashboardVo.CategoryExpenseVo> categoryExpenses = new ArrayList<>();
        byCategory.forEach((category, bills) -> {
            FeeDashboardVo.CategoryExpenseVo item = new FeeDashboardVo.CategoryExpenseVo();
            item.setCategoryName(category);
            item.setAmount(bills.stream().map(b -> zero(b.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setBillCount(bills.size());
            categoryExpenses.add(item);
        });
        categoryExpenses.sort(Comparator.comparing(FeeDashboardVo.CategoryExpenseVo::getAmount).reversed());
        vo.setCategoryExpenses(categoryExpenses);

        // 风险预警区：烂账（看板联动） + 合同变更待收款确认（含逾期）
        List<FeeBadDebtVo> badDebts = badDebtMapper.selectVoList(
            Wrappers.<FeeBadDebt>lambdaQuery()
                .in(FeeBadDebt::getStatus, "OPEN", "FOLLOWING")
                .orderByDesc(FeeBadDebt::getMarkedAt));
        fillBadDebtProjectName(badDebts);
        vo.setBadDebtWarnings(badDebts);
        List<FeeProjectChangeVo> changes = changeMapper.selectVoList(
            Wrappers.<FeeProjectChange>lambdaQuery()
                .in(FeeProjectChange::getChangeStatus, "APPROVED", "OVERDUE")
                .orderByAsc(FeeProjectChange::getApprovedAt));
        vo.setChangeConfirmWarnings(changes);

        // 固定成本卡片：本月计提
        String month = DateUtil.format(new Date(), "yyyy-MM");
        vo.setAccrualMonth(month);
        vo.setMonthlySalaryAccrual(sum(salaryRecordMapper.selectList(
            Wrappers.<FeeSalaryRecord>lambdaQuery().eq(FeeSalaryRecord::getAccrualMonth, month)),
            FeeSalaryRecord::getTotalAmount));
        vo.setMonthlyDepreciation(sum(depreciationMapper.selectList(
            Wrappers.<FeeAssetDepreciation>lambdaQuery().eq(FeeAssetDepreciation::getAccrualMonth, month)),
            FeeAssetDepreciation::getAmount));
        vo.setMonthlyLoanInterest(sum(loanInterestMapper.selectList(
            Wrappers.<FeeLoanInterest>lambdaQuery().eq(FeeLoanInterest::getAccrualMonth, month)),
            FeeLoanInterest::getAmount));

        // 未归类池：未关联项目（公司级）的已审批报销单
        List<ExpenseBill> unclassified = approvedBills.stream()
            .filter(b -> b.getProjectId() == null || b.getProjectId() == 0L)
            .collect(Collectors.toList());
        vo.setUnclassifiedBillCount(unclassified.size());
        vo.setUnclassifiedBillAmount(unclassified.stream()
            .map(b -> zero(b.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
        return vo;
    }

    @Override
    public List<FeeProjectVo> listProjectCards() {
        List<FeeProjectVo> list = projectMapper.selectVoList(Wrappers.<FeeProject>lambdaQuery()
            .in(FeeProject::getStatus, "ACTIVE", "COMPLETED"));
        // 按实际利润（已核验收入-支出）升序，风险项目靠前
        list.sort(Comparator.comparing(p -> zero(p.getVerifiedIncome()).subtract(zero(p.getTotalExpense()))));
        return list;
    }

    @Override
    public List<Map<String, Object>> drillDown(Integer level, String category, Long projectId) {
        int lv = level == null ? 1 : level;
        List<ExpenseBill> bills = billMapper.selectList(
            Wrappers.<ExpenseBill>lambdaQuery().eq(ExpenseBill::getStatus, "APPROVED"));
        List<FeePayment> payments = paymentMapper.selectList(
            Wrappers.<FeePayment>lambdaQuery().eq(FeePayment::getStatus, "APPROVED"));
        List<Map<String, Object>> result = new ArrayList<>();
        if (lv <= 1) {
            // 公司→大类：按会计大类聚合
            Map<String, BigDecimal> byCategory = new HashMap<>();
            bills.forEach(b -> byCategory.merge(StrUtil.blankToDefault(b.getCalcAccountingClass(), "未分类"),
                zero(b.getAmount()), BigDecimal::add));
            byCategory.forEach((name, amount) -> {
                Map<String, Object> row = new HashMap<>();
                row.put("name", name);
                row.put("amount", amount);
                result.add(row);
            });
            result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        } else if (lv == 2) {
            // 大类→项目：该大类支出按项目聚合
            Map<Long, BigDecimal> byProject = new HashMap<>();
            bills.stream().filter(b -> StrUtil.equals(b.getCalcAccountingClass(), category))
                .forEach(b -> byProject.merge(zeroProjectId(b.getProjectId()),
                    zero(b.getAmount()), BigDecimal::add));
            Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(
                    byProject.keySet().stream().filter(id -> id > 0).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(FeeProject::getId, p -> p));
            byProject.forEach((pid, amount) -> {
                Map<String, Object> row = new HashMap<>();
                FeeProject project = projectMap.get(pid);
                row.put("projectId", pid);
                row.put("name", project != null ? project.getProjectName() : "公司级（未归类）");
                row.put("amount", amount);
                result.add(row);
            });
        } else if (lv == 3) {
            // 项目→明细单据（报销单+付款单）
            bills.stream()
                .filter(b -> Objects.equals(zeroProjectId(b.getProjectId()), projectId)
                    && StrUtil.equals(b.getCalcAccountingClass(), category))
                .forEach(b -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("type", "EXPENSE_BILL");
                    row.put("no", b.getBillNo());
                    row.put("amount", zero(b.getAmount()));
                    row.put("date", b.getExpenseDate());
                    result.add(row);
                });
            if (category == null) {
                payments.stream().filter(p -> Objects.equals(p.getProjectId(), projectId))
                    .forEach(p -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("type", "PAYMENT");
                        row.put("no", p.getPaymentNo());
                        row.put("amount", zero(p.getAmount()));
                        row.put("date", p.getPaymentDate());
                        result.add(row);
                    });
            }
        } else {
            // 4层：单项目全量明细（报销单+付款单+收款）
            bills.stream().filter(b -> Objects.equals(zeroProjectId(b.getProjectId()), projectId))
                .forEach(b -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("type", "EXPENSE_BILL");
                    row.put("no", b.getBillNo());
                    row.put("category", b.getCalcAccountingClass());
                    row.put("amount", zero(b.getAmount()));
                    row.put("date", b.getExpenseDate());
                    result.add(row);
                });
            payments.stream().filter(p -> Objects.equals(p.getProjectId(), projectId))
                .forEach(p -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("type", "PAYMENT");
                    row.put("no", p.getPaymentNo());
                    row.put("amount", zero(p.getAmount()));
                    row.put("date", p.getPaymentDate());
                    result.add(row);
                });
        }
        return result;
    }

    @Override
    public List<FeeMemberLoadVo> memberLoad() {
        // 进行中项目口径：仅统计 ACTIVE 项目成员（已关闭/烂尾项目不计负载）
        List<FeeProject> actives = projectMapper.selectList(Wrappers.<FeeProject>lambdaQuery()
            .eq(FeeProject::getStatus, "ACTIVE"));
        if (actives.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, FeeProject> projectMap = actives.stream()
            .collect(Collectors.toMap(FeeProject::getId, p -> p, (a, b) -> a));
        List<FeeProjectMember> members = memberMapper.selectList(Wrappers.<FeeProjectMember>lambdaQuery()
            .eq(FeeProjectMember::getIsActive, 1)
            .in(FeeProjectMember::getProjectId, projectMap.keySet()));
        // 按用户聚合投入比例（手动维护），合计>100% 提示负载超限（仅提示不拦截）
        Map<Long, FeeMemberLoadVo> byUser = new HashMap<>();
        for (FeeProjectMember member : members) {
            FeeProject project = projectMap.get(member.getProjectId());
            if (project == null) {
                continue;
            }
            FeeMemberLoadVo vo = byUser.computeIfAbsent(member.getUserId(), uid -> {
                FeeMemberLoadVo created = new FeeMemberLoadVo();
                created.setUserId(uid);
                created.setUserName(member.getUserName());
                created.setProjectCount(0);
                created.setTotalRatio(0);
                created.setOverLoad(false);
                return created;
            });
            FeeMemberLoadVo.ProjectPart part = new FeeMemberLoadVo.ProjectPart();
            part.setProjectId(member.getProjectId());
            part.setProjectName(project.getProjectName());
            part.setRole(member.getRole());
            part.setInvestRatio(member.getInvestRatio() == null ? 0 : member.getInvestRatio());
            part.setProjectStatus(project.getStatus());
            vo.getProjects().add(part);
            vo.setTotalRatio(vo.getTotalRatio() + part.getInvestRatio());
            vo.setProjectCount(vo.getProjectCount() + 1);
        }
        List<FeeMemberLoadVo> result = new ArrayList<>(byUser.values());
        result.forEach(vo -> vo.setOverLoad(vo.getTotalRatio() > 100));
        // 负载超限靠前，其次按总投入降序（团队人力分布一眼看出谁超载）
        result.sort(Comparator.comparing(FeeMemberLoadVo::getOverLoad).reversed()
            .thenComparing(Comparator.comparing(FeeMemberLoadVo::getTotalRatio).reversed()));
        return result;
    }

    @Override
    public FeeLightOverviewVo getLightOverview() {
        FeeLightOverviewVo vo = new FeeLightOverviewVo();
        Date monthStart = DateUtil.beginOfMonth(new Date());
        Date monthEnd = DateUtil.endOfMonth(new Date());
        vo.setMonth(DateUtil.format(new Date(), "yyyy-MM"));

        // 本月收入：当月已核验收入确认合计（T5.4 三态，待核验不计入）
        BigDecimal income = sum(incomeMapper.selectList(
            Wrappers.<FeeIncomeConfirmation>lambdaQuery()
                .eq(FeeIncomeConfirmation::getVerifyStatus, "VERIFIED")
                .ge(FeeIncomeConfirmation::getVerifyAt, monthStart)
                .le(FeeIncomeConfirmation::getVerifyAt, monthEnd)),
            FeeIncomeConfirmation::getConfirmedAmount);

        // 本月支出：当月已审批报销单 + 付款单合计
        BigDecimal billExpense = sum(billMapper.selectList(
            Wrappers.<ExpenseBill>lambdaQuery()
                .eq(ExpenseBill::getStatus, "APPROVED")
                .ge(ExpenseBill::getApproveTime, monthStart)
                .le(ExpenseBill::getApproveTime, monthEnd)),
            ExpenseBill::getAmount);
        BigDecimal paymentExpense = sum(paymentMapper.selectList(
            Wrappers.<FeePayment>lambdaQuery()
                .eq(FeePayment::getStatus, "APPROVED")
                .ge(FeePayment::getApprovedAt, monthStart)
                .le(FeePayment::getApprovedAt, monthEnd)),
            FeePayment::getAmount);

        vo.setIncome(income);
        vo.setExpense(billExpense.add(paymentExpense));
        vo.setBalance(income.subtract(vo.getExpense()));
        return vo;
    }

    /**
     * 批量填充烂账项目名称
     */
    private void fillBadDebtProjectName(List<FeeBadDebtVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeBadDebtVo::getProjectId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(FeeProject::getId, p -> p));
        list.forEach(vo -> {
            FeeProject project = projectMap.get(vo.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        });
    }

    private <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream().map(getter).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private Long zeroProjectId(Long projectId) {
        return projectId == null ? 0L : projectId;
    }
}
