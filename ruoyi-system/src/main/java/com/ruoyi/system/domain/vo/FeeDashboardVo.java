package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 老板看板视图对象（T5.10）
 * 总览 + 大类卡片 + 项目卡片 + 风险预警区 + 固定成本卡片 + 未归类池
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 总览 ==========
    /** 合同总额（进行中项目） */
    private BigDecimal totalContractAmount;
    /** 已核验收入合计 */
    private BigDecimal totalVerifiedIncome;
    /** 待核验收入合计 */
    private BigDecimal totalPendingIncome;
    /** 总支出合计 */
    private BigDecimal totalExpense;
    /** 实际利润合计（已核验收入 - 支出） */
    private BigDecimal totalActualProfit;
    /** 进行中项目数 */
    private Integer activeProjectCount;
    /** 待审批事项数（项目/付款/变更/分成） */
    private Integer pendingApprovalCount;

    // ========== 大类卡片 ==========
    /** 按会计大类支出 {"差旅招待":1234.56} */
    private List<CategoryExpenseVo> categoryExpenses;

    // ========== 风险预警区 ==========
    /** 烂账风险（OPEN/FOLLOWING） */
    private List<FeeBadDebtVo> badDebtWarnings;
    /** 合同变更待收款确认（含逾期） */
    private List<FeeProjectChangeVo> changeConfirmWarnings;

    // ========== 固定成本卡片（本月计提） ==========
    /** 薪资社保计提 */
    private BigDecimal monthlySalaryAccrual;
    /** 资产折旧计提 */
    private BigDecimal monthlyDepreciation;
    /** 贷款利息计提 */
    private BigDecimal monthlyLoanInterest;
    /** 计提月份 */
    private String accrualMonth;

    // ========== 未归类池 ==========
    /** 未关联项目（公司级）报销单数 */
    private Integer unclassifiedBillCount;
    /** 未关联项目报销金额 */
    private BigDecimal unclassifiedBillAmount;

    /**
     * 大类支出卡片
     */
    @Data
    public static class CategoryExpenseVo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 会计大类名称 */
        private String categoryName;
        /** 支出金额 */
        private BigDecimal amount;
        /** 单据数 */
        private Integer billCount;
    }
}
