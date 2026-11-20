package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.ActivitySettlementBo;
import com.ruoyi.system.domain.vo.ActivitySettlementVo;
import com.ruoyi.system.domain.vo.SettlementCostItemVo;
import com.ruoyi.system.domain.vo.SettlementIncomeItemVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IActivitySettlementService;
import com.ruoyi.system.service.IActivityStaffSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动结算单服务实现（M36 T36.1~T36.6）
 * <p>核心规则（§16）：
 * <ul>
 *   <li>代收类型（COLLECTED）不计入收入/利润/分账基数；</li>
 *   <li>员工成本由 activity_staff_snapshot 快照自动生成，不可手填；</li>
 *   <li>状态机：DRAFT → SUBMITTED → LOCKED，提交后不可编辑；</li>
 *   <li>退款单独记负向收入项（amount&lt;0），不改原金额；</li>
 *   <li>锁定后发现错误 → 追加调整单（is_adjustment=1，不覆盖原报表）。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySettlementServiceImpl implements IActivitySettlementService {

    private final ActivitySettlementMapper settlementMapper;
    private final SettlementIncomeItemMapper incomeItemMapper;
    private final SettlementCostItemMapper costItemMapper;
    private final IndustrySettlementTemplateMapper industryTemplateMapper;
    private final PromotionActivityMapper activityMapper;
    private final ActivityStaffSnapshotMapper snapshotMapper;
    private final StaffPricingMapper staffPricingMapper;
    private final IActivityStaffSnapshotService snapshotService;
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;

    /** 状态常量 */
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_LOCKED = "LOCKED";

    /** 收入项类型 */
    private static final String TYPE_TICKET = "TICKET";
    private static final String TYPE_ADDON = "ADDON";
    private static final String TYPE_COLLECTED = "COLLECTED";

    /** 成本项类型 */
    private static final String COST_EMPLOYEE = "EMPLOYEE";
    private static final String COST_OTHER = "OTHER";

    private static final ObjectMapper JSON = new ObjectMapper();

    // ===================================================================
    // T36.1/T36.3 查询
    // ===================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivitySettlementVo getByActivityId(Long activityId) {
        // ★fail-fast
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        // 查原单（is_adjustment=0）
        LambdaQueryWrapper<ActivitySettlement> wrapper = Wrappers.<ActivitySettlement>lambdaQuery()
            .eq(ActivitySettlement::getActivityId, activityId)
            .eq(ActivitySettlement::getIsAdjustment, 0)
            .orderByDesc(ActivitySettlement::getCreateTime)
            .last("LIMIT 1");
        ActivitySettlement settlement = settlementMapper.selectOne(wrapper);
        if (settlement == null) {
            // 不存在则自动初始化草稿（含行业模板收入项 + 员工成本快照）
            settlement = initDraft(activityId);
        } else if (STATUS_DRAFT.equals(settlement.getStatus())) {
            // ★DRAFT 状态自动重算员工成本（修复旧结算单员工成本为 0 的问题）
            recalculateEmployeeCost(settlement.getId());
            // 重算后重新从 DB 读取最新数据
            settlement = settlementMapper.selectById(settlement.getId());
        }
        return buildVo(settlement, true);
    }

    @Override
    public ActivitySettlementVo getById(Long id) {
        if (id == null) {
            throw new ServiceException("结算单ID不能为空");
        }
        ActivitySettlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw new ServiceException("结算单不存在");
        }
        return buildVo(settlement, true);
    }

    /**
     * 初始化草稿结算单（首次进入结算页自动创建）
     */
    @Transactional(rollbackFor = Exception.class)
    public ActivitySettlement initDraft(Long activityId) {
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在: " + activityId);
        }
        Long tenantId = LoginHelper.getTenantId();
        ActivitySettlement settlement = new ActivitySettlement();
        settlement.setTenantId(tenantId);
        settlement.setActivityId(activityId);
        settlement.setStatus(STATUS_DRAFT);
        settlement.setIsAdjustment(0);
        settlement.setTicketRevenue(BigDecimal.ZERO);
        settlement.setAddonRevenue(BigDecimal.ZERO);
        settlement.setCollectedAmount(BigDecimal.ZERO);
        settlement.setTotalRevenue(BigDecimal.ZERO);
        settlement.setEmployeeCost(BigDecimal.ZERO);
        settlement.setOtherCost(BigDecimal.ZERO);
        settlement.setTotalCost(BigDecimal.ZERO);
        settlement.setProfit(BigDecimal.ZERO);
        // ★预估利润不自动预填（autoCalculatedPrice 是活动总价而非利润，自动带入容易误导）
        // Boss 可在保存草稿时手动填入 estimatedProfit
        settlementMapper.insert(settlement);

        // ★活动实收金额（已结束活动有 receivedAmount，优先用；否则用 receivableAmount 应收）
        BigDecimal activityReceived = activity.getReceivedAmount();
        if (activityReceived == null || activityReceived.compareTo(BigDecimal.ZERO) == 0) {
            activityReceived = activity.getReceivableAmount();
        }
        if (activityReceived == null) {
            activityReceived = BigDecimal.ZERO;
        }
        int participantCount = activity.getParticipantCount() != null ? activity.getParticipantCount() : 0;

        // 按行业模板初始化收入项（T36.2）
        String industryCode = resolveIndustryCode(tenantId);
        List<IndustrySettlementTemplate> templates = industryTemplateMapper.selectList(
            Wrappers.<IndustrySettlementTemplate>lambdaQuery()
                .eq(IndustrySettlementTemplate::getIndustryCode, industryCode)
                .eq(IndustrySettlementTemplate::getStatus, "0")
                .orderByAsc(IndustrySettlementTemplate::getSortOrder));
        // 无行业模板时兜底一条默认票面行
        if (templates == null || templates.isEmpty()) {
            SettlementIncomeItem defaultTicket = new SettlementIncomeItem();
            defaultTicket.setTenantId(tenantId);
            defaultTicket.setSettlementId(settlement.getId());
            defaultTicket.setItemName("票面收入");
            defaultTicket.setItemType(TYPE_TICKET);
            // ★自动带入活动实收金额
            defaultTicket.setAmount(activityReceived);
            defaultTicket.setQuantity(participantCount);
            // 自动算单价 = 金额 / 人数
            if (participantCount > 0 && activityReceived.compareTo(BigDecimal.ZERO) > 0) {
                defaultTicket.setUnitPrice(activityReceived.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP));
            }
            defaultTicket.setIncludeInCommission(1);
            defaultTicket.setIncludeInProfit(1);
            defaultTicket.setIsDefaultTicket(1);
            defaultTicket.setSortOrder(1);
            incomeItemMapper.insert(defaultTicket);
        } else {
            int sort = 1;
            boolean ticketAmountFilled = false;
            for (IndustrySettlementTemplate tpl : templates) {
                SettlementIncomeItem item = new SettlementIncomeItem();
                item.setTenantId(tenantId);
                item.setSettlementId(settlement.getId());
                item.setItemName(tpl.getItemName());
                item.setItemType(tpl.getItemType());
                // ★第一个 TICKET 类型项自动带入活动实收金额（其余为 0，由 Boss 手填）
                if (TYPE_TICKET.equals(tpl.getItemType()) && !ticketAmountFilled && activityReceived.compareTo(BigDecimal.ZERO) > 0) {
                    item.setAmount(activityReceived);
                    item.setQuantity(participantCount);
                    if (participantCount > 0) {
                        item.setUnitPrice(activityReceived.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP));
                    }
                    ticketAmountFilled = true;
                } else {
                    item.setAmount(BigDecimal.ZERO);
                    item.setQuantity(tpl.getDefaultQuantity() != null ? tpl.getDefaultQuantity() : 1);
                }
                item.setIncludeInCommission(tpl.getIncludeInCommission());
                item.setIncludeInProfit(tpl.getIncludeInProfit());
                item.setIsDefaultTicket(tpl.getIsDefaultTicket());
                item.setSortOrder(sort++);
                item.setRemark(tpl.getRemark());
                incomeItemMapper.insert(item);
            }
        }

        // 从 activity_staff_snapshot 生成员工成本行（T36.1）
        recalculateEmployeeCost(settlement.getId());

        // ★重算汇总（把刚填入的收入项金额汇总到 settlement 主表）
        recalculateSummary(settlement);
        settlementMapper.updateById(settlement);

        log.info("[initDraft] 结算单草稿初始化完成: activityId={}, settlementId={}, industry={}, activityReceived={}",
            activityId, settlement.getId(), industryCode, activityReceived);
        return settlement;
    }

    // ===================================================================
    // T36.5 状态机 + 保存/提交/锁定
    // ===================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(ActivitySettlementBo bo) {
        // ★fail-fast
        if (bo == null) {
            throw new ServiceException("结算单参数不能为空");
        }
        if (bo.getActivityId() == null) {
            throw new ServiceException("活动ID不能为空");
        }
        ActivitySettlement settlement;
        if (bo.getId() != null) {
            settlement = settlementMapper.selectById(bo.getId());
            if (settlement == null) {
                throw new ServiceException("结算单不存在");
            }
            // ★状态机：仅 DRAFT 可编辑
            if (!STATUS_DRAFT.equals(settlement.getStatus())) {
                throw new ServiceException("结算单已" + statusText(settlement.getStatus()) + "，不可编辑；如需修改请追加调整单");
            }
        } else {
            // 新建：查是否已有原单
            ActivitySettlement exist = settlementMapper.selectOne(Wrappers.<ActivitySettlement>lambdaQuery()
                .eq(ActivitySettlement::getActivityId, bo.getActivityId())
                .eq(ActivitySettlement::getIsAdjustment, 0)
                .last("LIMIT 1"));
            if (exist != null) {
                settlement = exist;
                if (!STATUS_DRAFT.equals(settlement.getStatus())) {
                    throw new ServiceException("结算单已" + statusText(settlement.getStatus()) + "，不可编辑");
                }
            } else {
                settlement = initDraft(bo.getActivityId());
            }
        }

        Long tenantId = settlement.getTenantId();
        Long settlementId = settlement.getId();

        // 覆盖式更新收入项
        if (bo.getIncomeItems() != null) {
            // 软删原有非默认票面行（保留 is_default_ticket=1 的行由前端传入决定）
            incomeItemMapper.delete(Wrappers.<SettlementIncomeItem>lambdaQuery()
                .eq(SettlementIncomeItem::getSettlementId, settlementId));
            int sort = 1;
            for (ActivitySettlementBo.SettlementIncomeItemBo itemBo : bo.getIncomeItems()) {
                validateIncomeItem(itemBo);
                SettlementIncomeItem item = new SettlementIncomeItem();
                item.setTenantId(tenantId);
                item.setSettlementId(settlementId);
                item.setItemName(itemBo.getItemName());
                item.setItemType(itemBo.getItemType());
                item.setAmount(itemBo.getAmount() != null ? itemBo.getAmount() : BigDecimal.ZERO);
                item.setQuantity(itemBo.getQuantity() != null ? itemBo.getQuantity() : 1);
                item.setUnitPrice(itemBo.getUnitPrice());
                // 代收类型强制不计入分账/利润
                if (TYPE_COLLECTED.equals(itemBo.getItemType())) {
                    item.setIncludeInCommission(0);
                    item.setIncludeInProfit(0);
                } else {
                    item.setIncludeInCommission(itemBo.getIncludeInCommission() != null ? itemBo.getIncludeInCommission() : 1);
                    item.setIncludeInProfit(itemBo.getIncludeInProfit() != null ? itemBo.getIncludeInProfit() : 1);
                }
                item.setIsDefaultTicket(itemBo.getIsDefaultTicket() != null ? itemBo.getIsDefaultTicket() : 0);
                item.setSortOrder(itemBo.getSortOrder() != null ? itemBo.getSortOrder() : sort);
                item.setRemark(itemBo.getRemark());
                incomeItemMapper.insert(item);
                sort++;
            }
        }

        // 覆盖式更新其他成本项（EMPLOYEE 类型由后端自动生成，不接受前端传入）
        if (bo.getOtherCostItems() != null) {
            costItemMapper.delete(Wrappers.<SettlementCostItem>lambdaQuery()
                .eq(SettlementCostItem::getSettlementId, settlementId)
                .eq(SettlementCostItem::getCostType, COST_OTHER));
            int sort = 1;
            for (ActivitySettlementBo.SettlementCostItemBo costBo : bo.getOtherCostItems()) {
                if (costBo.getAmount() != null && costBo.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ServiceException("成本项金额不得为负: " + costBo.getItemName());
                }
                SettlementCostItem cost = new SettlementCostItem();
                cost.setTenantId(tenantId);
                cost.setSettlementId(settlementId);
                cost.setItemName(StrUtil.isNotBlank(costBo.getItemName()) ? costBo.getItemName() : "其他成本");
                cost.setCostType(COST_OTHER);
                cost.setAmount(costBo.getAmount() != null ? costBo.getAmount() : BigDecimal.ZERO);
                cost.setIsAutoGenerated(0);
                cost.setSortOrder(costBo.getSortOrder() != null ? costBo.getSortOrder() : sort);
                cost.setRemark(costBo.getRemark());
                costItemMapper.insert(cost);
                sort++;
            }
        }

        // 重算员工成本（快照自动生成）
        recalculateEmployeeCost(settlementId);

        // 重算汇总字段
        recalculateSummary(settlement);

        if (bo.getRemark() != null) {
            settlement.setRemark(bo.getRemark());
        }
        if (bo.getEstimatedProfit() != null) {
            settlement.setEstimatedProfit(bo.getEstimatedProfit());
            settlement.setProfitDiff(settlement.getProfit().subtract(bo.getEstimatedProfit()));
        }
        settlementMapper.updateById(settlement);
        return settlementId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        ActivitySettlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw new ServiceException("结算单不存在");
        }
        if (!STATUS_DRAFT.equals(settlement.getStatus())) {
            throw new ServiceException("仅草稿状态可提交，当前状态：" + statusText(settlement.getStatus()));
        }
        // ★T36.5 校验：收入合计为 0 禁止提交
        if (settlement.getTotalRevenue() == null || settlement.getTotalRevenue().compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("收入合计为 0，无法提交结算；请先填写票面/追加收入");
        }
        // ★T36.5 代收校验：存在代收收入但未配代付成本 → 弹提示不阻断（此处仅日志，前端根据 needCollectWarning 提示）
        checkCollectedPairing(settlement.getId());

        settlement.setStatus(STATUS_SUBMITTED);
        settlement.setSettledBy(LoginHelper.getUserId());
        settlement.setSettledTime(new Date());
        if (StrUtil.isBlank(settlement.getSettlementNo())) {
            settlement.setSettlementNo(generateSettlementNo());
        }
        settlementMapper.updateById(settlement);
        log.info("[submit] 结算单已提交: id={}, no={}", id, settlement.getSettlementNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long id) {
        ActivitySettlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw new ServiceException("结算单不存在");
        }
        if (!STATUS_SUBMITTED.equals(settlement.getStatus())) {
            throw new ServiceException("仅已提交状态可锁定，当前状态：" + statusText(settlement.getStatus()));
        }
        settlement.setStatus(STATUS_LOCKED);
        settlement.setLockedTime(new Date());
        settlementMapper.updateById(settlement);
        log.info("[lock] 结算单已锁定: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAdjustment(Long originId, ActivitySettlementBo bo) {
        ActivitySettlement origin = settlementMapper.selectById(originId);
        if (origin == null) {
            throw new ServiceException("原结算单不存在");
        }
        if (!STATUS_LOCKED.equals(origin.getStatus())) {
            throw new ServiceException("仅已锁定的结算单可追加调整单");
        }
        Long tenantId = origin.getTenantId();
        ActivitySettlement adjust = new ActivitySettlement();
        adjust.setTenantId(tenantId);
        adjust.setActivityId(origin.getActivityId());
        adjust.setStatus(STATUS_DRAFT);
        adjust.setIsAdjustment(1);
        adjust.setOriginSettlementId(originId);
        adjust.setTicketRevenue(BigDecimal.ZERO);
        adjust.setAddonRevenue(BigDecimal.ZERO);
        adjust.setCollectedAmount(BigDecimal.ZERO);
        adjust.setTotalRevenue(BigDecimal.ZERO);
        adjust.setEmployeeCost(BigDecimal.ZERO);
        adjust.setOtherCost(BigDecimal.ZERO);
        adjust.setTotalCost(BigDecimal.ZERO);
        adjust.setProfit(BigDecimal.ZERO);
        adjust.setRemark("调整单（原单 " + origin.getSettlementNo() + "）");
        settlementMapper.insert(adjust);

        // 调整单入参覆盖
        if (bo != null) {
            bo.setId(adjust.getId());
            bo.setActivityId(origin.getActivityId());
            saveDraft(bo);
        }
        log.info("[createAdjustment] 调整单已创建: originId={}, adjustId={}", originId, adjust.getId());
        return adjust.getId();
    }

    // ===================================================================
    // T36.4 员工端只读视图
    // ===================================================================

    @Override
    public ActivitySettlementVo getStaffReadonlyView(Long activityId, Long staffUserId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        if (staffUserId == null) {
            throw new ServiceException("员工用户ID不能为空");
        }
        ActivitySettlement settlement = settlementMapper.selectOne(Wrappers.<ActivitySettlement>lambdaQuery()
            .eq(ActivitySettlement::getActivityId, activityId)
            .eq(ActivitySettlement::getIsAdjustment, 0)
            .orderByDesc(ActivitySettlement::getCreateTime)
            .last("LIMIT 1"));
        if (settlement == null) {
            throw new ServiceException("该活动尚未生成结算单");
        }
        ActivitySettlementVo vo = buildVo(settlement, false);
        // ★权限收窄：成本项仅可见自己那条员工成本行
        if (vo.getCostItems() != null) {
            List<SettlementCostItemVo> mine = vo.getCostItems().stream()
                .filter(c -> COST_EMPLOYEE.equals(c.getCostType()) && staffUserId.equals(c.getStaffId()))
                .collect(Collectors.toList());
            vo.setCostItems(mine);
        }
        // ★合计利润区不可见：置空汇总字段（前端据此隐藏）
        vo.setTotalRevenue(null);
        vo.setEmployeeCost(null);
        vo.setOtherCost(null);
        vo.setTotalCost(null);
        vo.setProfit(null);
        vo.setEstimatedProfit(null);
        vo.setProfitDiff(null);
        return vo;
    }

    // ===================================================================
    // T36.2 行业模板
    // ===================================================================

    @Override
    public List<ActivitySettlementBo.SettlementIncomeItemBo> initIncomeItemsByIndustry(String industryCode) {
        if (StrUtil.isBlank(industryCode)) {
            return Collections.emptyList();
        }
        List<IndustrySettlementTemplate> templates = industryTemplateMapper.selectList(
            Wrappers.<IndustrySettlementTemplate>lambdaQuery()
                .eq(IndustrySettlementTemplate::getIndustryCode, industryCode)
                .eq(IndustrySettlementTemplate::getStatus, "0")
                .orderByAsc(IndustrySettlementTemplate::getSortOrder));
        if (templates == null) {
            return Collections.emptyList();
        }
        List<ActivitySettlementBo.SettlementIncomeItemBo> result = new ArrayList<>();
        int sort = 1;
        for (IndustrySettlementTemplate tpl : templates) {
            ActivitySettlementBo.SettlementIncomeItemBo bo = new ActivitySettlementBo.SettlementIncomeItemBo();
            bo.setItemName(tpl.getItemName());
            bo.setItemType(tpl.getItemType());
            bo.setAmount(BigDecimal.ZERO);
            bo.setQuantity(tpl.getDefaultQuantity() != null ? tpl.getDefaultQuantity() : 1);
            bo.setIncludeInCommission(tpl.getIncludeInCommission());
            bo.setIncludeInProfit(tpl.getIncludeInProfit());
            bo.setIsDefaultTicket(tpl.getIsDefaultTicket());
            bo.setSortOrder(sort++);
            bo.setRemark(tpl.getRemark());
            result.add(bo);
        }
        return result;
    }

    // ===================================================================
    // T36.1 员工成本重算（快照自动生成）
    // ===================================================================

    /**
     * 根据活动选中员工的 staff_pricing（资源价格设置）计算员工成本
     * <p>FIXED（一口价）：直接取 hourlyRate（基准价=固定值）
     * <p>其他（duration/times/package）：hourlyRate × 活动时长（小时）
     * <p>无定价记录或 hourlyRate=0：成本为 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateEmployeeCost(Long settlementId) {
        ActivitySettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            return;
        }
        Long activityId = settlement.getActivityId();
        Long tenantId = settlement.getTenantId();
        // 删除原有 EMPLOYEE 成本行
        costItemMapper.delete(Wrappers.<SettlementCostItem>lambdaQuery()
            .eq(SettlementCostItem::getSettlementId, settlementId)
            .eq(SettlementCostItem::getCostType, COST_EMPLOYEE));
        // 从 activity_staff_snapshot 读取快照
        List<ActivityStaffSnapshot> snapshots = snapshotMapper.selectList(
            Wrappers.<ActivityStaffSnapshot>lambdaQuery()
                .eq(ActivityStaffSnapshot::getActivityId, activityId));
        if (snapshots == null || snapshots.isEmpty()) {
            // ★FALLBACK：无快照时，从活动的 selectedStaffIds 自动生成（兼容历史活动）
            log.info("[recalculateEmployeeCost] activityId={} 无快照，尝试从 selectedStaffIds 生成", activityId);
            PromotionActivity activity = activityMapper.selectById(activityId);
            if (activity != null && StrUtil.isNotBlank(activity.getSelectedStaffIds())) {
                try {
                    List<Long> staffIds = JSON.readValue(activity.getSelectedStaffIds(), new TypeReference<List<Long>>() {});
                    if (CollUtil.isNotEmpty(staffIds)) {
                        snapshotService.rebuildSnapshots(activityId, staffIds);
                        log.info("[recalculateEmployeeCost] 从 selectedStaffIds 生成快照成功 activityId={}, staffIds={}", activityId, staffIds);
                        // 重新查询快照
                        snapshots = snapshotMapper.selectList(
                            Wrappers.<ActivityStaffSnapshot>lambdaQuery()
                                .eq(ActivityStaffSnapshot::getActivityId, activityId));
                    }
                } catch (Exception e) {
                    log.warn("[recalculateEmployeeCost] 从 selectedStaffIds 生成快照失败 activityId={}", activityId, e);
                }
            }
            if (snapshots == null || snapshots.isEmpty()) {
                log.warn("[recalculateEmployeeCost] 无员工快照 activityId={}，员工成本为0", activityId);
                return;
            }
        }

        // ★计算活动时长（小时），与 calculateAndSetAutoPrice 口径一致
        PromotionActivity activity = activityMapper.selectById(activityId);
        int hours = 0;
        if (activity != null && activity.getStartTime() != null && activity.getEndTime() != null) {
            long diffMs = activity.getEndTime().getTime() - activity.getStartTime().getTime();
            if (diffMs > 0) {
                hours = (int) Math.ceil(diffMs / 3600000.0);
            }
        }
        log.info("[recalculateEmployeeCost] activityId={}, hours={}, snapshots={}", activityId, hours, snapshots.size());

        // ★批量查询员工定价（staff_pricing），与 calculateAndSetAutoPrice 口径一致
        List<Long> staffIds = snapshots.stream()
            .map(ActivityStaffSnapshot::getStaffId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, StaffPricing> pricingMap = new HashMap<>();
        if (!staffIds.isEmpty()) {
            List<StaffPricing> pricings = staffPricingMapper.selectList(
                Wrappers.<StaffPricing>lambdaQuery()
                    .in(StaffPricing::getUserId, staffIds));
            for (StaffPricing p : pricings) {
                pricingMap.put(p.getUserId(), p);
            }
            log.info("[recalculateEmployeeCost] staffIds={}, pricingMapSize={}", staffIds, pricingMap.size());
            // ★诊断：打印每个员工的定价详情
            for (Long sid : staffIds) {
                StaffPricing sp = pricingMap.get(sid);
                if (sp == null) {
                    log.warn("[recalculateEmployeeCost] 员工 {} 无 staff_pricing 记录", sid);
                } else {
                    log.info("[recalculateEmployeeCost] 员工 {} pricing: hourlyRate={}, pricingType={}", sid, sp.getHourlyRate(), sp.getPricingType());
                }
            }
        }

        BigDecimal totalEmployee = BigDecimal.ZERO;
        int sort = 1;
        for (ActivityStaffSnapshot snap : snapshots) {
            String staffName = resolveStaffName(snap.getStaffId());
            StaffPricing pricing = pricingMap.get(snap.getStaffId());
            BigDecimal amount = calcStaffCostFromPricing(snap, pricingMap, hours);
            // ★回写 calculatedAmount 到快照
            snap.setCalculatedAmount(amount);
            snapshotMapper.updateById(snap);

            // ★按定价类型拆分明细行
            if (pricing != null && "fixed".equals(pricing.getPricingType())) {
                // 一口价：拆分包干价 + 超时费
                BigDecimal basePrice = pricing.getHourlyRate().setScale(2, RoundingMode.HALF_UP);
                int includedDuration = 0;
                BigDecimal overtimePrice = null;
                if (StrUtil.isNotBlank(pricing.getExtraConfig()) && !"{}".equals(pricing.getExtraConfig())) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> extra = JSON.readValue(pricing.getExtraConfig(), Map.class);
                        Object incDur = extra.get("includedDuration");
                        if (incDur != null) includedDuration = new BigDecimal(incDur.toString()).intValue();
                        Object otp = extra.get("overtimePrice");
                        if (otp != null) overtimePrice = new BigDecimal(otp.toString()).setScale(2, RoundingMode.HALF_UP);
                    } catch (Exception ignored) {}
                }
                // 行1：包干价
                SettlementCostItem baseCost = new SettlementCostItem();
                baseCost.setTenantId(tenantId);
                baseCost.setSettlementId(settlementId);
                baseCost.setItemName("员工分成-" + staffName + " · 包干价");
                baseCost.setCostType(COST_EMPLOYEE);
                baseCost.setAmount(basePrice);
                baseCost.setStaffId(snap.getStaffId());
                baseCost.setSnapshotId(snap.getId());
                baseCost.setIsAutoGenerated(1);
                baseCost.setSortOrder(sort++);
                baseCost.setRemark("包干价 " + basePrice + " 元（含 " + includedDuration + " 小时）");
                costItemMapper.insert(baseCost);
                totalEmployee = totalEmployee.add(basePrice);

                // 行2：超时费（有超时时才生成）
                if (overtimePrice != null && overtimePrice.compareTo(BigDecimal.ZERO) > 0 && hours > includedDuration) {
                    int overtimeHours = hours - includedDuration;
                    BigDecimal overtimeCost = overtimePrice.multiply(BigDecimal.valueOf(overtimeHours)).setScale(2, RoundingMode.HALF_UP);
                    SettlementCostItem otCost = new SettlementCostItem();
                    otCost.setTenantId(tenantId);
                    otCost.setSettlementId(settlementId);
                    otCost.setItemName("员工分成-" + staffName + " · 超时费");
                    otCost.setCostType(COST_EMPLOYEE);
                    otCost.setAmount(overtimeCost);
                    otCost.setStaffId(snap.getStaffId());
                    otCost.setSnapshotId(snap.getId());
                    otCost.setIsAutoGenerated(1);
                    otCost.setSortOrder(sort++);
                    otCost.setRemark("超时 " + overtimeHours + " 小时 × " + overtimePrice + " 元/小时 = " + overtimeCost + " 元");
                    costItemMapper.insert(otCost);
                    totalEmployee = totalEmployee.add(overtimeCost);
                }
            } else {
                // 计时/计量/套餐/无定价：单行
                SettlementCostItem cost = new SettlementCostItem();
                cost.setTenantId(tenantId);
                cost.setSettlementId(settlementId);
                cost.setItemName("员工分成-" + staffName);
                cost.setCostType(COST_EMPLOYEE);
                cost.setAmount(amount);
                cost.setStaffId(snap.getStaffId());
                cost.setSnapshotId(snap.getId());
                cost.setIsAutoGenerated(1);
                cost.setSortOrder(sort++);
                if (pricing != null && hours > 0) {
                    cost.setRemark(pricing.getHourlyRate() + " 元/小时 × " + hours + " 小时 = " + amount + " 元");
                }
                costItemMapper.insert(cost);
                totalEmployee = totalEmployee.add(amount);
            }
        }
        settlement.setEmployeeCost(totalEmployee);
        settlementMapper.updateById(settlement);
    }

    /**
     * 从 staff_pricing（资源价格设置）计算单个员工的成本
     * <p>FIXED（一口价）：hourlyRate 为包干价，extra_config 含 includedDuration（含时长）+ overtimePrice（超时费）
     * <p>  成本 = hourlyRate + max(0, hours - includedDuration) × overtimePrice
     * <p>其他（duration/times/package）：hourlyRate × 活动时长（小时）
     * <p>无定价记录或 hourlyRate=0：成本为 0
     */
    private BigDecimal calcStaffCostFromPricing(ActivityStaffSnapshot snap,
                                                 Map<Long, StaffPricing> pricingMap, int hours) {
        if (snap == null || snap.getStaffId() == null) {
            return BigDecimal.ZERO;
        }
        StaffPricing pricing = pricingMap.get(snap.getStaffId());
        if (pricing == null) {
            log.warn("[calcStaffCostFromPricing] staffId={} 无定价记录，成本=0", snap.getStaffId());
            return BigDecimal.ZERO;
        }
        if (pricing.getHourlyRate() == null || pricing.getHourlyRate().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("[calcStaffCostFromPricing] staffId={} hourlyRate={}，成本=0", snap.getStaffId(), pricing.getHourlyRate());
            return BigDecimal.ZERO;
        }
        BigDecimal rate = pricing.getHourlyRate();
        // 一口价：包干价 + 超时费
        if ("fixed".equals(pricing.getPricingType())) {
            BigDecimal basePrice = rate.setScale(2, RoundingMode.HALF_UP);
            // 解析 extra_config 获取含时长和超时费
            int includedDuration = 0;
            BigDecimal overtimePrice = null;
            if (StrUtil.isNotBlank(pricing.getExtraConfig()) && !"{}".equals(pricing.getExtraConfig())) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> extra = JSON.readValue(pricing.getExtraConfig(), Map.class);
                    Object incDur = extra.get("includedDuration");
                    if (incDur != null) {
                        includedDuration = new BigDecimal(incDur.toString()).intValue();
                    }
                    Object otp = extra.get("overtimePrice");
                    if (otp != null) {
                        overtimePrice = new BigDecimal(otp.toString()).setScale(2, RoundingMode.HALF_UP);
                    }
                } catch (Exception e) {
                    log.warn("[calcStaffCostFromPricing] extraConfig 解析失败 staffId={}", snap.getStaffId(), e);
                }
            }
            // 超时费计算
            BigDecimal overtimeCost = BigDecimal.ZERO;
            if (overtimePrice != null && overtimePrice.compareTo(BigDecimal.ZERO) > 0 && hours > includedDuration) {
                int overtimeHours = hours - includedDuration;
                overtimeCost = overtimePrice.multiply(BigDecimal.valueOf(overtimeHours)).setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal total = basePrice.add(overtimeCost);
            log.info("[calcStaffCostFromPricing] staffId={} fixed模式, 包干价={}, 含{}h, 超时费={}×{}h={}, 总成本={}",
                snap.getStaffId(), basePrice, includedDuration, overtimePrice, Math.max(0, hours - includedDuration), overtimeCost, total);
            return total;
        }
        // 计时/计量/套餐：基准价 × 时长
        if (hours <= 0) {
            log.warn("[calcStaffCostFromPricing] staffId={} 非fixed模式但hours={}，成本=0", snap.getStaffId(), hours);
            return BigDecimal.ZERO;
        }
        BigDecimal result = rate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
        log.info("[calcStaffCostFromPricing] staffId={} {}模式, rate={}×hours={}, 成本={}", snap.getStaffId(), pricing.getPricingType(), rate, hours, result);
        return result;
    }

    // ===================================================================
    // T36.3 导出 CSV
    // ===================================================================

    @Override
    public String exportCsv(Long id) {
        ActivitySettlementVo vo = getById(id);
        StringBuilder sb = new StringBuilder();
        sb.append("活动结算单\n");
        sb.append("结算单号,").append(nullSafe(vo.getSettlementNo())).append("\n");
        sb.append("活动名称,").append(nullSafe(vo.getActivityTitle())).append("\n");
        sb.append("状态,").append(statusText(vo.getStatus())).append("\n\n");
        sb.append("【收入项】\n");
        sb.append("名称,类型,数量,单价,金额,计入分账,计入利润\n");
        if (vo.getIncomeItems() != null) {
            for (SettlementIncomeItemVo item : vo.getIncomeItems()) {
                sb.append(nullSafe(item.getItemName())).append(",")
                    .append(incomeTypeText(item.getItemType())).append(",")
                    .append(item.getQuantity() != null ? item.getQuantity() : "").append(",")
                    .append(item.getUnitPrice() != null ? item.getUnitPrice() : "").append(",")
                    .append(item.getAmount() != null ? item.getAmount() : "0").append(",")
                    .append(intToYesNo(item.getIncludeInCommission())).append(",")
                    .append(intToYesNo(item.getIncludeInProfit())).append("\n");
            }
        }
        sb.append("\n【成本项】\n");
        sb.append("名称,类型,金额,员工,自动生成\n");
        if (vo.getCostItems() != null) {
            for (SettlementCostItemVo cost : vo.getCostItems()) {
                sb.append(nullSafe(cost.getItemName())).append(",")
                    .append(costTypeText(cost.getCostType())).append(",")
                    .append(cost.getAmount() != null ? cost.getAmount() : "0").append(",")
                    .append(nullSafe(cost.getStaffName())).append(",")
                    .append(intToYesNo(cost.getIsAutoGenerated())).append("\n");
            }
        }
        sb.append("\n【合计】\n");
        sb.append("票面收入,").append(vo.getTicketRevenue() != null ? vo.getTicketRevenue() : "0").append("\n");
        sb.append("追加收入,").append(vo.getAddonRevenue() != null ? vo.getAddonRevenue() : "0").append("\n");
        sb.append("代收金额,").append(vo.getCollectedAmount() != null ? vo.getCollectedAmount() : "0").append("\n");
        sb.append("收入合计,").append(vo.getTotalRevenue() != null ? vo.getTotalRevenue() : "0").append("\n");
        sb.append("员工成本,").append(vo.getEmployeeCost() != null ? vo.getEmployeeCost() : "0").append("\n");
        sb.append("其他成本,").append(vo.getOtherCost() != null ? vo.getOtherCost() : "0").append("\n");
        sb.append("成本合计,").append(vo.getTotalCost() != null ? vo.getTotalCost() : "0").append("\n");
        sb.append("利润,").append(vo.getProfit() != null ? vo.getProfit() : "0").append("\n");
        sb.append("预估利润,").append(vo.getEstimatedProfit() != null ? vo.getEstimatedProfit() : "").append("\n");
        sb.append("差额,").append(vo.getProfitDiff() != null ? vo.getProfitDiff() : "").append("\n");
        return sb.toString();
    }

    // ===================================================================
    // T36.6 收工时追加消费提醒
    // ===================================================================

    @Override
    public boolean needAddonReminder(Long settlementId) {
        if (settlementId == null) {
            return false;
        }
        Long addonCount = incomeItemMapper.selectCount(Wrappers.<SettlementIncomeItem>lambdaQuery()
            .eq(SettlementIncomeItem::getSettlementId, settlementId)
            .eq(SettlementIncomeItem::getItemType, TYPE_ADDON)
            .gt(SettlementIncomeItem::getAmount, BigDecimal.ZERO));
        return addonCount == null || addonCount == 0;
    }

    // ===================================================================
    // 私有辅助方法
    // ===================================================================

    /**
     * 构建聚合 VO（含收入项/成本项列表 + 活动信息冗余）
     */
    private ActivitySettlementVo buildVo(ActivitySettlement settlement, boolean includeAllCosts) {
        ActivitySettlementVo vo = BeanCopyUtils.copy(settlement, ActivitySettlementVo.class);
        // 活动信息冗余
        PromotionActivity activity = activityMapper.selectById(settlement.getActivityId());
        if (activity != null) {
            vo.setActivityTitle(activity.getTitle());
            vo.setActivityStartTime(activity.getStartTime());
            vo.setActivityEndTime(activity.getEndTime());
            vo.setParticipantCount(activity.getParticipantCount());
        }
        // 结算人姓名回填
        if (settlement.getSettledBy() != null) {
            vo.setSettledByName(resolveStaffName(settlement.getSettledBy()));
        }
        // 收入项列表
        List<SettlementIncomeItem> incomeList = incomeItemMapper.selectList(
            Wrappers.<SettlementIncomeItem>lambdaQuery()
                .eq(SettlementIncomeItem::getSettlementId, settlement.getId())
                .orderByAsc(SettlementIncomeItem::getSortOrder));
        vo.setIncomeItems(incomeList != null ? BeanCopyUtils.copyList(incomeList, SettlementIncomeItemVo.class) : Collections.emptyList());
        // 成本项列表
        List<SettlementCostItem> costList = costItemMapper.selectList(
            Wrappers.<SettlementCostItem>lambdaQuery()
                .eq(SettlementCostItem::getSettlementId, settlement.getId())
                .orderByAsc(SettlementCostItem::getSortOrder));
        List<SettlementCostItemVo> costVos = costList != null ? BeanCopyUtils.copyList(costList, SettlementCostItemVo.class) : new ArrayList<>();
        // 回填员工姓名
        for (SettlementCostItemVo c : costVos) {
            if (c.getStaffId() != null) {
                c.setStaffName(resolveStaffName(c.getStaffId()));
            }
        }
        vo.setCostItems(costVos);
        // ★T36.6 追加消费提醒标志
        vo.setNeedAddonReminder(needAddonReminder(settlement.getId()));
        return vo;
    }

    /**
     * 重算汇总字段（收入合计/成本合计/利润）
     */
    private void recalculateSummary(ActivitySettlement settlement) {
        Long settlementId = settlement.getId();
        List<SettlementIncomeItem> incomeList = incomeItemMapper.selectList(
            Wrappers.<SettlementIncomeItem>lambdaQuery()
                .eq(SettlementIncomeItem::getSettlementId, settlementId));
        BigDecimal ticket = BigDecimal.ZERO;
        BigDecimal addon = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        if (incomeList != null) {
            for (SettlementIncomeItem item : incomeList) {
                BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                if (TYPE_TICKET.equals(item.getItemType())) {
                    ticket = ticket.add(amt);
                } else if (TYPE_ADDON.equals(item.getItemType())) {
                    addon = addon.add(amt);
                } else if (TYPE_COLLECTED.equals(item.getItemType())) {
                    collected = collected.add(amt);
                }
            }
        }
        settlement.setTicketRevenue(ticket);
        settlement.setAddonRevenue(addon);
        settlement.setCollectedAmount(collected);
        // ★代收不计入收入合计
        settlement.setTotalRevenue(ticket.add(addon));

        List<SettlementCostItem> costList = costItemMapper.selectList(
            Wrappers.<SettlementCostItem>lambdaQuery()
                .eq(SettlementCostItem::getSettlementId, settlementId));
        BigDecimal employee = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        if (costList != null) {
            for (SettlementCostItem cost : costList) {
                BigDecimal amt = cost.getAmount() != null ? cost.getAmount() : BigDecimal.ZERO;
                if (COST_EMPLOYEE.equals(cost.getCostType())) {
                    employee = employee.add(amt);
                } else {
                    other = other.add(amt);
                }
            }
        }
        settlement.setEmployeeCost(employee);
        settlement.setOtherCost(other);
        settlement.setTotalCost(employee.add(other));
        // 利润 = 收入合计 - 成本合计
        settlement.setProfit(settlement.getTotalRevenue().subtract(settlement.getTotalCost()));
        if (settlement.getEstimatedProfit() != null) {
            settlement.setProfitDiff(settlement.getProfit().subtract(settlement.getEstimatedProfit()));
        }
    }

    /**
     * 校验收入项
     */
    private void validateIncomeItem(ActivitySettlementBo.SettlementIncomeItemBo itemBo) {
        if (StrUtil.isBlank(itemBo.getItemName())) {
            throw new ServiceException("收入项名称不能为空");
        }
        if (StrUtil.isBlank(itemBo.getItemType())) {
            throw new ServiceException("收入项类型不能为空");
        }
        if (!Arrays.asList(TYPE_TICKET, TYPE_ADDON, TYPE_COLLECTED).contains(itemBo.getItemType())) {
            throw new ServiceException("收入项类型非法: " + itemBo.getItemType());
        }
        // 退款允许负值，其他情况金额不得为负
        if (itemBo.getAmount() != null && itemBo.getAmount().compareTo(BigDecimal.ZERO) < 0
            && StrUtil.isBlank(itemBo.getRemark())) {
            throw new ServiceException("负向金额（退款）必须填写备注说明原因: " + itemBo.getItemName());
        }
    }

    /**
     * 代收代付配对检查（T36.5：存在代收收入但未配代付成本 → 弹提示不阻断）
     */
    private void checkCollectedPairing(Long settlementId) {
        Long collectedCount = incomeItemMapper.selectCount(Wrappers.<SettlementIncomeItem>lambdaQuery()
            .eq(SettlementIncomeItem::getSettlementId, settlementId)
            .eq(SettlementIncomeItem::getItemType, TYPE_COLLECTED)
            .gt(SettlementIncomeItem::getAmount, BigDecimal.ZERO));
        if (collectedCount == null || collectedCount == 0) {
            return;
        }
        // 检查是否有代付成本（OTHER 类型 + remark 含"代付"）
        Long payCount = costItemMapper.selectCount(Wrappers.<SettlementCostItem>lambdaQuery()
            .eq(SettlementCostItem::getSettlementId, settlementId)
            .eq(SettlementCostItem::getCostType, COST_OTHER)
            .like(SettlementCostItem::getRemark, "代付"));
        if (payCount == null || payCount == 0) {
            log.warn("[checkCollectedPairing] 结算单 {} 存在代收收入但未配代付成本，请核对现金流", settlementId);
        }
    }

    /**
     * 生成结算单号（ST + yyyyMMdd + 4 位序号）
     */
    private String generateSettlementNo() {
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Long count = settlementMapper.selectCount(Wrappers.<ActivitySettlement>lambdaQuery()
            .likeRight(ActivitySettlement::getSettlementNo, "ST" + datePart));
        long seq = (count != null ? count : 0) + 1;
        return "ST" + datePart + String.format("%04d", seq);
    }

    /**
     * 解析租户行业编码（从 sys_tenant.industry 读取）
     * <p>★sys_tenant 在 IGNORE_TENANT_TABLES，需显式查询；失败兜底 board_game。
     */
    private String resolveIndustryCode(Long tenantId) {
        if (tenantId == null) {
            return "board_game";
        }
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getIndustry())) {
                return tenant.getIndustry();
            }
        } catch (Exception e) {
            log.warn("[resolveIndustryCode] 读取租户行业失败，兜底 board_game: tenantId={}, err={}", tenantId, e.getMessage());
        }
        return "board_game";
    }

    /**
     * 解析员工姓名（sys_user.nick_name）
     */
    private String resolveStaffName(Long userId) {
        if (userId == null) {
            return "-";
        }
        SysUser user = userMapper.selectById(userId);
        return user != null && StrUtil.isNotBlank(user.getNickName()) ? user.getNickName() : "-";
    }

    private String statusText(String status) {
        if (STATUS_DRAFT.equals(status)) return "草稿";
        if (STATUS_SUBMITTED.equals(status)) return "已提交";
        if (STATUS_LOCKED.equals(status)) return "已锁定";
        return status != null ? status : "-";
    }

    private String incomeTypeText(String type) {
        if (TYPE_TICKET.equals(type)) return "票面";
        if (TYPE_ADDON.equals(type)) return "追加";
        if (TYPE_COLLECTED.equals(type)) return "代收";
        return type != null ? type : "-";
    }

    private String costTypeText(String type) {
        if (COST_EMPLOYEE.equals(type)) return "员工成本";
        if (COST_OTHER.equals(type)) return "其他成本";
        return type != null ? type : "-";
    }

    private String intToYesNo(Integer v) {
        return v != null && v == 1 ? "是" : "否";
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
