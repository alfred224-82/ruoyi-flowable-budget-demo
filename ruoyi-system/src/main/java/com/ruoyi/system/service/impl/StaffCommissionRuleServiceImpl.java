package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.StaffCommissionRule;
import com.ruoyi.system.domain.bo.StaffCommissionRuleBo;
import com.ruoyi.system.domain.vo.ActivityPricingVo;
import com.ruoyi.system.domain.vo.EstimateVsActualVo;
import com.ruoyi.system.domain.vo.StaffCommissionRuleVo;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.StaffCommissionRuleMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IActivityStaffSnapshotService;
import com.ruoyi.system.service.IStaffCommissionRuleService;
import com.ruoyi.system.service.IStaffCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 员工分成规则服务实现（M35 T35.2~T35.7）
 * <p>staff_commission_rule 为租户级表，多租户插件自动隔离。
 * <p>★fail-soft：任何聚合均做 null 保护，规则为空/停用时试算金额为 0，不抛异常。
 * <p>★T35.4 保本人数公式：breakeven = ceil(totalCost ÷ unitContribution)；
 *   unitContribution = unitPrice - 单人变动员工成本（PERCENTAGE 类型按 unitPrice×rate/100 计）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffCommissionRuleServiceImpl implements IStaffCommissionRuleService {

    private final StaffCommissionRuleMapper ruleMapper;
    private final PromotionActivityMapper activityMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final IStaffCommissionService staffCommissionService;
    private final IActivityStaffSnapshotService snapshotService;

    @Override
    public TableDataInfo<StaffCommissionRuleVo> queryPageList(StaffCommissionRuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StaffCommissionRule> wrapper = buildQueryWrapper(bo);
        Page<StaffCommissionRuleVo> page = ruleMapper.selectVoPage(pageQuery.build(), wrapper);
        fillExtraInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<StaffCommissionRuleVo> queryList(StaffCommissionRuleBo bo) {
        List<StaffCommissionRuleVo> list = ruleMapper.selectVoList(buildQueryWrapper(bo));
        fillExtraInfo(list);
        return list;
    }

    @Override
    public StaffCommissionRuleVo queryById(Long id) {
        StaffCommissionRuleVo vo = ruleMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("员工分成规则不存在");
        }
        fillExtraInfo(CollUtil.newArrayList(vo));
        return vo;
    }

    @Override
    public StaffCommissionRule getActiveByStaffId(Long staffId) {
        if (staffId == null) {
            return null;
        }
        return ruleMapper.selectOne(Wrappers.<StaffCommissionRule>lambdaQuery()
            .eq(StaffCommissionRule::getStaffId, staffId)
            .eq(StaffCommissionRule::getIsActive, 1)
            .last("LIMIT 1"));
    }

    @Override
    public Map<Long, StaffCommissionRule> listActiveByStaffIds(Collection<Long> staffIds) {
        if (CollUtil.isEmpty(staffIds)) {
            return Collections.emptyMap();
        }
        List<StaffCommissionRule> list = ruleMapper.selectList(Wrappers.<StaffCommissionRule>lambdaQuery()
            .in(StaffCommissionRule::getStaffId, staffIds)
            .eq(StaffCommissionRule::getIsActive, 1));
        Map<Long, StaffCommissionRule> map = new HashMap<>();
        for (StaffCommissionRule r : list) {
            map.put(r.getStaffId(), r);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(StaffCommissionRuleBo bo) {
        validateByType(bo);
        Long dup = ruleMapper.selectCount(Wrappers.<StaffCommissionRule>lambdaQuery()
            .eq(StaffCommissionRule::getStaffId, bo.getStaffId()));
        if (dup != null && dup > 0) {
            throw new ServiceException("该员工已存在分成规则，请直接修改");
        }
        StaffCommissionRule entity = BeanCopyUtils.copy(bo, StaffCommissionRule.class);
        entity.setId(null);
        if (entity.getIsActive() == null) {
            entity.setIsActive(1);
        }
        if (entity.getIncludeAddonInCommission() == null) {
            entity.setIncludeAddonInCommission(0);
        }
        ruleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(StaffCommissionRuleBo bo) {
        StaffCommissionRule exist = ruleMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("员工分成规则不存在");
        }
        validateByType(bo);
        if (bo.getStaffId() != null && !bo.getStaffId().equals(exist.getStaffId())) {
            Long dup = ruleMapper.selectCount(Wrappers.<StaffCommissionRule>lambdaQuery()
                .eq(StaffCommissionRule::getStaffId, bo.getStaffId())
                .ne(StaffCommissionRule::getId, bo.getId()));
            if (dup != null && dup > 0) {
                throw new ServiceException("该员工已存在分成规则，请勿重复配置");
            }
        }
        StaffCommissionRule update = BeanCopyUtils.copy(bo, StaffCommissionRule.class);
        return ruleMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return ruleMapper.deleteBatchIds(ids);
    }

    @Override
    public List<Map<String, Object>> listStaffOptions() {
        return staffCommissionService.listStaffOptions();
    }

    // ===== T35.4 定价页自动算账 =====

    @Override
    public ActivityPricingVo calcPricing(List<Long> staffIds, BigDecimal unitPrice,
                                         Integer expectedHeadcount, BigDecimal otherCost) {
        ActivityPricingVo vo = new ActivityPricingVo();
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        int headcount = expectedHeadcount == null || expectedHeadcount < 0 ? 0 : expectedHeadcount;
        BigDecimal other = otherCost == null ? BigDecimal.ZERO : otherCost;

        vo.setUnitPrice(price);
        vo.setExpectedHeadcount(headcount);
        vo.setOtherCost(other);

        BigDecimal expectedRevenue = price.multiply(BigDecimal.valueOf(headcount)).setScale(2, RoundingMode.HALF_UP);
        vo.setExpectedRevenue(expectedRevenue);

        // 员工成本试算
        List<ActivityPricingVo.StaffCostItem> items = new ArrayList<>();
        BigDecimal employeeCost = BigDecimal.ZERO;
        BigDecimal variablePerHead = BigDecimal.ZERO; // 单人变动员工成本（PERCENTAGE 类型）
        if (CollUtil.isNotEmpty(staffIds)) {
            Map<Long, StaffCommissionRule> ruleMap = listActiveByStaffIds(staffIds);
            Map<Long, String> nameMap = resolveStaffNames(staffIds);
            for (Long sid : staffIds) {
                StaffCommissionRule rule = ruleMap.get(sid);
                ActivityPricingVo.StaffCostItem item = new ActivityPricingVo.StaffCostItem();
                item.setStaffId(sid);
                item.setStaffName(nameMap.getOrDefault(sid, "员工" + sid));
                if (rule == null) {
                    item.setCommissionType(null);
                    item.setAmount(BigDecimal.ZERO);
                    item.setRuleDesc("未配置分成规则（按 0 计）");
                } else {
                    item.setCommissionType(rule.getCommissionType());
                    BigDecimal amount = calcRuleAmount(rule, expectedRevenue);
                    item.setAmount(amount);
                    item.setRuleDesc(describeRule(rule));
                    employeeCost = employeeCost.add(amount);
                    // PERCENTAGE 类型：单人变动成本 = unitPrice × rate / 100
                    if (StaffCommissionRule.TYPE_PERCENTAGE.equals(rule.getCommissionType())
                        && rule.getPercentageRate() != null) {
                        variablePerHead = variablePerHead.add(
                            price.multiply(rule.getPercentageRate())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                    }
                }
                items.add(item);
            }
        }
        employeeCost = employeeCost.setScale(2, RoundingMode.HALF_UP);
        vo.setEmployeeCost(employeeCost);
        vo.setStaffCostItems(items);

        BigDecimal totalCost = employeeCost.add(other).setScale(2, RoundingMode.HALF_UP);
        vo.setTotalCost(totalCost);

        BigDecimal estimatedProfit = expectedRevenue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        vo.setEstimatedProfit(estimatedProfit);

        // 单位边际贡献 = unitPrice - 单人变动员工成本
        BigDecimal unitContribution = price.subtract(variablePerHead).setScale(4, RoundingMode.HALF_UP);
        vo.setUnitContribution(unitContribution);

        // 保本人数 = ceil(totalCost ÷ unitContribution)
        if (unitContribution.compareTo(BigDecimal.ZERO) > 0 && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal raw = totalCost.divide(unitContribution, 4, RoundingMode.HALF_UP);
            vo.setBreakevenHeadcount(raw.setScale(0, RoundingMode.CEILING).intValue());
        } else {
            vo.setBreakevenHeadcount(null);
        }

        vo.setFootnote("口径：预计收入=单人售价×预计人数；员工成本按当前分成规则试算（FIXED_AMOUNT=固定金额，"
            + "PERCENTAGE=预计收入×比例）；保本人数=ceil(成本合计÷单位边际贡献)，单位边际贡献=单人售价-单人变动员工成本。"
            + "月底薪进 M38 月分摊固定成本，不进单场。");
        return vo;
    }

    // ===== T35.7 预估 vs 实际报表 =====

    @Override
    public List<EstimateVsActualVo> queryEstimateVsActual(Date beginTime, Date endTime, String status) {
        LambdaQueryWrapper<PromotionActivity> wrapper = Wrappers.<PromotionActivity>lambdaQuery()
            .ge(beginTime != null, PromotionActivity::getStartTime, beginTime)
            .le(endTime != null, PromotionActivity::getStartTime, endTime)
            .eq(StrUtil.isNotBlank(status), PromotionActivity::getStatus, status)
            .orderByDesc(PromotionActivity::getStartTime)
            .orderByDesc(PromotionActivity::getId);
        List<PromotionActivity> activities = activityMapper.selectList(wrapper);
        if (CollUtil.isEmpty(activities)) {
            return Collections.emptyList();
        }
        List<EstimateVsActualVo> result = new ArrayList<>(activities.size());
        for (PromotionActivity a : activities) {
            EstimateVsActualVo row = new EstimateVsActualVo();
            row.setActivityId(a.getId());
            row.setActivityTitle(a.getTitle());
            row.setStartTime(a.getStartTime());
            row.setStatus(a.getStatus());
            row.setExpectedHeadcount(a.getParticipantCount());
            // 单人售价：优先 customPrice，其次 autoCalculatedPrice / participantCount
            BigDecimal unitPrice = resolveUnitPrice(a);
            row.setUnitPrice(unitPrice);
            int headcount = a.getParticipantCount() == null ? 0 : a.getParticipantCount();
            BigDecimal expectedRevenue = unitPrice.multiply(BigDecimal.valueOf(headcount)).setScale(2, RoundingMode.HALF_UP);
            row.setExpectedRevenue(expectedRevenue);
            // 员工成本：快照合计（未结算时按试算估算）
            BigDecimal employeeCost = snapshotService.sumEmployeeCost(a.getId());
            row.setEmployeeCost(employeeCost);
            BigDecimal other = a.getOtherCost() == null ? BigDecimal.ZERO : a.getOtherCost();
            row.setOtherCost(other);
            BigDecimal estimatedProfit = expectedRevenue.subtract(employeeCost).subtract(other).setScale(2, RoundingMode.HALF_UP);
            row.setEstimatedProfit(estimatedProfit);
            // ★M36 活动结算单未交付前 actualProfit 为 null；交付后从 activity_settlement.profit 读取
            row.setActualProfit(null);
            row.setDiff(null);
            row.setDiffReason(null);
            row.setSettlementStatus("UNSETTLED");
            result.add(row);
        }
        return result;
    }

    // ===== 私有方法 =====

    private LambdaQueryWrapper<StaffCommissionRule> buildQueryWrapper(StaffCommissionRuleBo bo) {
        LambdaQueryWrapper<StaffCommissionRule> w = Wrappers.<StaffCommissionRule>lambdaQuery()
            .eq(bo.getStaffId() != null, StaffCommissionRule::getStaffId, bo.getStaffId())
            .eq(StrUtil.isNotBlank(bo.getCommissionType()), StaffCommissionRule::getCommissionType, bo.getCommissionType())
            .eq(bo.getIsActive() != null, StaffCommissionRule::getIsActive, bo.getIsActive())
            .orderByDesc(StaffCommissionRule::getCreateTime)
            .orderByDesc(StaffCommissionRule::getId);
        // 员工姓名模糊查询：先查 sys_user 拿 ID 集合再 in 过滤
        if (StrUtil.isNotBlank(bo.getStaffName())) {
            List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .like(SysUser::getNickName, bo.getStaffName())
                .select(SysUser::getUserId));
            if (CollUtil.isEmpty(users)) {
                // 无匹配员工 → 返回空集
                w.eq(StaffCommissionRule::getId, -1L);
            } else {
                w.in(StaffCommissionRule::getStaffId,
                    users.stream().map(SysUser::getUserId).collect(Collectors.toList()));
            }
        }
        return w;
    }

    /** 回填员工姓名 + 部门名 */
    private void fillExtraInfo(List<StaffCommissionRuleVo> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Set<Long> staffIds = records.stream()
            .map(StaffCommissionRuleVo::getStaffId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        if (staffIds.isEmpty()) {
            return;
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, staffIds)
            .select(SysUser::getUserId, SysUser::getNickName, SysUser::getDeptId));
        Map<Long, SysUser> userMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        Set<Long> deptIds = users.stream()
            .map(SysUser::getDeptId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .in(SysDept::getDeptId, deptIds)
                .select(SysDept::getDeptId, SysDept::getDeptName));
            for (SysDept d : depts) {
                deptNameMap.put(d.getDeptId(), d.getDeptName());
            }
        }
        for (StaffCommissionRuleVo vo : records) {
            SysUser u = userMap.get(vo.getStaffId());
            if (u != null) {
                vo.setStaffName(u.getNickName());
                if (u.getDeptId() != null) {
                    vo.setDeptName(deptNameMap.get(u.getDeptId()));
                }
            }
        }
    }

    private Map<Long, String> resolveStaffNames(Collection<Long> staffIds) {
        if (CollUtil.isEmpty(staffIds)) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, staffIds)
            .select(SysUser::getUserId, SysUser::getNickName));
        Map<Long, String> map = new HashMap<>();
        for (SysUser u : users) {
            map.put(u.getUserId(), u.getNickName());
        }
        return map;
    }

    /** 按类型校验规则参数 */
    private void validateByType(StaffCommissionRuleBo bo) {
        String type = bo.getCommissionType();
        if (StaffCommissionRule.TYPE_FIXED_AMOUNT.equals(type)) {
            if (bo.getFixedAmount() == null || bo.getFixedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("固定金额类型必须填写大于 0 的固定金额");
            }
        } else if (StaffCommissionRule.TYPE_PERCENTAGE.equals(type)) {
            if (bo.getPercentageRate() == null
                || bo.getPercentageRate().compareTo(BigDecimal.ZERO) < 0
                || bo.getPercentageRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ServiceException("按比例类型必须填写 0~100 的百分比");
            }
        } else if (StaffCommissionRule.TYPE_TIERED.equals(type)) {
            if (StrUtil.isBlank(bo.getTieredConfig())) {
                throw new ServiceException("阶梯类型必须填写阶梯规则 JSON（本期预留）");
            }
        } else {
            throw new ServiceException("不支持的分成类型：" + type);
        }
    }

    /** 按规则试算单员工金额 */
    private BigDecimal calcRuleAmount(StaffCommissionRule rule, BigDecimal expectedRevenue) {
        if (rule == null) {
            return BigDecimal.ZERO;
        }
        String type = rule.getCommissionType();
        if (StaffCommissionRule.TYPE_FIXED_AMOUNT.equals(type)) {
            return rule.getFixedAmount() == null ? BigDecimal.ZERO
                : rule.getFixedAmount().setScale(2, RoundingMode.HALF_UP);
        }
        if (StaffCommissionRule.TYPE_PERCENTAGE.equals(type)) {
            if (rule.getPercentageRate() == null || expectedRevenue == null) {
                return BigDecimal.ZERO;
            }
            return expectedRevenue.multiply(rule.getPercentageRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        // TIERED 预留：本期返回 0
        return BigDecimal.ZERO;
    }

    private String describeRule(StaffCommissionRule rule) {
        if (rule == null) {
            return "未配置";
        }
        String type = rule.getCommissionType();
        if (StaffCommissionRule.TYPE_FIXED_AMOUNT.equals(type)) {
            return "固定 " + (rule.getFixedAmount() == null ? "0" : rule.getFixedAmount().toPlainString()) + " 元/场";
        }
        if (StaffCommissionRule.TYPE_PERCENTAGE.equals(type)) {
            return "按 " + (rule.getPercentageRate() == null ? "0" : rule.getPercentageRate().toPlainString()) + "% 提成";
        }
        if (StaffCommissionRule.TYPE_TIERED.equals(type)) {
            return "阶梯（预留）";
        }
        return type;
    }

    /** 单人售价：优先 customPrice，其次 autoCalculatedPrice÷participantCount，再次 receivableAmount÷participantCount */
    private BigDecimal resolveUnitPrice(PromotionActivity a) {
        if (a.getCustomPrice() != null && a.getCustomPrice().compareTo(BigDecimal.ZERO) > 0) {
            return a.getCustomPrice();
        }
        Integer pc = a.getParticipantCount();
        if (pc != null && pc > 0) {
            if (a.getAutoCalculatedPrice() != null) {
                return a.getAutoCalculatedPrice().divide(BigDecimal.valueOf(pc), 2, RoundingMode.HALF_UP);
            }
            if (a.getReceivableAmount() != null) {
                return a.getReceivableAmount().divide(BigDecimal.valueOf(pc), 2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }
}
