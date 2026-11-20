package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.ActivityStaffSnapshot;
import com.ruoyi.system.domain.StaffCommissionRule;
import com.ruoyi.system.mapper.ActivityStaffSnapshotMapper;
import com.ruoyi.system.mapper.StaffCommissionRuleMapper;
import com.ruoyi.system.service.IActivityStaffSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动员工分成快照服务实现（M35 T35.3）
 * <p>快照生成时机：活动创建/编辑保存时按当前选中员工集合重建（先删后插，幂等）。
 * <p>历史账不可变：员工规则变更后历史活动快照不受影响；事实变更（删除缺席员工）
 * 通过重新调用 rebuildSnapshots 覆盖，报表按实际重算。
 * <p>snapshot_value JSON 结构：{type, fixedAmount, percentageRate, includeAddon, baseSalary}。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityStaffSnapshotServiceImpl implements IActivityStaffSnapshotService {

    private final ActivityStaffSnapshotMapper snapshotMapper;
    /** ★直连规则 Mapper 而非 IStaffCommissionRuleService：后者（报表试算）反向注入本服务，Service 互注入会形成循环依赖导致启动失败 */
    private final StaffCommissionRuleMapper ruleMapper;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuildSnapshots(Long activityId, Collection<Long> staffIds) {
        if (activityId == null) {
            return 0;
        }
        // 先删后插，幂等
        snapshotMapper.delete(Wrappers.<ActivityStaffSnapshot>lambdaQuery()
            .eq(ActivityStaffSnapshot::getActivityId, activityId));
        if (CollUtil.isEmpty(staffIds)) {
            return 0;
        }
        Map<Long, StaffCommissionRule> ruleMap = listActiveRulesByStaffIds(staffIds);
        int count = 0;
        for (Long sid : staffIds) {
            if (sid == null) {
                continue;
            }
            StaffCommissionRule rule = ruleMap.get(sid);
            ActivityStaffSnapshot snap = new ActivityStaffSnapshot();
            snap.setActivityId(activityId);
            snap.setStaffId(sid);
            if (rule == null) {
                // 无启用规则：快照类型 NONE，金额 0（保留行以便报表显示「未配置」）
                snap.setRuleType("NONE");
                snap.setSnapshotValue("{}");
                snap.setCalculatedAmount(BigDecimal.ZERO);
            } else {
                snap.setRuleType(rule.getCommissionType());
                snap.setSnapshotValue(toJson(rule));
                // calculated_amount 初始为 0，结算时（M36）按实际人数/收入回写
                snap.setCalculatedAmount(BigDecimal.ZERO);
            }
            snapshotMapper.insert(snap);
            count++;
        }
        return count;
    }

    @Override
    public List<ActivityStaffSnapshot> listByActivityId(Long activityId) {
        if (activityId == null) {
            return Collections.emptyList();
        }
        return snapshotMapper.selectList(Wrappers.<ActivityStaffSnapshot>lambdaQuery()
            .eq(ActivityStaffSnapshot::getActivityId, activityId)
            .orderByAsc(ActivityStaffSnapshot::getId));
    }

    @Override
    public ActivityStaffSnapshot getByActivityAndStaff(Long activityId, Long staffId) {
        if (activityId == null || staffId == null) {
            return null;
        }
        return snapshotMapper.selectOne(Wrappers.<ActivityStaffSnapshot>lambdaQuery()
            .eq(ActivityStaffSnapshot::getActivityId, activityId)
            .eq(ActivityStaffSnapshot::getStaffId, staffId)
            .last("LIMIT 1"));
    }

    @Override
    public BigDecimal sumEmployeeCost(Long activityId) {
        List<ActivityStaffSnapshot> list = listByActivityId(activityId);
        if (CollUtil.isEmpty(list)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (ActivityStaffSnapshot s : list) {
            if (s.getCalculatedAmount() != null) {
                sum = sum.add(s.getCalculatedAmount());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByActivityId(Long activityId) {
        if (activityId == null) {
            return 0;
        }
        return snapshotMapper.delete(Wrappers.<ActivityStaffSnapshot>lambdaQuery()
            .eq(ActivityStaffSnapshot::getActivityId, activityId));
    }

    /** 按员工ID集合查询启用中的分成规则（口径与 IStaffCommissionRuleService.listActiveByStaffIds 一致） */
    private Map<Long, StaffCommissionRule> listActiveRulesByStaffIds(Collection<Long> staffIds) {
        List<StaffCommissionRule> rules = ruleMapper.selectList(Wrappers.<StaffCommissionRule>lambdaQuery()
            .in(StaffCommissionRule::getStaffId, staffIds)
            .eq(StaffCommissionRule::getIsActive, 1));
        Map<Long, StaffCommissionRule> map = new HashMap<>();
        for (StaffCommissionRule r : rules) {
            map.put(r.getStaffId(), r);
        }
        return map;
    }

    /** 规则 → 快照 JSON（固化 type + 关键参数，规则变更后历史快照不变） */
    private String toJson(StaffCommissionRule rule) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", rule.getCommissionType());
        m.put("fixedAmount", rule.getFixedAmount());
        m.put("percentageRate", rule.getPercentageRate());
        m.put("includeAddon", rule.getIncludeAddonInCommission());
        m.put("baseSalary", rule.getBaseSalary());
        try {
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            log.warn("[M35] 快照 JSON 序列化失败 staffId={}", rule.getStaffId(), e);
            return "{}";
        }
    }
}
