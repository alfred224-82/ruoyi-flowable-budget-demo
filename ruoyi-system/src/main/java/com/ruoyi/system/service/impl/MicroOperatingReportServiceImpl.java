package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.ActivitySettlement;
import com.ruoyi.system.domain.ActivityStaffSnapshot;
import com.ruoyi.system.domain.MonthlyOperatingClosing;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.bo.ActivityDetailQueryBo;
import com.ruoyi.system.domain.bo.MonthlyTaxBo;
import com.ruoyi.system.domain.vo.ActivityDetailReportVo;
import com.ruoyi.system.domain.vo.ActivityDetailRowVo;
import com.ruoyi.system.domain.vo.DailyReportVo;
import com.ruoyi.system.domain.vo.MonthlyClosingVo;
import com.ruoyi.system.domain.vo.MonthlyFixedCostVo;
import com.ruoyi.system.domain.vo.MonthlyOperatingVo;
import com.ruoyi.system.domain.vo.ReconcileVo;
import com.ruoyi.system.mapper.ActivitySettlementMapper;
import com.ruoyi.system.mapper.ActivityStaffSnapshotMapper;
import com.ruoyi.system.mapper.MonthlyOperatingClosingMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IMicroOperatingReportService;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import com.ruoyi.system.service.IStaffCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 小微经营核算与三张报表服务实现（M38 T38.3/T38.4 + M39 T39.1~T39.5）
 * <p>以 activity_settlement（单场毛利=profit）为权威数据源，promotion_activity.end_time 决定跨月归属；
 * 采用 Service 层两步 Wrappers 聚合（先活动后结算），与既有 Mapper（BaseMapperPlus，无 XML）风格一致。
 * <p>「已结算」= 原单（is_adjustment=0）status ∈ {SUBMITTED, LOCKED}；调整单（同活动 SUBMITTED/LOCKED）叠加计入。
 * <p>代收 collected_amount 不计入收入/利润；月净利 = 月毛利 − 固定成本合计（含人力成本）− 税费。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroOperatingReportServiceImpl implements IMicroOperatingReportService {

    private final ActivitySettlementMapper settlementMapper;
    private final PromotionActivityMapper activityMapper;
    private final ActivityStaffSnapshotMapper snapshotMapper;
    private final SysUserMapper userMapper;
    private final MonthlyOperatingClosingMapper closingMapper;
    private final IMonthlyFixedCostService fixedCostService;
    private final IStaffCommissionService staffCommissionService;

    /** 结算状态常量（对齐 ActivitySettlementServiceImpl） */
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_LOCKED = "LOCKED";

    /** 月结状态常量 */
    private static final String CLOSING_OPEN = "OPEN";
    private static final String CLOSING_LOCKED = "LOCKED";

    /** 计税方式 */
    private static final String TAX_MANUAL = "MANUAL";
    private static final String TAX_RATE = "RATE";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ===================================================================
    // T38.3 月度经营报表
    // ===================================================================

    @Override
    public MonthlyOperatingVo monthlyOperating(String month) {
        String m = normalizeMonth(month);
        MonthlyOperatingVo vo = buildMonthly(m);
        // 对比上月
        String prev = minusOneMonth(m);
        MonthlyOperatingVo p = buildMonthly(prev);
        vo.setPrevMonthGrossProfit(p.getMonthGrossProfit());
        vo.setPrevNetProfit(p.getNetProfit());
        return vo;
    }

    /** 构建单月经营报表（不含对比上月字段，避免递归） */
    private MonthlyOperatingVo buildMonthly(String month) {
        MonthlyOperatingVo vo = new MonthlyOperatingVo();
        vo.setMonth(month);

        Date[] range = monthRange(month);
        List<ActivityDetailRowVo> rows = buildRows(range[0], range[1], null, null, null);
        List<ActivityDetailRowVo> settled = rows.stream()
            .filter(r -> Boolean.TRUE.equals(r.getSettled())).collect(Collectors.toList());

        BigDecimal gross = sum(settled, ActivityDetailRowVo::getProfit);
        BigDecimal revenue = sum(settled, ActivityDetailRowVo::getTotalRevenue);
        BigDecimal cost = sum(settled, ActivityDetailRowVo::getTotalCost);
        int activityCount = settled.size();

        vo.setMonthGrossProfit(scale(gross));
        vo.setMonthRevenue(scale(revenue));
        vo.setMonthCost(scale(cost));
        vo.setActivityCount(activityCount);
        vo.setUnsettledCount(rows.size() - activityCount);
        BigDecimal avg = activityCount > 0
            ? gross.divide(BigDecimal.valueOf(activityCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        vo.setAvgSessionProfit(scale(avg));

        // 固定成本构成（分摊 + 人力成本）
        List<Map<String, Object>> composition = new ArrayList<>();
        BigDecimal fixedCostTotal = fixedCostTotal(month, composition);
        vo.setFixedCostComposition(composition);
        vo.setFixedCostTotal(scale(fixedCostTotal));

        // 税前利润 = 月毛利 − 固定成本合计
        BigDecimal pretax = gross.subtract(fixedCostTotal);
        vo.setPretaxProfit(scale(pretax));

        // 税费
        MonthlyOperatingClosing closing = findClosing(month);
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (closing != null) {
            vo.setTaxMode(closing.getTaxMode());
            vo.setTaxRate(closing.getTaxRate());
            if (TAX_RATE.equals(closing.getTaxMode()) && closing.getTaxRate() != null) {
                // 按率估算：实时按当前税前利润重算（活动数据变动后不陈旧）
                taxAmount = estimateTax(pretax, closing.getTaxRate());
            } else if (closing.getTaxAmount() != null) {
                taxAmount = closing.getTaxAmount();
            }
        }
        vo.setTaxAmount(scale(taxAmount));
        vo.setNetProfit(scale(pretax.subtract(taxAmount)));
        vo.setClosingStatus(closing != null && StrUtil.isNotBlank(closing.getStatus())
            ? closing.getStatus() : CLOSING_OPEN);

        // 保本线
        vo.setBreakevenGrossProfit(scale(fixedCostTotal));
        int breakevenSessions = 0;
        if (fixedCostTotal.compareTo(BigDecimal.ZERO) > 0 && avg.compareTo(BigDecimal.ZERO) > 0) {
            breakevenSessions = (int) Math.ceil(
                fixedCostTotal.doubleValue() / avg.doubleValue());
        }
        vo.setBreakevenSessions(breakevenSessions);
        vo.setBreakevenProgress(progress(gross, fixedCostTotal));
        vo.setPassedBreakeven(gross.compareTo(fixedCostTotal) >= 0);
        return vo;
    }

    // ===================================================================
    // T39.2 日报
    // ===================================================================

    @Override
    public DailyReportVo dailyReport(Date date) {
        Date day = date != null ? date : new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dayBegin = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date dayEnd = cal.getTime();

        String month = new SimpleDateFormat("yyyy-MM").format(day);
        Date[] monthRange = monthRange(month);

        DailyReportVo vo = new DailyReportVo();
        vo.setDate(dayBegin);

        // 当日活动清单
        List<ActivityDetailRowVo> dayRows = buildRows(dayBegin, dayEnd, null, null, null);
        vo.setSessions(dayRows);
        List<ActivityDetailRowVo> daySettled = dayRows.stream()
            .filter(r -> Boolean.TRUE.equals(r.getSettled())).collect(Collectors.toList());
        vo.setSessionCount(daySettled.size());
        BigDecimal dayGross = sum(daySettled, ActivityDetailRowVo::getProfit);
        vo.setRevenue(scale(sum(daySettled, ActivityDetailRowVo::getTotalRevenue)));
        vo.setCost(scale(sum(daySettled, ActivityDetailRowVo::getTotalCost)));
        vo.setGrossProfit(scale(dayGross));

        // 日均固定成本参考 = 月固定成本合计 ÷ 当月天数
        BigDecimal fixedCostTotal = fixedCostTotal(month, null);
        int daysInMonth = YearMonth.parse(month, MONTH_FMT).lengthOfMonth();
        BigDecimal dailyRef = fixedCostTotal.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        vo.setDailyFixedCostRef(scale(dailyRef));
        vo.setEstNetProfit(scale(dayGross.subtract(dailyRef)));

        // 本月进度（累计至当日）
        List<ActivityDetailRowVo> accumRows = buildRows(monthRange[0], dayEnd, null, null, null);
        BigDecimal monthAccum = sum(accumRows.stream()
            .filter(r -> Boolean.TRUE.equals(r.getSettled())).collect(Collectors.toList()),
            ActivityDetailRowVo::getProfit);
        vo.setMonthAccumGrossProfit(scale(monthAccum));
        vo.setBreakevenGrossProfit(scale(fixedCostTotal));
        vo.setBreakevenProgress(progress(monthAccum, fixedCostTotal));
        vo.setPassedBreakeven(monthAccum.compareTo(fixedCostTotal) >= 0);
        return vo;
    }

    // ===================================================================
    // T39.1 活动明细表
    // ===================================================================

    @Override
    public ActivityDetailReportVo activityDetail(ActivityDetailQueryBo bo) {
        ActivityDetailQueryBo query = bo != null ? bo : new ActivityDetailQueryBo();
        Date begin = parseDate(query.getBeginDate(), false);
        Date end = parseDate(query.getEndDate(), true);
        List<ActivityDetailRowVo> rows = buildRows(begin, end,
            query.getActivityStatus(), query.getTitle(), query.getStaffId());

        // 结算状态过滤（DRAFT=未结算；SUBMITTED/LOCKED=对应状态）
        if (StrUtil.isNotBlank(query.getSettleStatus())) {
            String ss = query.getSettleStatus();
            rows = rows.stream().filter(r -> {
                if (STATUS_DRAFT.equals(ss)) {
                    return !Boolean.TRUE.equals(r.getSettled());
                }
                return ss.equals(r.getSettlementStatus());
            }).collect(Collectors.toList());
        }

        ActivityDetailReportVo report = new ActivityDetailReportVo();
        report.setRows(rows);
        report.setTotal((long) rows.size());

        // 合计行：仅累加已结算行（与月报月毛利天然一致）
        List<ActivityDetailRowVo> settled = rows.stream()
            .filter(r -> Boolean.TRUE.equals(r.getSettled())).collect(Collectors.toList());
        ActivityDetailRowVo total = new ActivityDetailRowVo();
        total.setActivityTitle("合计（已结算 " + settled.size() + " 场）");
        total.setParticipantCount(settled.stream().map(ActivityDetailRowVo::getParticipantCount)
            .filter(Objects::nonNull).reduce(0, Integer::sum));
        total.setTicketRevenue(scale(sum(settled, ActivityDetailRowVo::getTicketRevenue)));
        total.setAddonRevenue(scale(sum(settled, ActivityDetailRowVo::getAddonRevenue)));
        total.setCollectedAmount(scale(sum(settled, ActivityDetailRowVo::getCollectedAmount)));
        total.setTotalRevenue(scale(sum(settled, ActivityDetailRowVo::getTotalRevenue)));
        total.setEmployeeCost(scale(sum(settled, ActivityDetailRowVo::getEmployeeCost)));
        total.setOtherCost(scale(sum(settled, ActivityDetailRowVo::getOtherCost)));
        total.setTotalCost(scale(sum(settled, ActivityDetailRowVo::getTotalCost)));
        total.setProfit(scale(sum(settled, ActivityDetailRowVo::getProfit)));
        BigDecimal est = sum(settled, ActivityDetailRowVo::getEstimatedProfit);
        total.setEstimatedProfit(scale(est));
        total.setProfitDiff(scale(sum(settled, ActivityDetailRowVo::getProfit).subtract(est)));
        report.setTotalRow(total);
        report.setUnsettledCount((int) rows.stream()
            .filter(r -> !Boolean.TRUE.equals(r.getSettled())).count());
        return report;
    }

    // ===================================================================
    // T39.5 对账校验
    // ===================================================================

    @Override
    public ReconcileVo reconcile(String month) {
        String m = normalizeMonth(month);
        Date[] range = monthRange(m);

        // 路径1：活动明细同月毛利合计（已结算行）
        List<ActivityDetailRowVo> rows = buildRows(range[0], range[1], null, null, null);
        List<ActivityDetailRowVo> settled = rows.stream()
            .filter(r -> Boolean.TRUE.equals(r.getSettled())).collect(Collectors.toList());
        BigDecimal detailTotal = sum(settled, ActivityDetailRowVo::getProfit);

        // 路径2：月报月毛利
        BigDecimal monthGross = buildMonthly(m).getMonthGrossProfit();

        // 路径3：日报月累计毛利（取当月最后一天的日报）
        BigDecimal dailyAccum = dailyReport(range[1]).getMonthAccumGrossProfit();

        // 路径4：直接聚合已结算 settlement.profit（独立查询路径）
        BigDecimal directSum = directSettledProfit(range[0], range[1]);

        List<ReconcileVo.Rule> rules = new ArrayList<>();
        rules.add(new ReconcileVo.Rule("日报月累计毛利 = 活动明细同月毛利合计", scale(dailyAccum), scale(detailTotal)));
        rules.add(new ReconcileVo.Rule("活动明细同月毛利合计 = 月报月毛利", scale(detailTotal), scale(monthGross)));
        rules.add(new ReconcileVo.Rule("月报月毛利 = 结算单直聚合毛利", scale(monthGross), scale(directSum)));
        rules.add(new ReconcileVo.Rule("月报月实收 = 结算单直聚合实收",
            scale(buildMonthly(m).getMonthRevenue()), scale(directSettledRevenue(range[0], range[1]))));

        ReconcileVo vo = new ReconcileVo();
        vo.setMonth(m);
        vo.setRules(rules);
        vo.setOverallPassed(rules.stream().allMatch(r -> Boolean.TRUE.equals(r.getPassed())));
        return vo;
    }

    // ===================================================================
    // T38.4 税费 / 月结
    // ===================================================================

    @Override
    public MonthlyClosingVo getClosing(String month) {
        String m = normalizeMonth(month);
        MonthlyOperatingClosing closing = findClosing(m);
        if (closing == null) {
            MonthlyClosingVo vo = new MonthlyClosingVo();
            vo.setMonth(m);
            vo.setStatus(CLOSING_OPEN);
            vo.setLocked(false);
            vo.setTaxAmount(BigDecimal.ZERO);
            return vo;
        }
        return toClosingVo(closing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthlyClosingVo saveTax(MonthlyTaxBo bo) {
        if (bo == null || StrUtil.isBlank(bo.getMonth())) {
            throw new ServiceException("月份不能为空");
        }
        String month = normalizeMonth(bo.getMonth());
        MonthlyOperatingClosing closing = findClosing(month);
        if (closing != null && CLOSING_LOCKED.equals(closing.getStatus())) {
            throw new ServiceException("该月已月结锁定，不可修改税费");
        }
        boolean isNew = closing == null;
        if (isNew) {
            closing = new MonthlyOperatingClosing();
            closing.setTenantId(LoginHelper.getTenantId());
            closing.setMonth(month);
            closing.setStatus(CLOSING_OPEN);
        }
        String taxMode = StrUtil.isNotBlank(bo.getTaxMode()) ? bo.getTaxMode() : TAX_MANUAL;
        closing.setTaxMode(taxMode);
        closing.setTaxRate(bo.getTaxRate());
        closing.setRemark(bo.getRemark());
        BigDecimal taxAmount;
        if (TAX_RATE.equals(taxMode)) {
            BigDecimal rate = bo.getTaxRate() != null ? bo.getTaxRate() : BigDecimal.ZERO;
            BigDecimal pretax = buildMonthly(month).getPretaxProfit();
            taxAmount = estimateTax(pretax, rate);
        } else {
            taxAmount = bo.getTaxAmount() != null ? bo.getTaxAmount() : BigDecimal.ZERO;
        }
        closing.setTaxAmount(scale(taxAmount));
        if (isNew) {
            closingMapper.insert(closing);
        } else {
            closingMapper.updateById(closing);
        }
        return toClosingVo(closing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthlyClosingVo lockMonth(String month) {
        String m = normalizeMonth(month);
        MonthlyOperatingClosing closing = findClosing(m);
        if (closing != null && CLOSING_LOCKED.equals(closing.getStatus())) {
            throw new ServiceException("该月已月结锁定，请勿重复操作");
        }
        boolean isNew = closing == null;
        if (isNew) {
            closing = new MonthlyOperatingClosing();
            closing.setTenantId(LoginHelper.getTenantId());
            closing.setMonth(m);
            closing.setTaxAmount(BigDecimal.ZERO);
            closing.setTaxMode(TAX_MANUAL);
        }
        closing.setStatus(CLOSING_LOCKED);
        closing.setLockTime(new Date());
        closing.setLockBy(LoginHelper.getUserId());
        if (isNew) {
            closingMapper.insert(closing);
        } else {
            closingMapper.updateById(closing);
        }
        log.info("[lockMonth] 月度经营月结锁定: month={}, lockBy={}", m, closing.getLockBy());
        return toClosingVo(closing);
    }

    // ===================================================================
    // 导出 CSV
    // ===================================================================

    @Override
    public String exportActivityDetailCsv(ActivityDetailQueryBo bo) {
        ActivityDetailReportVo report = activityDetail(bo);
        StringBuilder sb = new StringBuilder();
        sb.append("活动明细表\n");
        sb.append("活动名称,本场员工,参与人数,票面收入,追加收入,代收金额,收入合计,员工成本,其他成本,成本合计,毛利,预估毛利,差额,结算状态,结束时间\n");
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (report.getRows() != null) {
            for (ActivityDetailRowVo r : report.getRows()) {
                sb.append(csv(r.getActivityTitle())).append(",")
                    .append(csv(r.getStaffNames())).append(",")
                    .append(nz(r.getParticipantCount())).append(",")
                    .append(num(r.getTicketRevenue())).append(",")
                    .append(num(r.getAddonRevenue())).append(",")
                    .append(num(r.getCollectedAmount())).append(",")
                    .append(num(r.getTotalRevenue())).append(",")
                    .append(num(r.getEmployeeCost())).append(",")
                    .append(num(r.getOtherCost())).append(",")
                    .append(num(r.getTotalCost())).append(",")
                    .append(num(r.getProfit())).append(",")
                    .append(num(r.getEstimatedProfit())).append(",")
                    .append(num(r.getProfitDiff())).append(",")
                    .append(settleText(r)).append(",")
                    .append(r.getEndTime() != null ? dt.format(r.getEndTime()) : "").append("\n");
            }
        }
        ActivityDetailRowVo t = report.getTotalRow();
        if (t != null) {
            sb.append(csv(t.getActivityTitle())).append(",,,,,")
                .append(num(t.getCollectedAmount())).append(",")
                .append(num(t.getTotalRevenue())).append(",,,")
                .append(num(t.getTotalCost())).append(",")
                .append(num(t.getProfit())).append(",")
                .append(num(t.getEstimatedProfit())).append(",")
                .append(num(t.getProfitDiff())).append(",,\n");
        }
        return sb.toString();
    }

    @Override
    public String exportMonthlyCsv(String month) {
        MonthlyOperatingVo vo = monthlyOperating(month);
        StringBuilder sb = new StringBuilder();
        sb.append("月度经营报表\n");
        sb.append("月份,").append(csv(vo.getMonth())).append("\n\n");
        sb.append("【月经营汇总】\n");
        sb.append("月实收,").append(num(vo.getMonthRevenue())).append("\n");
        sb.append("月变动成本,").append(num(vo.getMonthCost())).append("\n");
        sb.append("月毛利,").append(num(vo.getMonthGrossProfit())).append("\n");
        sb.append("已结算场次,").append(nz(vo.getActivityCount())).append("\n");
        sb.append("平均单场毛利,").append(num(vo.getAvgSessionProfit())).append("\n");
        sb.append("未结算场次,").append(nz(vo.getUnsettledCount())).append("\n\n");
        sb.append("【固定成本构成】\n");
        if (vo.getFixedCostComposition() != null) {
            for (Map<String, Object> item : vo.getFixedCostComposition()) {
                sb.append(csv(String.valueOf(item.get("name")))).append(",")
                    .append(item.get("amount")).append(",")
                    .append(item.get("percent")).append("%\n");
            }
        }
        sb.append("固定成本合计,").append(num(vo.getFixedCostTotal())).append("\n\n");
        sb.append("【净利润】\n");
        sb.append("税前利润,").append(num(vo.getPretaxProfit())).append("\n");
        sb.append("税费,").append(num(vo.getTaxAmount())).append("\n");
        sb.append("月净利润,").append(num(vo.getNetProfit())).append("\n\n");
        sb.append("【保本线】\n");
        sb.append("月保本毛利,").append(num(vo.getBreakevenGrossProfit())).append("\n");
        sb.append("月保本场次,").append(nz(vo.getBreakevenSessions())).append("\n");
        sb.append("保本进度,").append(num(vo.getBreakevenProgress())).append("%\n\n");
        sb.append("【对比上月】\n");
        sb.append("上月毛利,").append(num(vo.getPrevMonthGrossProfit())).append("\n");
        sb.append("上月净利,").append(num(vo.getPrevNetProfit())).append("\n");
        return sb.toString();
    }

    // ===================================================================
    // 核心聚合：两步 Wrappers（先活动后结算）
    // ===================================================================

    /**
     * 构建 [begin, end] 区间内（按活动 end_time 归属）的活动明细行。
     *
     * @param staffId 本场员工过滤（可空）
     */
    private List<ActivityDetailRowVo> buildRows(Date begin, Date end, String activityStatus,
                                                String title, Long staffId) {
        List<PromotionActivity> acts = queryActivities(begin, end, activityStatus, title);
        if (CollUtil.isEmpty(acts)) {
            return new ArrayList<>();
        }
        List<Long> actIds = acts.stream().map(PromotionActivity::getId).collect(Collectors.toList());
        Map<Long, List<ActivitySettlement>> settleMap = loadSettlements(actIds);
        Map<Long, String> staffNameCache = new HashMap<>();

        List<ActivityDetailRowVo> rows = new ArrayList<>();
        for (PromotionActivity act : acts) {
            List<Long> staffIds = parseStaffIds(act);
            if (staffId != null && !staffIds.contains(staffId)) {
                continue;
            }
            rows.add(buildRow(act, settleMap.getOrDefault(act.getId(), Collections.emptyList()),
                staffIds, staffNameCache));
        }
        return rows;
    }

    private List<PromotionActivity> queryActivities(Date begin, Date end, String activityStatus, String title) {
        LambdaQueryWrapper<PromotionActivity> w = Wrappers.<PromotionActivity>lambdaQuery()
            .ge(begin != null, PromotionActivity::getEndTime, begin)
            .le(end != null, PromotionActivity::getEndTime, end)
            .eq(StrUtil.isNotBlank(activityStatus), PromotionActivity::getStatus, activityStatus)
            .like(StrUtil.isNotBlank(title), PromotionActivity::getTitle, title)
            .orderByAsc(PromotionActivity::getEndTime);
        List<PromotionActivity> list = activityMapper.selectList(w);
        return list != null ? list : Collections.emptyList();
    }

    private Map<Long, List<ActivitySettlement>> loadSettlements(List<Long> actIds) {
        if (CollUtil.isEmpty(actIds)) {
            return Collections.emptyMap();
        }
        List<ActivitySettlement> list = settlementMapper.selectList(
            Wrappers.<ActivitySettlement>lambdaQuery().in(ActivitySettlement::getActivityId, actIds));
        Map<Long, List<ActivitySettlement>> map = new HashMap<>();
        if (list != null) {
            for (ActivitySettlement s : list) {
                map.computeIfAbsent(s.getActivityId(), k -> new ArrayList<>()).add(s);
            }
        }
        return map;
    }

    /**
     * 由活动 + 其结算单集合构建明细行。
     * <p>已结算：原单（is_adjustment=0）status ∈ {SUBMITTED, LOCKED}；金额=Σ 已提交/锁定的原单+调整单。
     * <p>未结算：展示原单/草稿原值，settled=false（不计入合计）。
     */
    private ActivityDetailRowVo buildRow(PromotionActivity act, List<ActivitySettlement> settlements,
                                         List<Long> staffIds, Map<Long, String> nameCache) {
        ActivityDetailRowVo row = new ActivityDetailRowVo();
        row.setActivityId(act.getId());
        row.setActivityTitle(act.getTitle());
        row.setParticipantCount(act.getParticipantCount());
        row.setEndTime(act.getEndTime());
        row.setStaffNames(resolveStaffNames(staffIds, nameCache));

        ActivitySettlement base = null;
        List<ActivitySettlement> counted = new ArrayList<>();
        for (ActivitySettlement s : settlements) {
            boolean isBase = s.getIsAdjustment() == null || Integer.valueOf(0).equals(s.getIsAdjustment());
            if (isBase && (base == null || compareCreate(s, base) > 0)) {
                base = s;
            }
            if (STATUS_SUBMITTED.equals(s.getStatus()) || STATUS_LOCKED.equals(s.getStatus())) {
                counted.add(s);
            }
        }
        boolean settled = base != null
            && (STATUS_SUBMITTED.equals(base.getStatus()) || STATUS_LOCKED.equals(base.getStatus()));

        ActivitySettlement display = base != null ? base
            : (settlements.isEmpty() ? null : settlements.get(0));
        row.setSettlementId(display != null ? display.getId() : null);
        row.setSettlementStatus(base != null ? base.getStatus()
            : (display != null ? display.getStatus() : STATUS_DRAFT));
        row.setSettled(settled);

        List<ActivitySettlement> src = settled ? counted
            : (display != null ? Collections.singletonList(display) : Collections.emptyList());
        BigDecimal ticket = BigDecimal.ZERO, addon = BigDecimal.ZERO, collected = BigDecimal.ZERO,
            totalRevenue = BigDecimal.ZERO, employee = BigDecimal.ZERO, other = BigDecimal.ZERO,
            totalCost = BigDecimal.ZERO, profit = BigDecimal.ZERO;
        for (ActivitySettlement s : src) {
            ticket = ticket.add(nvl(s.getTicketRevenue()));
            addon = addon.add(nvl(s.getAddonRevenue()));
            collected = collected.add(nvl(s.getCollectedAmount()));
            totalRevenue = totalRevenue.add(nvl(s.getTotalRevenue()));
            employee = employee.add(nvl(s.getEmployeeCost()));
            other = other.add(nvl(s.getOtherCost()));
            totalCost = totalCost.add(nvl(s.getTotalCost()));
            profit = profit.add(nvl(s.getProfit()));
        }
        row.setTicketRevenue(scale(ticket));
        row.setAddonRevenue(scale(addon));
        row.setCollectedAmount(scale(collected));
        row.setTotalRevenue(scale(totalRevenue));
        row.setEmployeeCost(scale(employee));
        row.setOtherCost(scale(other));
        row.setTotalCost(scale(totalCost));
        row.setProfit(scale(profit));
        BigDecimal est = display != null ? display.getEstimatedProfit() : null;
        row.setEstimatedProfit(est != null ? scale(est) : null);
        row.setProfitDiff(est != null ? scale(profit.subtract(est)) : null);
        return row;
    }

    /** 独立路径：直接聚合区间内已结算 settlement.profit（对账用） */
    private BigDecimal directSettledProfit(Date begin, Date end) {
        return directSettledSum(begin, end, true);
    }

    /** 独立路径：直接聚合区间内已结算 (ticket+addon) 实收（对账用） */
    private BigDecimal directSettledRevenue(Date begin, Date end) {
        return directSettledSum(begin, end, false);
    }

    private BigDecimal directSettledSum(Date begin, Date end, boolean profit) {
        List<PromotionActivity> acts = queryActivities(begin, end, null, null);
        if (CollUtil.isEmpty(acts)) {
            return BigDecimal.ZERO;
        }
        List<Long> actIds = acts.stream().map(PromotionActivity::getId).collect(Collectors.toList());
        List<ActivitySettlement> list = settlementMapper.selectList(
            Wrappers.<ActivitySettlement>lambdaQuery()
                .in(ActivitySettlement::getActivityId, actIds)
                .in(ActivitySettlement::getStatus, STATUS_SUBMITTED, STATUS_LOCKED));
        BigDecimal total = BigDecimal.ZERO;
        if (list != null) {
            for (ActivitySettlement s : list) {
                total = total.add(profit ? nvl(s.getProfit())
                    : nvl(s.getTicketRevenue()).add(nvl(s.getAddonRevenue())));
            }
        }
        return total;
    }

    // ===================================================================
    // 固定成本 / 税费 辅助
    // ===================================================================

    /**
     * 固定成本合计（T30.6 分摊 + 人力成本），与 BossDashboard 成本构成同口径。
     *
     * @param compositionOut 非空时回填 [{category,name,amount,percent}]
     */
    private BigDecimal fixedCostTotal(String month, List<Map<String, Object>> compositionOut) {
        List<MonthlyFixedCostVo> costs = fixedCostService.listActiveInMonth(month);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> categorySum = new LinkedHashMap<>();
        Map<String, String> categoryNameMap = new LinkedHashMap<>();
        for (MonthlyFixedCostVo c : costs) {
            BigDecimal amt = c.getMonthlyAmount() != null ? c.getMonthlyAmount() : BigDecimal.ZERO;
            total = total.add(amt);
            String cat = c.getCategory() != null ? c.getCategory() : "other";
            categorySum.merge(cat, amt, BigDecimal::add);
            categoryNameMap.putIfAbsent(cat, c.getCategoryName() != null ? c.getCategoryName() : cat);
        }
        BigDecimal laborCost = BigDecimal.ZERO;
        try {
            BigDecimal calc = staffCommissionService.sumMonthlyLaborCost(month);
            if (calc != null) {
                laborCost = calc;
            }
        } catch (Exception e) {
            log.warn("[OperatingReport] 人力成本聚合失败，按 0 计入: month={}", month, e);
        }
        if (laborCost.compareTo(BigDecimal.ZERO) > 0) {
            categorySum.merge("labor", laborCost, BigDecimal::add);
            categoryNameMap.putIfAbsent("labor", "人力成本");
        }
        BigDecimal grandTotal = total.add(laborCost);
        if (compositionOut != null) {
            final BigDecimal gt = grandTotal;
            categorySum.forEach((cat, amt) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("category", cat);
                item.put("name", categoryNameMap.getOrDefault(cat, cat));
                item.put("amount", scale(amt));
                item.put("percent", gt.compareTo(BigDecimal.ZERO) > 0
                    ? amt.multiply(new BigDecimal("100")).divide(gt, 1, RoundingMode.HALF_UP).doubleValue() : 0.0);
                compositionOut.add(item);
            });
            compositionOut.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        }
        return grandTotal;
    }

    /** 按率估算税费：税前利润 > 0 时 = 税前利润 × 税率% */
    private BigDecimal estimateTax(BigDecimal pretax, BigDecimal rate) {
        if (pretax == null || rate == null || pretax.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return pretax.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private MonthlyOperatingClosing findClosing(String month) {
        return closingMapper.selectOne(Wrappers.<MonthlyOperatingClosing>lambdaQuery()
            .eq(MonthlyOperatingClosing::getMonth, month)
            .orderByDesc(MonthlyOperatingClosing::getCreateTime)
            .last("LIMIT 1"));
    }

    private MonthlyClosingVo toClosingVo(MonthlyOperatingClosing closing) {
        MonthlyClosingVo vo = new MonthlyClosingVo();
        vo.setId(closing.getId());
        vo.setTenantId(closing.getTenantId());
        vo.setMonth(closing.getMonth());
        vo.setTaxAmount(closing.getTaxAmount());
        vo.setTaxRate(closing.getTaxRate());
        vo.setTaxMode(closing.getTaxMode());
        vo.setStatus(closing.getStatus());
        vo.setLocked(CLOSING_LOCKED.equals(closing.getStatus()));
        vo.setLockTime(closing.getLockTime());
        vo.setLockBy(closing.getLockBy());
        if (closing.getLockBy() != null) {
            vo.setLockByName(resolveStaffName(closing.getLockBy(), new HashMap<>()));
        }
        vo.setRemark(closing.getRemark());
        vo.setCreateTime(closing.getCreateTime());
        vo.setUpdateTime(closing.getUpdateTime());
        return vo;
    }

    // ===================================================================
    // 通用辅助
    // ===================================================================

    private List<Long> parseStaffIds(PromotionActivity act) {
        List<Long> ids = new ArrayList<>();
        if (StrUtil.isNotBlank(act.getSelectedStaffIds())) {
            try {
                List<Long> parsed = JsonUtils.parseArray(act.getSelectedStaffIds(), Long.class);
                if (parsed != null) {
                    ids.addAll(parsed);
                }
            } catch (Exception e) {
                log.warn("[OperatingReport] 解析 selectedStaffIds 失败: activityId={}, raw={}",
                    act.getId(), act.getSelectedStaffIds());
            }
        }
        if (ids.isEmpty()) {
            List<ActivityStaffSnapshot> snaps = snapshotMapper.selectList(
                Wrappers.<ActivityStaffSnapshot>lambdaQuery()
                    .eq(ActivityStaffSnapshot::getActivityId, act.getId()));
            if (snaps != null) {
                for (ActivityStaffSnapshot s : snaps) {
                    if (s.getStaffId() != null) {
                        ids.add(s.getStaffId());
                    }
                }
            }
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private String resolveStaffNames(List<Long> staffIds, Map<Long, String> cache) {
        if (CollUtil.isEmpty(staffIds)) {
            return "";
        }
        return staffIds.stream()
            .map(id -> {
                String name = cache.get(id);
                if (name == null) {
                    name = resolveStaffName(id, cache);
                    cache.put(id, name);
                }
                return name;
            })
            .filter(n -> n != null && !"-".equals(n))
            .collect(Collectors.joining("、"));
    }

    private String resolveStaffName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return "-";
        }
        String cached = cache.get(userId);
        if (cached != null) {
            return cached;
        }
        SysUser user = userMapper.selectById(userId);
        String name = user != null && StrUtil.isNotBlank(user.getNickName()) ? user.getNickName() : "-";
        cache.put(userId, name);
        return name;
    }

    /** createTime 比较（null 视为最早） */
    private int compareCreate(ActivitySettlement a, ActivitySettlement b) {
        Date ta = a.getCreateTime(), tb = b.getCreateTime();
        if (ta == null && tb == null) return 0;
        if (ta == null) return -1;
        if (tb == null) return 1;
        return ta.compareTo(tb);
    }

    private String normalizeMonth(String month) {
        if (StrUtil.isNotBlank(month)) {
            return month.trim();
        }
        return YearMonth.now().format(MONTH_FMT);
    }

    private String minusOneMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FMT).minusMonths(1).format(MONTH_FMT);
        } catch (Exception e) {
            return month;
        }
    }

    /** 月份区间 [首日 00:00:00, 末日 23:59:59.999] */
    private Date[] monthRange(String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month, MONTH_FMT);
        } catch (Exception e) {
            ym = YearMonth.now();
        }
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(ym.getYear(), ym.getMonthValue() - 1, 1, 0, 0, 0);
        Date begin = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, ym.lengthOfMonth());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date end = cal.getTime();
        return new Date[]{begin, end};
    }

    /** yyyy-MM-dd 字符串 → Date；endOfDay=true 时置 23:59:59.999 */
    private Date parseDate(String dateStr, boolean endOfDay) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr.trim());
            if (endOfDay) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                return cal.getTime();
            }
            return d;
        } catch (Exception e) {
            log.warn("[OperatingReport] 日期解析失败: {}", dateStr);
            return null;
        }
    }

    /** 保本进度（%）：分母 ≤ 0 时，分子 > 0 返回 100 否则 0 */
    private BigDecimal progress(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return numerator != null && numerator.compareTo(BigDecimal.ZERO) > 0
                ? new BigDecimal("100.0") : BigDecimal.ZERO.setScale(1);
        }
        return numerator.multiply(new BigDecimal("100"))
            .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<ActivityDetailRowVo> rows,
                           java.util.function.Function<ActivityDetailRowVo, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (ActivityDetailRowVo r : rows) {
            total = total.add(nvl(getter.apply(r)));
        }
        return total;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal scale(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String settleText(ActivityDetailRowVo r) {
        if (!Boolean.TRUE.equals(r.getSettled())) {
            return "未结算";
        }
        if (STATUS_LOCKED.equals(r.getSettlementStatus())) {
            return "已锁定";
        }
        if (STATUS_SUBMITTED.equals(r.getSettlementStatus())) {
            return "已提交";
        }
        return "已结算";
    }

    private String num(BigDecimal v) {
        return v != null ? v.toPlainString() : "0";
    }

    private String nz(Integer v) {
        return v != null ? String.valueOf(v) : "0";
    }

    /** CSV 字段转义（含逗号/引号/换行时加引号） */
    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
