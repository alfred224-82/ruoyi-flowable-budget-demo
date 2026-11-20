package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.vo.BossDashboardVo;
import com.ruoyi.system.domain.vo.MonthlyFixedCostVo;
import com.ruoyi.system.domain.vo.MonthlyOperatingVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IBossDashboardService;
import com.ruoyi.system.service.IMicroOperatingReportService;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import com.ruoyi.system.service.IStaffCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 老板经营驾驶舱首页服务实现（T18.7，小微版 M18）
 * <p>★fail-fast：所有聚合查询均做 null 保护，返回空集合不返回 null。
 * <p>v1.68 修复：固定支出异常计数与异常处理中心同口径——老板当月已点「已处理」
 * （micro_exception_ack 存在当月确认标记）则计 0，避免驾驶舱/小程序待办徽章「复活」。
 * <p>T30.6：固定支出聚合改用周期分摊口径（每月分摊成本 = 总额 ÷ 周期总月数，
 * 如 5 年 50 万 → 8333.33/月），覆盖月区间内每月计入，与异常处理中心同口径。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BossDashboardServiceImpl implements IBossDashboardService {

    private final CashFlowMapper cashFlowMapper;
    private final IMonthlyFixedCostService fixedCostService;
    private final BookingOrderMapper bookingOrderMapper;
    private final VoiceComplianceHitMapper complianceHitMapper;
    private final MicroExceptionAckMapper exceptionAckMapper;
    /** ★REQ-02 活动应收/实收聚合（按 start_time 落在今日/本月） */
    private final PromotionActivityMapper activityMapper;
    /** ★REQ-03 人力成本聚合（基础工资 + 提成） */
    private final IStaffCommissionService staffCommissionService;
    /** ★T38.5 结算口径月毛利/净利/保本线（复用经营报表服务） */
    private final IMicroOperatingReportService operatingReportService;

    @Override
    public BossDashboardVo getDashboard() {
        BossDashboardVo vo = new BossDashboardVo();

        // ===== 核心经营数据 =====
        fillCoreMetrics(vo);

        // ===== 成本构成 =====
        fillCostComposition(vo);

        // ===== ★T38.5 结算口径月毛利/净利/保本线 =====
        fillOperatingProfit(vo);

        // ===== 业务待办 =====
        fillTodos(vo);

        return vo;
    }

    /**
     * ★T38.5 结算口径月利润：月毛利取自已结算活动（Σprofit），
     * 月净利 = 月毛利 − 固定成本合计 − 当月税费（复用 IMicroOperatingReportService，读 monthly_operating_closing）。
     * <p>fail-soft：报表服务异常不影响驾驶舱其余指标。
     */
    private void fillOperatingProfit(BossDashboardVo vo) {
        String currentMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
        try {
            MonthlyOperatingVo op = operatingReportService.monthlyOperating(currentMonth);
            if (op != null) {
                vo.setMonthGrossProfit(op.getMonthGrossProfit());
                vo.setMonthOperatingNetProfit(op.getNetProfit());
                vo.setBreakevenGrossProfit(op.getBreakevenGrossProfit());
                vo.setBreakevenProgress(op.getBreakevenProgress());
            }
        } catch (Exception e) {
            log.warn("[BossDashboard] 结算口径月利润聚合失败，按空处理: month={}", currentMonth, e);
        }
    }

    /**
     * 核心经营数据：今日/本月的收入、支出、净利
     */
    private void fillCoreMetrics(BossDashboardVo vo) {
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = dayFmt.format(new Date());

        // 今日
        Calendar todayCal = Calendar.getInstance();
        Date todayStart = todayCal.getTime();
        todayCal.set(Calendar.HOUR_OF_DAY, 23);
        todayCal.set(Calendar.MINUTE, 59);
        todayCal.set(Calendar.SECOND, 59);
        Date todayEnd = todayCal.getTime();

        BigDecimal[] todaySums = sumCashFlow(todayStart, todayEnd);
        vo.setTodayIncome(todaySums[0]);
        vo.setTodayExpense(todaySums[1]);
        vo.setTodayNetProfit(todaySums[0].subtract(todaySums[1]).setScale(2, RoundingMode.HALF_UP));

        // 本月
        Calendar monthCal = Calendar.getInstance();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        monthCal.set(Calendar.HOUR_OF_DAY, 0);
        monthCal.set(Calendar.MINUTE, 0);
        monthCal.set(Calendar.SECOND, 0);
        Date monthStart = monthCal.getTime();

        Calendar monthEndCal = Calendar.getInstance();
        monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        monthEndCal.set(Calendar.HOUR_OF_DAY, 23);
        monthEndCal.set(Calendar.MINUTE, 59);
        monthEndCal.set(Calendar.SECOND, 59);
        Date monthEnd = monthEndCal.getTime();

        BigDecimal[] monthSums = sumCashFlow(monthStart, monthEnd);
        vo.setMonthIncome(monthSums[0]);
        vo.setMonthExpense(monthSums[1]);
        vo.setMonthNetProfit(monthSums[0].subtract(monthSums[1]).setScale(2, RoundingMode.HALF_UP));

        // ★本月订单数 = 本月内状态为 PUBLISHED 的活动数（发布即计 1 单，下架/结束仍计入）
        Long orderCount = activityMapper.selectCount(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getStatus, "PUBLISHED")
                .ge(PromotionActivity::getStartTime, monthStart)
                .le(PromotionActivity::getStartTime, monthEnd));
        vo.setMonthOrderCount(orderCount != null ? orderCount.intValue() : 0);

        // ★REQ-02 今日/本月活动应收、实收（按 start_time 落在今日/本月区间聚合）
        BigDecimal[] todayAct = sumActivityAmounts(todayStart, todayEnd);
        vo.setTodayReceivable(todayAct[0]);
        vo.setTodayReceived(todayAct[1]);
        BigDecimal[] monthAct = sumActivityAmounts(monthStart, monthEnd);
        vo.setMonthReceivable(monthAct[0]);
        vo.setMonthReceived(monthAct[1]);
    }

    /**
     * ★REQ-02 汇总 start_time 落在 [begin, end] 的活动应收/实收
     * <p>应收=receivable_amount（活动发布时锁定=活动总价）；实收=received_amount（活动结束回写，未结束为0）。
     * <p>兜底：存量活动 receivable_amount 为 NULL 时，用 COALESCE(customPrice, autoCalculatedPrice) 计算。
     * @return [receivable, received]
     */
    private BigDecimal[] sumActivityAmounts(Date begin, Date end) {
        List<PromotionActivity> acts = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .ge(PromotionActivity::getStartTime, begin)
                .le(PromotionActivity::getStartTime, end));
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        if (acts != null) {
            for (PromotionActivity a : acts) {
                // 应收：优先取 receivable_amount，兜底用有效价格
                BigDecimal actReceivable = a.getReceivableAmount();
                if (actReceivable == null) {
                    actReceivable = a.getCustomPrice() != null
                        ? a.getCustomPrice() : a.getAutoCalculatedPrice();
                }
                if (actReceivable != null) {
                    receivable = receivable.add(actReceivable);
                }
                if (a.getReceivedAmount() != null) {
                    received = received.add(a.getReceivedAmount());
                }
            }
        }
        return new BigDecimal[]{receivable.setScale(2, RoundingMode.HALF_UP),
            received.setScale(2, RoundingMode.HALF_UP)};
    }

    /**
     * 汇总 cash_flow 收入/支出
     */
    private BigDecimal[] sumCashFlow(Date begin, Date end) {
        List<CashFlow> flows = cashFlowMapper.selectList(
            Wrappers.<CashFlow>lambdaQuery()
                .ge(CashFlow::getOccurDate, begin)
                .le(CashFlow::getOccurDate, end));
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        if (flows != null) {
            for (CashFlow f : flows) {
                BigDecimal amt = f.getAmount() != null ? f.getAmount() : BigDecimal.ZERO;
                if (CashFlowType.INCOME.getCode().equals(f.getFlowType())) {
                    income = income.add(amt);
                } else if (CashFlowType.EXPENSE.getCode().equals(f.getFlowType())) {
                    expense = expense.add(amt);
                }
            }
        }
        return new BigDecimal[]{income, expense};
    }

    /**
     * 成本构成：按 monthly_fixed_cost 的 category 分组统计本月占比
     * <p>T30.6 分摊口径：金额为每月分摊成本（总额 ÷ 周期总月数），覆盖月区间内每月计入；
     * 分类显示名优先取标签名（Service 回填 categoryName）。
     */
    private void fillCostComposition(BossDashboardVo vo) {
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());

        List<MonthlyFixedCostVo> costs = fixedCostService.listActiveInMonth(currentMonth);

        BigDecimal total = costs.stream()
            .map(c -> c.getMonthlyAmount() != null ? c.getMonthlyAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 按 category 分组（分摊额累加）
        Map<String, BigDecimal> categorySum = new LinkedHashMap<>();
        Map<String, String> categoryNameMap = new LinkedHashMap<>();
        for (MonthlyFixedCostVo c : costs) {
            String cat = c.getCategory() != null ? c.getCategory() : "other";
            BigDecimal amt = c.getMonthlyAmount() != null ? c.getMonthlyAmount() : BigDecimal.ZERO;
            categorySum.merge(cat, amt, BigDecimal::add);
            categoryNameMap.putIfAbsent(cat, c.getCategoryName() != null ? c.getCategoryName() : cat);
        }

        // ★REQ-03 新增「人力成本」分类 = 当月基础工资 + 当月提成（来自 staff_commission 聚合）
        BigDecimal laborCost = BigDecimal.ZERO;
        try {
            BigDecimal calc = staffCommissionService.sumMonthlyLaborCost(currentMonth);
            if (calc != null) {
                laborCost = calc;
            }
        } catch (Exception e) {
            log.warn("[BossDashboard] 人力成本聚合失败，按 0 计入: month={}", currentMonth, e);
        }
        if (laborCost.compareTo(BigDecimal.ZERO) > 0) {
            categorySum.merge("labor", laborCost, BigDecimal::add);
            categoryNameMap.putIfAbsent("labor", "人力成本");
        }

        // ★合计 = 固定支出 + 人力成本（前端「合计」行使用，必须与 costComposition 所有项之和一致）
        final BigDecimal grandTotal = total.add(laborCost);
        vo.setMonthFixedCostTotal(grandTotal);
        List<Map<String, Object>> composition = new ArrayList<>();
        categorySum.forEach((cat, amt) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", cat);
            item.put("name", categoryNameMap.getOrDefault(cat, cat));
            item.put("amount", amt);
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                item.put("percent", amt.multiply(new BigDecimal("100"))
                    .divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue());
            } else {
                item.put("percent", 0.0);
            }
            composition.add(item);
        });
        // 按金额降序
        composition.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        vo.setCostComposition(composition);
    }

    /**
     * 业务待办：预约/合规/固定支出异常
     */
    private void fillTodos(BossDashboardVo vo) {
        // 待处理预约（PENDING/CONFIRMED 状态）
        Long pendingBooking = bookingOrderMapper.selectCount(
            Wrappers.<BookingOrder>lambdaQuery()
                .in(BookingOrder::getOrderStatus, "PENDING", "CONFIRMED"));
        vo.setPendingBookingCount(pendingBooking != null ? pendingBooking.intValue() : 0);

        // 今日待服务
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = dayFmt.format(new Date());
        Calendar todayStartCal = Calendar.getInstance();
        todayStartCal.set(Calendar.HOUR_OF_DAY, 0);
        todayStartCal.set(Calendar.MINUTE, 0);
        todayStartCal.set(Calendar.SECOND, 0);
        Date todayStart = todayStartCal.getTime();

        Calendar todayEndCal = Calendar.getInstance();
        todayEndCal.set(Calendar.HOUR_OF_DAY, 23);
        todayEndCal.set(Calendar.MINUTE, 59);
        todayEndCal.set(Calendar.SECOND, 59);
        Date todayEnd = todayEndCal.getTime();

        Long todayService = bookingOrderMapper.selectCount(
            Wrappers.<BookingOrder>lambdaQuery()
                .ge(BookingOrder::getBookingTime, todayStart)
                .le(BookingOrder::getBookingTime, todayEnd)
                .in(BookingOrder::getOrderStatus, "CONFIRMED", "IN_SERVICE"));
        vo.setTodayServiceCount(todayService != null ? todayService.intValue() : 0);

        // 合规待处理（PENDING 状态 + result=FAIL，近 7 天；PASS 记录不计入待处理，与异常处理中心同口径）
        Calendar cal7 = Calendar.getInstance();
        cal7.add(Calendar.DAY_OF_MONTH, -7);
        Long compliancePending = complianceHitMapper.selectCount(
            Wrappers.<VoiceComplianceHit>lambdaQuery()
                .ge(VoiceComplianceHit::getHitTime, cal7.getTime())
                .eq(VoiceComplianceHit::getStatus, "PENDING")
                .eq(VoiceComplianceHit::getResult, "FAIL"));
        vo.setCompliancePendingCount(compliancePending != null ? compliancePending.intValue() : 0);

        // 固定支出异常：本月无活跃固定支出项（T30.6 分摊覆盖口径；老板当月已点「已处理」则视为已处理，与异常处理中心同口径）
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());
        long monthCostCount = fixedCostService.countActiveInMonth(currentMonth);
        boolean costAnomaly = monthCostCount == 0;
        if (costAnomaly) {
            Long ackCount = exceptionAckMapper.selectCount(
                Wrappers.<MicroExceptionAck>lambdaQuery()
                    .eq(MicroExceptionAck::getExceptionType, "FIXED_COST_ANOMALY")
                    .eq(MicroExceptionAck::getExceptionKey, currentMonth));
            costAnomaly = ackCount == null || ackCount == 0;
        }
        vo.setCostAnomalyCount(costAnomaly ? 1 : 0);
    }
}
