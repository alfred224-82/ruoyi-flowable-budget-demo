package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.CashFlow;
import com.ruoyi.system.domain.MicroDailyRevenue;
import com.ruoyi.system.domain.bo.ReportQueryBo;
import com.ruoyi.system.domain.enums.BookingPayStatus;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.vo.MicroDailyRevenueVo;
import com.ruoyi.system.domain.vo.RevenueSummaryVo;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.CashFlowMapper;
import com.ruoyi.system.mapper.MicroDailyRevenueMapper;
import com.ruoyi.system.service.IMicroReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 小微版经营报表服务实现（T15.5/T15.14/T15.22）
 * <p>报表口径基于 cash_flow（纯记账）聚合：grossIncome=Σincome，totalExpense=Σexpense，netProfit=收入-支出，
 * orderCount=周期内已收款订单数。Web 报表端依赖多租户插件自动隔离（tenantId 传 null）；
 * {@link #generateDailyRevenue} 供 XXL-JOB 逐租户调用，跨租户写入：显式按 tenantId 聚合 + setTenantId（uk_tenant_date 幂等 upsert）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroReportServiceImpl implements IMicroReportService {

    private final CashFlowMapper cashFlowMapper;
    private final BookingOrderMapper bookingMapper;
    private final MicroDailyRevenueMapper revenueMapper;

    @Override
    public RevenueSummaryVo today() {
        Date now = new Date();
        return summarize(null, DateUtil.beginOfDay(now), DateUtil.endOfDay(now));
    }

    @Override
    public RevenueSummaryVo weekly() {
        Date now = new Date();
        Date begin = DateUtil.beginOfDay(DateUtil.offsetDay(now, -6));
        return summarize(null, begin, DateUtil.endOfDay(now));
    }

    @Override
    public RevenueSummaryVo monthly() {
        Date now = new Date();
        return summarize(null, DateUtil.beginOfMonth(now), DateUtil.endOfDay(now));
    }

    @Override
    public RevenueSummaryVo profit(ReportQueryBo bo) {
        if (bo == null || bo.getBeginDate() == null || bo.getEndDate() == null) {
            throw new ServiceException("利润报表须指定起止日期");
        }
        if (bo.getBeginDate().after(bo.getEndDate())) {
            throw new ServiceException("起始日期不能晚于结束日期");
        }
        return summarize(null, DateUtil.beginOfDay(bo.getBeginDate()), DateUtil.endOfDay(bo.getEndDate()));
    }

    @Override
    public RevenueSummaryVo cashPosition() {
        Date now = new Date();
        // 今日营收/净利
        RevenueSummaryVo vo = summarize(null, DateUtil.beginOfDay(now), DateUtil.endOfDay(now));
        // 累计资金结余（全周期 Σincome-Σexpense）
        BigDecimal[] all = sumCashFlow(null, null, null);
        vo.setCashBalance(all[0].subtract(all[1]).setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public List<MicroDailyRevenueVo> trend(ReportQueryBo bo) {
        Date begin = (bo != null && bo.getBeginDate() != null)
            ? DateUtil.beginOfDay(bo.getBeginDate())
            : DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -29));
        Date end = (bo != null && bo.getEndDate() != null)
            ? DateUtil.endOfDay(bo.getEndDate())
            : DateUtil.endOfDay(new Date());
        return revenueMapper.selectVoList(Wrappers.<MicroDailyRevenue>lambdaQuery()
            .ge(MicroDailyRevenue::getRevenueDate, begin)
            .le(MicroDailyRevenue::getRevenueDate, end)
            .orderByAsc(MicroDailyRevenue::getRevenueDate));
    }

    @Override
    public int generateDailyRevenue(Long tenantId, Date date) {
        Long tid = tenantId != null ? tenantId : LoginHelper.getTenantId();
        if (tid == null) {
            throw new ServiceException("租户ID不能为空");
        }
        Date day = date != null ? DateUtil.beginOfDay(date) : DateUtil.beginOfDay(new Date());
        Date dayEnd = DateUtil.endOfDay(day);
        // ★跨租户安全：显式按 tenantId 聚合（admin/Job 上下文插件可能不过滤）
        BigDecimal[] sums = sumCashFlow(tid, day, dayEnd);
        BigDecimal income = sums[0];
        BigDecimal expense = sums[1];
        BigDecimal net = income.subtract(expense).setScale(2, RoundingMode.HALF_UP);
        int orderCount = countReceivedOrders(tid, day, dayEnd);
        // uk_tenant_date 幂等 upsert
        MicroDailyRevenue exist = revenueMapper.selectOne(Wrappers.<MicroDailyRevenue>lambdaQuery()
            .eq(MicroDailyRevenue::getTenantId, tid)
            .eq(MicroDailyRevenue::getRevenueDate, day));
        if (exist != null) {
            MicroDailyRevenue update = new MicroDailyRevenue();
            update.setId(exist.getId());
            update.setGrossIncome(income);
            update.setTotalExpense(expense);
            update.setNetProfit(net);
            update.setOrderCount(orderCount);
            update.setSnapshotTime(new Date());
            return revenueMapper.updateById(update);
        }
        MicroDailyRevenue entity = new MicroDailyRevenue();
        entity.setTenantId(tid); // ★跨租户写入显式指定
        entity.setRevenueDate(day);
        entity.setGrossIncome(income);
        entity.setTotalExpense(expense);
        entity.setNetProfit(net);
        entity.setOrderCount(orderCount);
        entity.setSnapshotTime(new Date());
        return revenueMapper.insert(entity);
    }

    /**
     * 聚合指定周期的收支（tenantId 为空时依赖多租户插件自动隔离）
     */
    private RevenueSummaryVo summarize(Long tenantId, Date begin, Date end) {
        BigDecimal[] sums = sumCashFlow(tenantId, begin, end);
        RevenueSummaryVo vo = new RevenueSummaryVo();
        vo.setStartDate(begin);
        vo.setEndDate(end);
        vo.setGrossIncome(sums[0]);
        vo.setTotalExpense(sums[1]);
        vo.setNetProfit(sums[0].subtract(sums[1]).setScale(2, RoundingMode.HALF_UP));
        vo.setOrderCount(countReceivedOrders(tenantId, begin, end));
        return vo;
    }

    /**
     * 汇总 cash_flow 收入/支出（返回 [income, expense]，均保留两位小数）
     */
    private BigDecimal[] sumCashFlow(Long tenantId, Date begin, Date end) {
        List<CashFlow> flows = cashFlowMapper.selectList(Wrappers.<CashFlow>lambdaQuery()
            .eq(tenantId != null, CashFlow::getTenantId, tenantId)
            .ge(begin != null, CashFlow::getOccurDate, begin)
            .le(end != null, CashFlow::getOccurDate, end));
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (CashFlow f : flows) {
            BigDecimal amt = f.getAmount() != null ? f.getAmount() : BigDecimal.ZERO;
            if (CashFlowType.INCOME.getCode().equals(f.getFlowType())) {
                income = income.add(amt);
            } else if (CashFlowType.EXPENSE.getCode().equals(f.getFlowType())) {
                expense = expense.add(amt);
            }
        }
        return new BigDecimal[]{income.setScale(2, RoundingMode.HALF_UP), expense.setScale(2, RoundingMode.HALF_UP)};
    }

    /**
     * 统计周期内已收款订单数（pay_status=RECEIVED 且 pay_time 落在周期内）
     */
    private int countReceivedOrders(Long tenantId, Date begin, Date end) {
        Long count = bookingMapper.selectCount(Wrappers.<BookingOrder>lambdaQuery()
            .eq(tenantId != null, BookingOrder::getTenantId, tenantId)
            .eq(BookingOrder::getPayStatus, BookingPayStatus.RECEIVED.getCode())
            .ge(begin != null, BookingOrder::getPayTime, begin)
            .le(end != null, BookingOrder::getPayTime, end));
        return count == null ? 0 : count.intValue();
    }
}
