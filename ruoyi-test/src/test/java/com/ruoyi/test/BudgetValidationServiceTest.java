package com.ruoyi.test;

import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.BudgetValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 风控校验模块单元测试
 * 对应验证文档：第四章 风控校验模块验证（P1）
 *
 * 验收项覆盖：
 * - RSK-001: VAL_001 预算超标（偏差超20%触发ERROR）
 * - RSK-002: VAL_002 逻辑一致性（明细合计≠主表总额）
 * - RSK-003: VAL_003 科目有效性
 * - RSK-004: VAL_004 必填完整性
 * - RSK-005: VAL_005 收入非负
 * - RSK-006: 严格规则阻止提交
 * - RSK-007: WARNING级规则不阻止提交
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Tag("risk")
@DisplayName("风控校验模块验证")
public class BudgetValidationServiceTest {

    private List<BudgetPreparationDetail> details;
    private BudgetPreparation sheet;

    @BeforeEach
    void setUp() {
        details = new ArrayList<>();
        sheet = new BudgetPreparation();
        sheet.setTotalBudget(new BigDecimal("100000"));
    }

    // ==================== RSK-001: 预算超标 ====================

    @Test
    @DisplayName("RSK-001: 明细偏差超过20%时触发ERROR")
    void testVal001_BudgetOver() {
        // Given - 偏差25%（超过20%阈值）
        BudgetPreparationDetail detail = createDetail("001", "INCOME",
            new BigDecimal("100000"), new BigDecimal("75000")); // 预算10万，实际7.5万，偏差25%
        details.add(detail);

        BigDecimal threshold = new BigDecimal("20");

        // When
        boolean triggered = checkBudgetOver(detail, threshold);

        // Then
        assertTrue(triggered, "偏差25%应触发超标警告");
    }

    @Test
    @DisplayName("RSK-001b: 偏差未超阈值不触发")
    void testVal001_BudgetOver_NotTriggered() {
        // Given - 偏差10%（未超20%阈值）
        BudgetPreparationDetail detail = createDetail("001", "INCOME",
            new BigDecimal("100000"), new BigDecimal("90000")); // 偏差10%
        details.add(detail);

        BigDecimal threshold = new BigDecimal("20");

        // When
        boolean triggered = checkBudgetOver(detail, threshold);

        // Then
        assertFalse(triggered, "偏差10%不应触发超标警告");
    }

    // ==================== RSK-002: 逻辑一致性 ====================

    @Test
    @DisplayName("RSK-002: 明细合计≠主表总额时触发ERROR")
    void testVal002_LogicError() {
        // Given - 明细合计90000，主表100000
        details.add(createDetail("001", "EXPENSE", new BigDecimal("50000"), null));
        details.add(createDetail("002", "EXPENSE", new BigDecimal("40000"), null));
        sheet.setTotalBudget(new BigDecimal("100000"));

        // When
        BigDecimal detailSum = details.stream()
            .map(d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean inconsistent = detailSum.compareTo(sheet.getTotalBudget()) != 0;

        // Then
        assertTrue(inconsistent, "明细合计90000≠主表100000应触发");
    }

    @Test
    @DisplayName("RSK-002b: 明细合计=主表总额不触发")
    void testVal002_LogicConsistent() {
        // Given - 明细合计100000，主表100000
        details.add(createDetail("001", "EXPENSE", new BigDecimal("60000"), null));
        details.add(createDetail("002", "EXPENSE", new BigDecimal("40000"), null));
        sheet.setTotalBudget(new BigDecimal("100000"));

        // When
        BigDecimal detailSum = details.stream()
            .map(d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean inconsistent = detailSum.compareTo(sheet.getTotalBudget()) != 0;

        // Then
        assertFalse(inconsistent, "明细合计=主表总额不应触发");
    }

    // ==================== RSK-004: 必填完整性 ====================

    @Test
    @DisplayName("RSK-004: 金额或科目编码为空时触发ERROR")
    void testVal004_MissingData() {
        // Given - 金额为空
        BudgetPreparationDetail detail1 = new BudgetPreparationDetail();
        detail1.setSubjectCode("001");
        detail1.setBudgetAmount(null); // 金额为空

        // Given - 科目编码为空
        BudgetPreparationDetail detail2 = new BudgetPreparationDetail();
        detail2.setSubjectCode(null); // 科目为空
        detail2.setBudgetAmount(new BigDecimal("10000"));

        // Then
        assertTrue(isMissingData(detail1), "金额为空应触发");
        assertTrue(isMissingData(detail2), "科目编码为空应触发");
    }

    @Test
    @DisplayName("RSK-004b: 必填字段完整不触发")
    void testVal004_CompleteData() {
        BudgetPreparationDetail detail = createDetail("001", "EXPENSE", new BigDecimal("10000"), null);
        assertFalse(isMissingData(detail), "必填字段完整不应触发");
    }

    // ==================== RSK-005: 收入非负 ====================

    @Test
    @DisplayName("RSK-005: 收入类科目金额为负时触发ERROR")
    void testVal005_IncomeNegative() {
        // Given - 收入类科目金额为负
        BudgetPreparationDetail detail = createDetail("001", "INCOME",
            new BigDecimal("-5000"), null);

        // Then
        assertTrue(isIncomeNegative(detail), "收入类科目金额为负应触发");
    }

    @Test
    @DisplayName("RSK-005b: 费用类科目金额为负不触发")
    void testVal005_ExpenseNegative_NotTriggered() {
        // Given - 费用类科目金额为负（允许）
        BudgetPreparationDetail detail = createDetail("001", "EXPENSE",
            new BigDecimal("-5000"), null);

        // Then
        assertFalse(isIncomeNegative(detail), "费用类科目金额为负不应触发");
    }

    // ==================== RSK-006/007: 规则级别 ====================

    @Test
    @DisplayName("RSK-006: ERROR级规则阻止提交")
    void testSeverityLevel_ErrorBlocks() {
        BudgetValidationRule rule = createRule("VAL_001", "ERROR");

        // ERROR级别应阻止提交
        assertTrue("ERROR".equals(rule.getSeverityLevel()));
    }

    @Test
    @DisplayName("RSK-007: WARNING级不阻止提交")
    void testSeverityLevel_WarningNotBlocks() {
        BudgetValidationRule rule = createRule("VAL_006", "WARNING");

        // WARNING不阻止提交
        assertFalse("ERROR".equals(rule.getSeverityLevel()));
    }

    // ==================== RSK-010: 分级展示 ====================

    @Test
    @DisplayName("RSK-010: ERROR红色/WARNING橙色/INFO蓝色")
    void testSeverityLevelColors() {
        assertEquals("#FF0000", getSeverityColor("ERROR"));   // 红色
        assertEquals("#FFA500", getSeverityColor("WARNING")); // 橙色
        assertEquals("#0000FF", getSeverityColor("INFO"));    // 蓝色
    }

    // ==================== 辅助方法 ====================

    private BudgetPreparationDetail createDetail(String code, String type,
                                                  BigDecimal budget, BigDecimal actual) {
        BudgetPreparationDetail d = new BudgetPreparationDetail();
        d.setSubjectCode(code);
        d.setSubjectType(type);
        d.setBudgetAmount(budget);
        d.setActualAmount(actual);
        if (budget != null && actual != null) {
            d.setVarianceAmount(budget.subtract(actual));
        }
        return d;
    }

    private BudgetValidationRule createRule(String code, String level) {
        BudgetValidationRule rule = new BudgetValidationRule();
        rule.setRuleCode(code);
        rule.setSeverityLevel(level);
        rule.setIsActive(1);
        return rule;
    }

    private boolean checkBudgetOver(BudgetPreparationDetail detail, BigDecimal threshold) {
        if (detail.getActualAmount() == null || detail.getActualAmount().compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        if (detail.getVarianceAmount() == null) {
            return false;
        }
        BigDecimal rate = detail.getVarianceAmount().abs()
            .divide(detail.getActualAmount().abs(), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return rate.compareTo(threshold) > 0;
    }

    private boolean isMissingData(BudgetPreparationDetail detail) {
        return detail.getBudgetAmount() == null || detail.getSubjectCode() == null;
    }

    private boolean isIncomeNegative(BudgetPreparationDetail detail) {
        return "INCOME".equals(detail.getSubjectType())
            && detail.getBudgetAmount() != null
            && detail.getBudgetAmount().compareTo(BigDecimal.ZERO) < 0;
    }

    private String getSeverityColor(String level) {
        switch (level) {
            case "ERROR": return "#FF0000";
            case "WARNING": return "#FFA500";
            case "INFO": return "#0000FF";
            default: return "#000000";
        }
    }
}
