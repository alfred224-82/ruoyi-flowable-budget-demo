package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 费用大类预算管控服务（T5.17）
 * <p>
 * 报销提交瞬间同步校验：大类额度硬拦截（设计文档 2.2 #4 / 2.3 ②）。
 * 大类额度来源：当期预算编制单 category_quotas（老板录入，T5.14 ①）；
 * 已用额度 = 已审批通过 + 在途占用（防拆单绕过）。
 * 预算执行 85%🟡/95%🟠/100%🔴 三级预警随校验结果一并产出。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
public interface ICategoryBudgetService {

    /**
     * 大类额度校验结果
     */
    class CategoryCheckResult {
        /** 是否通过（未配置大类额度时默认通过，不改变既有行为） */
        private boolean passed = true;
        /** 大类额度 */
        private BigDecimal quota;
        /** 已用额度（含在途占用） */
        private BigDecimal used;
        /** 校验后执行率（百分比，含本次金额） */
        private BigDecimal usageRate;
        /** 预警级别（85/95/100 阈值命中，null=未达预警） */
        private Integer warningLevel;
        /** 提示信息 */
        private String message;

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public BigDecimal getQuota() { return quota; }
        public void setQuota(BigDecimal quota) { this.quota = quota; }
        public BigDecimal getUsed() { return used; }
        public void setUsed(BigDecimal used) { this.used = used; }
        public BigDecimal getUsageRate() { return usageRate; }
        public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
        public Integer getWarningLevel() { return warningLevel; }
        public void setWarningLevel(Integer warningLevel) { this.warningLevel = warningLevel; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 查询指定部门当期大类的预算额度
     *
     * @param deptId       部门ID（编制单 org_id 维度）
     * @param categoryCode 费用大类编码
     * @param budgetYear   预算年度
     * @param budgetMonth  预算月份
     * @return 大类额度，未编制/未设置返回 null
     */
    BigDecimal getCategoryQuota(Long deptId, String categoryCode, Integer budgetYear, Integer budgetMonth);

    /**
     * 大类额度硬拦截校验（报销提交瞬间调用）
     * <p>超额直接抛 ServiceException 阻断提交；未配置额度时放行（保持既有行为）。
     * 校验后按 85/95/100 阈值触发三级预警消息（每单只发一次）。
     *
     * @param deptId       部门ID
     * @param categoryCode 费用大类编码
     * @param amount       本次报销金额
     * @param budgetYear   预算年度
     * @param budgetMonth  预算月份
     * @return 校验结果（含执行率与预警级别）
     */
    CategoryCheckResult checkCategoryBudget(Long deptId, String categoryCode, BigDecimal amount,
                                            Integer budgetYear, Integer budgetMonth);

    /**
     * 查询各大类预算使用率统计（T5.13 调整下限控制用）
     * <p>返回 Map：categoryCode → {used, usageRate, quota, minAmount}
     * <ul>
     *   <li>used：已用额度（已审批 + 在途占用）</li>
     *   <li>usageRate：使用率百分比（used / quota × 100）</li>
     *   <li>quota：当前大类预算额度</li>
     *   <li>minAmount：调整下限 = quota × usageRate / 100（即已用金额，T5.13 v1.8）</li>
     * </ul>
     *
     * @param deptId      部门ID
     * @param budgetYear  预算年度
     * @param budgetMonth 预算月份
     * @return 各大类使用率统计
     */
    Map<String, Map<String, BigDecimal>> getCategoryUsageStats(Long deptId, Integer budgetYear, Integer budgetMonth);
}
