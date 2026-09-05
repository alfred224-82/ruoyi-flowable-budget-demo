package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.FeeExpenseCategory;
import com.ruoyi.system.domain.vo.BudgetCategoryQuotaItemVo;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeExpenseCategoryMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ICategoryBudgetService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用大类预算管控服务实现（T5.17）
 * <p>
 * 大类额度硬拦截 + 85%🟡/95%🟠/100%🔴 三级预警（设计文档 2.2 #4）。
 * 大类额度取当期预算编制单 category_quotas（优先已审批单，其次最近更新单）。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryBudgetServiceImpl implements ICategoryBudgetService {

    private final BudgetPreparationMapper preparationMapper;
    private final ExpenseBillMapper expenseBillMapper;
    private final FeeExpenseCategoryMapper feeExpenseCategoryMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ISysMessageService sysMessageService;

    @Override
    public BigDecimal getCategoryQuota(Long deptId, String categoryCode, Integer budgetYear, Integer budgetMonth) {
        if (deptId == null || StrUtil.isBlank(categoryCode)) {
            return null;
        }
        // 优先取当期已审批编制单，无则取最近更新单（含草稿，老板已录额度即生效）
        // 年月+部门维度：按 departmentId 查询编制单
        BudgetPreparation preparation = preparationMapper.selectOne(Wrappers.<BudgetPreparation>lambdaQuery()
            .eq(BudgetPreparation::getDepartmentId, deptId)
            .eq(BudgetPreparation::getBudgetYear, budgetYear)
            .eq(BudgetPreparation::getBudgetMonth, budgetMonth)
            .eq(BudgetPreparation::getStatus, "Approved")
            .orderByDesc(BudgetPreparation::getUpdateTime)
            .last("LIMIT 1"));
        if (preparation == null) {
            preparation = preparationMapper.selectOne(Wrappers.<BudgetPreparation>lambdaQuery()
                .eq(BudgetPreparation::getDepartmentId, deptId)
                .eq(BudgetPreparation::getBudgetYear, budgetYear)
                .eq(BudgetPreparation::getBudgetMonth, budgetMonth)
                .isNotNull(BudgetPreparation::getCategoryQuotas)
                .orderByDesc(BudgetPreparation::getUpdateTime)
                .orderByDesc(BudgetPreparation::getId)
                .last("LIMIT 1"));
        }
        if (preparation == null || StrUtil.isBlank(preparation.getCategoryQuotas())) {
            return null;
        }
        List<BudgetCategoryQuotaItemVo> quotas =
            JsonUtils.parseArray(preparation.getCategoryQuotas(), BudgetCategoryQuotaItemVo.class);
        if (quotas == null) {
            return null;
        }
        for (BudgetCategoryQuotaItemVo item : quotas) {
            if (categoryCode.equals(item.getCategoryCode())) {
                return item.getAmount();
            }
        }
        return null;
    }

    @Override
    public CategoryCheckResult checkCategoryBudget(Long deptId, String categoryCode, BigDecimal amount,
                                                   Integer budgetYear, Integer budgetMonth) {
        CategoryCheckResult result = new CategoryCheckResult();
        if (deptId == null || StrUtil.isBlank(categoryCode) || amount == null) {
            return result;
        }
        BigDecimal quota = getCategoryQuota(deptId, categoryCode, budgetYear, budgetMonth);
        if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
            // 未编制大类额度：不拦截（保持既有科目级管控行为）
            result.setMessage("大类未配置预算额度，跳过大类校验");
            return result;
        }

        BigDecimal used = expenseBillMapper.sumUsedAmountByCategory(deptId, categoryCode, budgetYear, budgetMonth);
        if (used == null) {
            used = BigDecimal.ZERO;
        }
        result.setQuota(quota);
        result.setUsed(used);

        BigDecimal after = used.add(amount);
        BigDecimal usageRate = after.multiply(new BigDecimal("100"))
            .divide(quota, 2, RoundingMode.HALF_UP);
        result.setUsageRate(usageRate);

        String categoryName = resolveCategoryName(categoryCode);

        // 硬拦截：超额直接阻断提交（设计文档 2.2 #4：大类硬拦截）
        if (after.compareTo(quota) > 0) {
            result.setPassed(false);
            result.setWarningLevel(100);
            result.setMessage("费用大类[" + categoryName + "]额度已用尽：额度" + quota
                + "，已用" + used + "，本次" + amount + "，超出" + after.subtract(quota) + "，提交被拦截");
            throw new ServiceException(result.getMessage());
        }

        // 三级预警：85%🟡 / 95%🟠 / 100%🔴（每单触发一次消息，通知老板+财务）
        int level = 0;
        if (usageRate.compareTo(new BigDecimal("100")) >= 0) {
            level = 100;
        } else if (usageRate.compareTo(new BigDecimal("95")) >= 0) {
            level = 95;
        } else if (usageRate.compareTo(new BigDecimal("85")) >= 0) {
            level = 85;
        }
        if (level > 0) {
            result.setWarningLevel(level);
            String icon = level == 100 ? "🔴" : level == 95 ? "🟠" : "🟡";
            result.setMessage("费用大类[" + categoryName + "]预算执行率达 " + usageRate + "%（" + icon + level + "% 预警线）");
            sendWarningMessage(categoryName, usageRate, level, budgetYear, budgetMonth);
        }
        result.setPassed(true);
        return result;
    }

    /**
     * 大类名称解析（主数据缺失时回退编码）
     */
    private String resolveCategoryName(String categoryCode) {
        try {
            FeeExpenseCategory category = feeExpenseCategoryMapper.selectOne(
                Wrappers.<FeeExpenseCategory>lambdaQuery()
                    .eq(FeeExpenseCategory::getCategoryCode, categoryCode)
                    .last("LIMIT 1"));
            return category != null ? category.getCategoryName() : categoryCode;
        } catch (Exception e) {
            return categoryCode;
        }
    }

    /**
     * 三级预警消息：发送给老板与财务角色用户（合并发送，失败不阻断主流程）
     */
    private void sendWarningMessage(String categoryName, BigDecimal usageRate, int level,
                                    Integer budgetYear, Integer budgetMonth) {
        try {
            List<Long> userIds = new ArrayList<>();
            for (String roleKey : new String[]{"boss", "finance"}) {
                SysRole role = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getRoleKey, roleKey)
                    .eq(SysRole::getStatus, "0"));
                if (role != null) {
                    List<Long> ids = sysUserRoleMapper.selectUserIdsByRoleId(role.getRoleId());
                    if (ids != null) {
                        userIds.addAll(ids);
                    }
                }
            }
            if (userIds.isEmpty()) {
                return;
            }
            String title = "预算执行" + level + "%预警";
            String content = "费用大类[" + categoryName + "]在 " + budgetYear + "年" + budgetMonth
                + "月预算执行率已达 " + usageRate + "%（" + level + "% 预警线），请及时关注。";
            sysMessageService.sendBatchMessage(userIds.stream().distinct()
                .collect(java.util.stream.Collectors.toList()), title, content,
                "SYSTEM_WARNING", "CATEGORY_BUDGET", null);
            log.info("大类预算预警已发送: category={}, rate={}%, level={}", categoryName, usageRate, level);
        } catch (Exception e) {
            log.warn("大类预算预警消息发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    /**
     * T5.13 预算金额调整下限控制：查询各大类使用率统计
     * <p>used = 已审批 + 在途占用；usageRate = used / quota × 100；
     * minAmount = quota × usageRate / 100 = used（即新金额不得低于已用金额）。
     */
    @Override
    public Map<String, Map<String, BigDecimal>> getCategoryUsageStats(Long deptId, Integer budgetYear, Integer budgetMonth) {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        if (deptId == null || budgetYear == null || budgetMonth == null) {
            return result;
        }
        // 查询当期编制单上的各大类额度（按 departmentId 查询，年月+部门维度）
        BudgetPreparation preparation = preparationMapper.selectOne(Wrappers.<BudgetPreparation>lambdaQuery()
            .eq(BudgetPreparation::getDepartmentId, deptId)
            .eq(BudgetPreparation::getBudgetYear, budgetYear)
            .eq(BudgetPreparation::getBudgetMonth, budgetMonth)
            .isNotNull(BudgetPreparation::getCategoryQuotas)
            .orderByDesc(BudgetPreparation::getUpdateTime)
            .orderByDesc(BudgetPreparation::getId)
            .last("LIMIT 1"));
        if (preparation == null || StrUtil.isBlank(preparation.getCategoryQuotas())) {
            return result;
        }
        List<BudgetCategoryQuotaItemVo> quotas =
            JsonUtils.parseArray(preparation.getCategoryQuotas(), BudgetCategoryQuotaItemVo.class);
        if (quotas == null || quotas.isEmpty()) {
            return result;
        }
        for (BudgetCategoryQuotaItemVo item : quotas) {
            String code = item.getCategoryCode();
            BigDecimal quota = item.getAmount();
            if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal used = expenseBillMapper.sumUsedAmountByCategory(deptId, code, budgetYear, budgetMonth);
            if (used == null) {
                used = BigDecimal.ZERO;
            }
            BigDecimal usageRate = used.multiply(new BigDecimal("100"))
                .divide(quota, 2, RoundingMode.HALF_UP);
            // 调整下限 = 已用金额（即 quota × usageRate / 100 = used）
            BigDecimal minAmount = used;

            Map<String, BigDecimal> stats = new HashMap<>();
            stats.put("quota", quota);
            stats.put("used", used);
            stats.put("usageRate", usageRate);
            stats.put("minAmount", minAmount);
            result.put(code, stats);
        }
        return result;
    }
}
