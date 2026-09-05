package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeClockRecord;
import com.ruoyi.system.domain.FeeClockTriggerRule;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.mapper.FeeClockRecordMapper;
import com.ruoyi.system.mapper.FeeClockTriggerRuleMapper;
import com.ruoyi.system.service.IFeeClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡定位Service实现（T6.2 / FEAT_TIME_CLOCK）
 * fee_clock_trigger_rule / fee_clock_record 均在租户豁免清单，
 * 服务层手工隔离：tenant_id IN (0=内置模板, 当前租户)，租户级覆盖内置模板
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeClockServiceImpl implements IFeeClockService {

    private final FeeClockRecordMapper clockRecordMapper;
    private final FeeClockTriggerRuleMapper triggerRuleMapper;

    /**
     * 定位存疑精度阈值（米），超过仅标记不拦截
     */
    private static final BigDecimal SUSPICIOUS_ACCURACY = new BigDecimal("50");

    /**
     * 打卡记录证据等级
     */
    private static final String CLOCK_EVIDENCE_LEVEL = "medium";

    @Override
    public List<String> evaluateTriggers(ExpenseBill bill, Long userId) {
        List<String> hits = new ArrayList<>();
        for (FeeClockTriggerRule rule : effectiveRules()) {
            if (rule.getIsEnabled() == null || rule.getIsEnabled() != 1) {
                continue;
            }
            boolean hit = false;
            switch (StrUtil.blankToDefault(rule.getRuleCode(), "")) {
                case "TRAVEL_CATEGORY":
                    hit = matchCategoryScope(rule.getCategoryScope(), bill.getItemCategory());
                    break;
                case "AMOUNT_THRESHOLD":
                    hit = rule.getThreshold() != null && bill.getAmount() != null
                        && bill.getAmount().compareTo(rule.getThreshold()) >= 0;
                    break;
                case "HISTORY_ABNORMAL":
                    hit = rule.getThreshold() != null && userId != null
                        && countSuspicious(userId) >= rule.getThreshold().intValue();
                    break;
                default:
                    break;
            }
            if (hit) {
                hits.add(rule.getRuleCode());
            }
        }
        return hits;
    }

    @Override
    public boolean isClockRequired(ExpenseBill bill, Long userId) {
        return !evaluateTriggers(bill, userId).isEmpty();
    }

    @Override
    public FeeClockRecord recordClock(Long billId, Long userId, String triggerRules, ExpenseBillBo.ClockInfo clockInfo) {
        FeeClockRecord record = new FeeClockRecord();
        record.setBillId(billId);
        record.setUserId(userId);
        record.setTriggerRule(triggerRules);
        record.setLongitude(clockInfo.getLongitude());
        record.setLatitude(clockInfo.getLatitude());
        record.setAddress(clockInfo.getAddress());
        record.setAccuracy(clockInfo.getAccuracy());
        record.setPhotoUrls(clockInfo.getPhotoUrls());
        record.setClockTime(new Date());
        // 定位存疑（精度>50米）仅标记不拦截
        record.setIsSuspicious(clockInfo.getAccuracy() != null
            && clockInfo.getAccuracy().compareTo(SUSPICIOUS_ACCURACY) > 0 ? 1 : 0);
        record.setEvidenceLevel(CLOCK_EVIDENCE_LEVEL);
        clockRecordMapper.insert(record);
        log.info("打卡记录落库: billId={}, userId={}, trigger={}, suspicious={}",
            billId, userId, triggerRules, record.getIsSuspicious());
        return record;
    }

    @Override
    public int countSuspicious(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = clockRecordMapper.selectCount(Wrappers.<FeeClockRecord>lambdaQuery()
            .eq(FeeClockRecord::getUserId, userId)
            .eq(FeeClockRecord::getIsSuspicious, 1));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Page<FeeClockRecord> pageRecords(Long billId, Long userId, PageQuery pageQuery) {
        return clockRecordMapper.selectPage(pageQuery.build(), Wrappers.<FeeClockRecord>lambdaQuery()
            .eq(billId != null, FeeClockRecord::getBillId, billId)
            .eq(userId != null, FeeClockRecord::getUserId, userId)
            .orderByDesc(FeeClockRecord::getClockTime));
    }

    @Override
    public FeeClockRecord getRecord(Long id) {
        return clockRecordMapper.selectById(id);
    }

    @Override
    public List<FeeClockRecord> listByBill(Long billId) {
        return clockRecordMapper.selectList(Wrappers.<FeeClockRecord>lambdaQuery()
            .eq(FeeClockRecord::getBillId, billId)
            .orderByAsc(FeeClockRecord::getClockTime));
    }

    @Override
    public List<FeeClockTriggerRule> listTriggerRuleConfigs() {
        return triggerRuleMapper.selectList(Wrappers.<FeeClockTriggerRule>lambdaQuery()
            .in(FeeClockTriggerRule::getTenantId, visibleTenantIds())
            .orderByAsc(FeeClockTriggerRule::getRuleCode)
            .orderByDesc(FeeClockTriggerRule::getTenantId));
    }

    @Override
    public boolean saveTriggerRule(FeeClockTriggerRule rule) {
        Long tenantId = currentTenantId();
        rule.setTenantId(tenantId);
        // 同规则编码租户级仅允许一条
        Long exists = triggerRuleMapper.selectCount(Wrappers.<FeeClockTriggerRule>lambdaQuery()
            .eq(FeeClockTriggerRule::getTenantId, tenantId)
            .eq(FeeClockTriggerRule::getRuleCode, rule.getRuleCode()));
        if (exists != null && exists > 0) {
            throw new ServiceException("规则编码[" + rule.getRuleCode() + "]已存在，请直接修改");
        }
        return triggerRuleMapper.insert(rule) > 0;
    }

    @Override
    public boolean updateTriggerRule(FeeClockTriggerRule rule) {
        checkTenantOwnership(rule.getId());
        return triggerRuleMapper.updateById(rule) > 0;
    }

    @Override
    public boolean deleteTriggerRule(Long id) {
        checkTenantOwnership(id);
        return triggerRuleMapper.deleteById(id) > 0;
    }

    /**
     * 校验规则归属当前租户（内置模板行不允许租户修改/删除）
     */
    private void checkTenantOwnership(Long id) {
        FeeClockTriggerRule db = triggerRuleMapper.selectById(id);
        if (db == null) {
            throw new ServiceException("打卡触发规则不存在");
        }
        if (db.getTenantId() != null && db.getTenantId() != 0L && !db.getTenantId().equals(currentTenantId())) {
            throw new ServiceException("无权操作其他租户的打卡规则");
        }
        if (db.getTenantId() != null && db.getTenantId() == 0L) {
            throw new ServiceException("内置模板规则不允许删除，可通过停用（isEnabled=0）关闭");
        }
    }

    /**
     * 生效规则：租户级覆盖内置模板（同 ruleCode 租户行优先）
     */
    private List<FeeClockTriggerRule> effectiveRules() {
        List<FeeClockTriggerRule> all = triggerRuleMapper.selectList(Wrappers.<FeeClockTriggerRule>lambdaQuery()
            .in(FeeClockTriggerRule::getTenantId, visibleTenantIds()));
        Map<String, FeeClockTriggerRule> map = new HashMap<>();
        for (FeeClockTriggerRule rule : all) {
            FeeClockTriggerRule prev = map.get(rule.getRuleCode());
            // tenant_id 大的（租户行）覆盖内置模板
            if (prev == null || rule.getTenantId() > prev.getTenantId()) {
                map.put(rule.getRuleCode(), rule);
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 大类范围前缀匹配：scope='TRAVEL,ENTERTAIN' 可匹配 TRAVEL_EXPENSES 等小类编码
     */
    private boolean matchCategoryScope(String categoryScope, String itemCategory) {
        if (StrUtil.isBlank(categoryScope) || StrUtil.isBlank(itemCategory)) {
            return false;
        }
        return Arrays.stream(categoryScope.split(","))
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .anyMatch(scope -> itemCategory.toUpperCase().startsWith(scope.toUpperCase()));
    }

    /**
     * 可见租户范围：内置模板(0) + 当前租户
     */
    private List<Long> visibleTenantIds() {
        Long current = currentTenantId();
        return current == null || current == 0L
            ? Arrays.asList(0L)
            : Arrays.asList(0L, current);
    }

    private Long currentTenantId() {
        try {
            return Long.valueOf(LoginHelper.getTenantId());
        } catch (Exception e) {
            return 0L;
        }
    }
}
