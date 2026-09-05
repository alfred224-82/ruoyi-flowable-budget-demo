package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.system.domain.FeeCreditRule;
import com.ruoyi.system.domain.FeeEmployeeCredit;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.FeeCreditRuleMapper;
import com.ruoyi.system.mapper.FeeEmployeeCreditMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IEmployeeCreditService;
import com.ruoyi.system.service.IFeeOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 员工信用体系Service实现（T6.3 / FEAT_CREDIT_SCORE）
 * fee_credit_rule 在租户豁免清单（内置模板 tenant_id=0），服务层手工隔离；
 * fee_employee_credit 走常规租户过滤
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeCreditServiceImpl implements IEmployeeCreditService {

    private final FeeEmployeeCreditMapper creditMapper;
    private final FeeCreditRuleMapper ruleMapper;
    private final SysTenantMapper tenantMapper;
    private final IFeeOperationLogService feeOperationLogService;
    private final FeatureChecker featureChecker;

    /**
     * 信用功能开关编码
     */
    private static final String FEATURE_CODE = "FEAT_CREDIT_SCORE";

    /**
     * 默认初始信用分（对齐建表 DDL）
     */
    private static final int DEFAULT_SCORE = 100;

    /**
     * 补票期限：优秀 14 天 / 其余 7 天
     */
    private static final int SUPPLEMENT_DAYS_EXCELLENT = 14;
    private static final int SUPPLEMENT_DAYS_STANDARD = 7;

    @Override
    public FeeEmployeeCredit getOrInit(Long userId) {
        FeeEmployeeCredit credit = creditMapper.selectOne(Wrappers.<FeeEmployeeCredit>lambdaQuery()
            .eq(FeeEmployeeCredit::getUserId, userId)
            .last("LIMIT 1"));
        if (credit == null) {
            credit = new FeeEmployeeCredit();
            credit.setUserId(userId);
            credit.setCreditScore(DEFAULT_SCORE);
            credit.setCreditLevel(calcLevel(DEFAULT_SCORE));
            credit.setOnTimeCount(0);
            credit.setOverdueCount(0);
            credit.setConsecutiveOnTime(0);
            creditMapper.insert(credit);
        }
        return credit;
    }

    @Override
    public String calcLevel(int creditScore) {
        if (creditScore >= 80) {
            return "EXCELLENT";
        }
        if (creditScore >= 50) {
            return "NORMAL";
        }
        return "RESTRICTED";
    }

    @Override
    public boolean allowInvoiceDefer(Long userId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE) || userId == null) {
            return true;
        }
        return !"RESTRICTED".equals(getOrInit(userId).getCreditLevel());
    }

    @Override
    public int getSupplementDays(Long userId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE) || userId == null) {
            return SUPPLEMENT_DAYS_STANDARD;
        }
        return "EXCELLENT".equals(getOrInit(userId).getCreditLevel())
            ? SUPPLEMENT_DAYS_EXCELLENT : SUPPLEMENT_DAYS_STANDARD;
    }

    @Override
    public boolean skipBossApproval(Long userId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE) || userId == null) {
            return false;
        }
        return "EXCELLENT".equals(getOrInit(userId).getCreditLevel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOnTime(Long userId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE) || userId == null) {
            return;
        }
        FeeCreditRule onTime = effectiveRule("ON_TIME");
        if (onTime == null) {
            return;
        }
        FeeEmployeeCredit credit = getOrInit(userId);
        int score = credit.getCreditScore() + onTime.getScoreDelta();
        credit.setOnTimeCount(credit.getOnTimeCount() + 1);
        credit.setConsecutiveOnTime(credit.getConsecutiveOnTime() + 1);
        // 连续按时奖励：达到阈值追加加分并清零连续计数
        FeeCreditRule bonus = effectiveRule("CONSECUTIVE_BONUS");
        if (bonus != null && bonus.getThreshold() != null
            && credit.getConsecutiveOnTime() >= bonus.getThreshold()) {
            score += bonus.getScoreDelta();
            credit.setConsecutiveOnTime(0);
        }
        credit.setCreditScore(Math.max(score, 0));
        credit.setCreditLevel(calcLevel(credit.getCreditScore()));
        creditMapper.updateById(credit);
        log.info("信用事件[ON_TIME]: userId={}, score={}", userId, credit.getCreditScore());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOverdue(Long userId) {
        if (!featureChecker.isFeatureEnabled(FEATURE_CODE) || userId == null) {
            return;
        }
        FeeCreditRule overdue = effectiveRule("OVERDUE");
        if (overdue == null) {
            return;
        }
        FeeEmployeeCredit credit = getOrInit(userId);
        credit.setCreditScore(Math.max(credit.getCreditScore() + overdue.getScoreDelta(), 0));
        credit.setOverdueCount(credit.getOverdueCount() + 1);
        credit.setConsecutiveOnTime(0);
        credit.setCreditLevel(calcLevel(credit.getCreditScore()));
        creditMapper.updateById(credit);
        log.info("信用事件[OVERDUE]: userId={}, score={}", userId, credit.getCreditScore());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustScore(Long userId, int delta, String reason) {
        if (delta == 0) {
            throw new ServiceException("调整分值不能为0");
        }
        FeeEmployeeCredit credit = getOrInit(userId);
        int before = credit.getCreditScore();
        credit.setCreditScore(Math.max(before + delta, 0));
        credit.setCreditLevel(calcLevel(credit.getCreditScore()));
        int rows = creditMapper.updateById(credit);
        feeOperationLogService.log("CREDIT", "ADJUST", credit.getId(), String.valueOf(userId),
            null, String.valueOf(before), String.valueOf(credit.getCreditScore()), reason);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalcLevels() {
        // 租户→开关映射（版本默认功能集；基础版租户跳过，验收用例 9）
        Map<Long, Boolean> tenantEnabled = new HashMap<>();
        for (SysTenant tenant : tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
            .eq(SysTenant::getStatus, "NORMAL"))) {
            Set<String> features = new HashSet<>(EditionFeatureConfig.getFeaturesByEdition(tenant.getEdition()));
            tenantEnabled.put(tenant.getId(), features.contains(FEATURE_CODE));
        }
        // Job 无登录态，租户拦截器不过滤，需全量扫描后按租户开关筛选
        List<FeeEmployeeCredit> all = creditMapper.selectList(Wrappers.<FeeEmployeeCredit>lambdaQuery());
        Date today = new Date();
        int count = 0;
        for (FeeEmployeeCredit credit : all) {
            if (!Boolean.TRUE.equals(tenantEnabled.get(credit.getTenantId()))) {
                continue;
            }
            FeeEmployeeCredit update = new FeeEmployeeCredit();
            update.setId(credit.getId());
            update.setCreditLevel(calcLevel(credit.getCreditScore()));
            update.setLastCalcDate(today);
            creditMapper.updateById(update);
            count++;
        }
        return count;
    }

    @Override
    public Page<FeeEmployeeCredit> pageCredits(Long userId, String creditLevel, PageQuery pageQuery) {
        return creditMapper.selectPage(pageQuery.build(), Wrappers.<FeeEmployeeCredit>lambdaQuery()
            .eq(userId != null, FeeEmployeeCredit::getUserId, userId)
            .eq(StrUtil.isNotBlank(creditLevel), FeeEmployeeCredit::getCreditLevel, creditLevel)
            .orderByAsc(FeeEmployeeCredit::getCreditScore));
    }

    @Override
    public List<FeeCreditRule> listRules() {
        return ruleMapper.selectList(Wrappers.<FeeCreditRule>lambdaQuery()
            .in(FeeCreditRule::getTenantId, visibleTenantIds())
            .orderByAsc(FeeCreditRule::getEventType)
            .orderByDesc(FeeCreditRule::getTenantId));
    }

    @Override
    public boolean saveRule(FeeCreditRule rule) {
        Long tenantId = currentTenantId();
        rule.setTenantId(tenantId);
        Long exists = ruleMapper.selectCount(Wrappers.<FeeCreditRule>lambdaQuery()
            .eq(FeeCreditRule::getTenantId, tenantId)
            .eq(FeeCreditRule::getEventType, rule.getEventType()));
        if (exists != null && exists > 0) {
            throw new ServiceException("事件类型[" + rule.getEventType() + "]规则已存在，请直接修改");
        }
        return ruleMapper.insert(rule) > 0;
    }

    @Override
    public boolean updateRule(FeeCreditRule rule) {
        checkTenantOwnership(rule.getId());
        return ruleMapper.updateById(rule) > 0;
    }

    @Override
    public boolean deleteRule(Long id) {
        checkTenantOwnership(id);
        return ruleMapper.deleteById(id) > 0;
    }

    /**
     * 生效规则：租户级覆盖内置模板（同 eventType 租户行优先），停用返回 null
     */
    private FeeCreditRule effectiveRule(String eventType) {
        List<FeeCreditRule> rules = ruleMapper.selectList(Wrappers.<FeeCreditRule>lambdaQuery()
            .in(FeeCreditRule::getTenantId, visibleTenantIds())
            .eq(FeeCreditRule::getEventType, eventType)
            .orderByDesc(FeeCreditRule::getTenantId)
            .last("LIMIT 1"));
        if (rules.isEmpty()) {
            return null;
        }
        FeeCreditRule rule = rules.get(0);
        return rule.getIsEnabled() != null && rule.getIsEnabled() == 1 ? rule : null;
    }

    /**
     * 校验规则归属当前租户（内置模板行不允许租户修改/删除）
     */
    private void checkTenantOwnership(Long id) {
        FeeCreditRule db = ruleMapper.selectById(id);
        if (db == null) {
            throw new ServiceException("信用规则不存在");
        }
        if (db.getTenantId() != null && db.getTenantId() == 0L) {
            throw new ServiceException("内置模板规则不允许删除，可通过停用（isEnabled=0）关闭");
        }
        if (db.getTenantId() != null && !db.getTenantId().equals(currentTenantId())) {
            throw new ServiceException("无权操作其他租户的信用规则");
        }
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
        Long tenantId = LoginHelper.getTenantId();
        return tenantId != null ? tenantId : 0L;
    }
}
