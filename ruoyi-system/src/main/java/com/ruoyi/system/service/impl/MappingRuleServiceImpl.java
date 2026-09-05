package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeMappingAuditLog;
import com.ruoyi.system.domain.MappingRule;
import com.ruoyi.system.domain.bo.MappingRuleBo;
import com.ruoyi.system.domain.vo.BudgetPreviewVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeMappingAuditLogMapper;
import com.ruoyi.system.mapper.MappingRuleMapper;
import com.ruoyi.system.service.IMappingRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 映射规则Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingRuleServiceImpl implements IMappingRuleService {

    private final MappingRuleMapper mappingRuleMapper;
    private final BudgetSubjectMapper budgetSubjectMapper;
    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;
    private final FeatureChecker featureChecker;
    // M6 T6.4 规则引擎自定义：审计留痕 + 影响分析（历史命中统计）
    private final FeeMappingAuditLogMapper auditLogMapper;
    private final ExpenseBillMapper expenseBillMapper;

    /** 规则引擎租户级自定义开关（开启后新增规则初始为草稿，需发布才参与匹配） */
    private static final String RULE_CUSTOM_FEATURE = "FEAT_RULE_ENGINE_CUSTOM";

    /** 物资小类默认费用确认方式兜底映射 */
    private static final java.util.Map<String, String> ITEM_CATEGORY_DEFAULT_COST_METHOD = new java.util.HashMap<String, String>() {{
        put("EQUIPMENT", "DEPRECIATION");        // 设备采购 → 折旧
        put("SOFTWARE_LICENSE", "ACCRUAL");      // 软件授权 → 计提摊销
        put("OFFICE_SUPPLIES", "DIRECT");        // 办公用品 → 直接费用化
        put("TRAVEL_EXPENSES", "DIRECT");        // 差旅费用 → 直接费用化
        put("COMMUNICATION", "DIRECT");          // 通讯费用 → 直接费用化
        put("TRAINING", "DIRECT");               // 培训费用 → 直接费用化
        put("MARKETING", "DIRECT");              // 市场推广 → 直接费用化
        put("CONSULTING", "DIRECT");             // 咨询服务 → 直接费用化
        put("MAINTENANCE", "DIRECT");            // 维修保养 → 直接费用化
        put("OTHER", "DIRECT");                  // 其他 → 直接费用化
    }};

    @Override
    public MappingRuleVo queryById(Long id) {
        return mappingRuleMapper.selectVoById(id);
    }

    @Override
    public Page<MappingRuleVo> selectPageList(MappingRuleBo bo, PageQuery pageQuery) {
        Wrapper<MappingRule> wrapper = buildQueryWrapper(bo);
        return mappingRuleMapper.selectVoPage(pageQuery.build(), wrapper);
    }

    @Override
    public Boolean insertByBo(MappingRuleBo bo) {
        MappingRule mappingRule = BeanCopyUtils.copy(bo, MappingRule.class);
        // T6.4：开关开启时新增规则初始为草稿，需发布后才参与匹配；基础版直接生效（存量行为不变）
        boolean customEnabled = featureChecker.isFeatureEnabled(RULE_CUSTOM_FEATURE);
        mappingRule.setPublishStatus(customEnabled ? "DRAFT" : "PUBLISHED");
        if (!customEnabled) {
            mappingRule.setPublishTime(new Date());
            mappingRule.setPublishedBy(LoginHelper.getUserId());
        }
        boolean flag = mappingRuleMapper.insert(mappingRule) > 0;
        if (flag) {
            bo.setId(mappingRule.getId());
            writeAudit(mappingRule, "CREATE", JSONUtil.toJsonStr(mappingRule), null, null);
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(MappingRuleBo bo) {
        MappingRule before = bo.getId() != null ? mappingRuleMapper.selectById(bo.getId()) : null;
        MappingRule mappingRule = BeanCopyUtils.copy(bo, MappingRule.class);
        boolean flag = mappingRuleMapper.updateById(mappingRule) > 0;
        if (flag) {
            writeAudit(mappingRule, "UPDATE", before != null ? JSONUtil.toJsonStr(before) : null,
                JSONUtil.toJsonStr(mappingRule), null);
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        // 删除前留痕（ADR-008）
        for (Long id : ids) {
            MappingRule rule = mappingRuleMapper.selectById(id);
            if (rule != null) {
                writeAudit(rule, "DELETE", JSONUtil.toJsonStr(rule), null, null);
            }
        }
        boolean flag = mappingRuleMapper.deleteBatchIds(ids) > 0;
        if (flag) {
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public MappingRuleVo matchRule(String itemCategory, String purposeTag, Long deptId) {
        // 1. 尝试缓存
        String cacheKey = "fee:mapping_rule:" + itemCategory + ":" + purposeTag;
        MappingRuleVo cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            // 缓存命中后仍需版本降级处理
            cached.setCostMethod(resolveCostMethod(cached.getCostMethod(), itemCategory));
            return cached;
        }

        // 2. 精确匹配（物资小类 + 用途标签）
        MappingRule rule = mappingRuleMapper.matchExact(itemCategory, purposeTag);

        // 3. 未匹配
        if (rule == null) {
            throw new ServiceException("未找到匹配的预算科目规则，请联系财务管理员配置映射规则");
        }

        MappingRuleVo vo = BeanCopyUtils.copy(rule, MappingRuleVo.class);
        // 4. 解析费用确认方式（版本降级 + 物资小类兜底）
        vo.setCostMethod(resolveCostMethod(vo.getCostMethod(), itemCategory));
        RedisUtils.setCacheObject(cacheKey, vo, Duration.ofMinutes(30));
        return vo;
    }

    /**
     * 解析费用确认方式：版本降级 + 物资小类默认值兜底
     * - 基础版(BASIC)：仅支持 DIRECT，所有报销统一直接费用化
     * - 高级版(ADVANCED)：支持 DIRECT + DEPRECIATION，ACCRUAL/FEE_RATE 降级为 DIRECT
     * - 企业版(ENTERPRISE)：支持全部 DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE
     *
     * @param costMethod   映射规则配置的费用确认方式（可能为null）
     * @param itemCategory 物资小类编码（用于兜底推断）
     * @return 实际生效的费用确认方式
     */
    private String resolveCostMethod(String costMethod, String itemCategory) {
        // 1. 兜底：如果映射规则未配置 costMethod，按物资小类默认值推断
        if (StrUtil.isBlank(costMethod)) {
            costMethod = ITEM_CATEGORY_DEFAULT_COST_METHOD.getOrDefault(itemCategory, "DIRECT");
        }

        // 2. 版本降级处理
        String edition = featureChecker.getCurrentEdition();
        if ("BASIC".equals(edition)) {
            // 基础版：强制直接费用化，不展示折旧/计提概念
            return "DIRECT";
        } else if ("ADVANCED".equals(edition)) {
            // 高级版：支持折旧，但计提/费率降级为直接费用化
            if ("ACCRUAL".equals(costMethod) || "FEE_RATE".equals(costMethod)) {
                return "DIRECT";
            }
            return costMethod;
        }
        // 企业版：保持原值（完整计提引擎支持）
        return costMethod;
    }

    @Override
    public BudgetPreviewVo previewBudgetSubject(String itemCategory, String purposeTag, Long deptId) {
        return previewBudgetSubject(itemCategory, purposeTag, deptId, null);
    }

    @Override
    public BudgetPreviewVo previewBudgetSubject(String itemCategory, String purposeTag, Long deptId, String budgetPeriod) {
        BudgetPreviewVo preview = new BudgetPreviewVo();
        try {
            MappingRuleVo rule = matchRule(itemCategory, purposeTag, deptId);
            preview.setPassed(true);
            preview.setBudgetSubject(rule.getBudgetSubject());
            preview.setAccountingClass(rule.getAccountingClass());
            preview.setIsFixedAsset(rule.getIsFixedAsset());
            preview.setCostMethod(rule.getCostMethod());
            // 查询预算科目名称
            BudgetSubject subject = budgetSubjectMapper.selectOne(
                Wrappers.<BudgetSubject>lambdaQuery()
                    .eq(BudgetSubject::getSubjectCode, rule.getBudgetSubject())
                    .last("LIMIT 1"));
            if (subject != null) {
                preview.setBudgetSubjectName(subject.getSubjectName());
            }
            // 查询可用余额（按部门+期间隔离），优先使用传入的budgetPeriod
            try {
                int year;
                int month;
                if (budgetPeriod != null && budgetPeriod.length() >= 7) {
                    String[] parts = budgetPeriod.split("-");
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                } else {
                    java.time.LocalDate now = java.time.LocalDate.now();
                    year = now.getYear();
                    month = now.getMonthValue();
                }
                java.math.BigDecimal remaining = budgetPreparationDetailMapper.getRemainingBudgetByPeriod(
                    rule.getBudgetSubject(), deptId, year, month);
                preview.setRemainingBudget(remaining != null ? remaining : java.math.BigDecimal.ZERO);
            } catch (Exception ex) {
                log.debug("查询可用余额失败: {}", ex.getMessage());
                preview.setRemainingBudget(java.math.BigDecimal.ZERO);
            }
            preview.setMessage(null);
        } catch (ServiceException e) {
            log.debug("预览预算归属未匹配到规则: itemCategory={}, purposeTag={}, deptId={}",
                itemCategory, purposeTag, deptId);
            preview.setPassed(false);
            preview.setMessage(e.getMessage());
        }
        return preview;
    }

    // ========== T6.4 规则引擎租户级自定义（FEAT_RULE_ENGINE_CUSTOM） ==========

    @Override
    public Boolean publishRule(Long id) {
        MappingRule rule = mappingRuleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("映射规则不存在");
        }
        if ("PUBLISHED".equals(rule.getPublishStatus())) {
            throw new ServiceException("规则已发布，无需重复操作");
        }
        if (!"ENABLED".equals(rule.getStatus())) {
            throw new ServiceException("仅启用状态的规则可发布");
        }
        // 发布前自动附带影响分析留痕
        Map<String, Object> impact = impactAnalysis(id);
        rule.setPublishStatus("PUBLISHED");
        rule.setPublishTime(new Date());
        rule.setPublishedBy(LoginHelper.getUserId());
        boolean flag = mappingRuleMapper.updateById(rule) > 0;
        if (flag) {
            writeAudit(rule, "PUBLISH", null, JSONUtil.toJsonStr(rule), JSONUtil.toJsonStr(impact));
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public Map<String, Object> impactAnalysis(Long id) {
        MappingRule rule = mappingRuleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("映射规则不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleId", rule.getId());
        result.put("ruleCode", rule.getRuleCode());
        result.put("itemCategory", rule.getItemCategory());
        result.put("purposeTag", rule.getPurposeTag());
        result.put("targetBudgetSubject", rule.getBudgetSubject());
        result.put("publishStatus", rule.getPublishStatus());
        // 近 90 天命中同组合的报销单数量（发布后受影响单据预估）
        Long matched = expenseBillMapper.selectCount(Wrappers.<ExpenseBill>lambdaQuery()
            .eq(ExpenseBill::getItemCategory, rule.getItemCategory())
            .eq(ExpenseBill::getPurposeTag, rule.getPurposeTag())
            .ge(ExpenseBill::getCreateTime, DateUtil.offsetDay(new Date(), -90)));
        result.put("matchedBillCount90d", matched != null ? matched : 0L);
        // 当前生效规则对比（发布前匹配链路追溯）
        try {
            MappingRuleVo current = matchRule(rule.getItemCategory(), rule.getPurposeTag(), null);
            result.put("currentEffectiveRuleId", current.getId());
            result.put("currentEffectiveSubject", current.getBudgetSubject());
            result.put("subjectChanged", !StrUtil.equals(current.getBudgetSubject(), rule.getBudgetSubject()));
        } catch (ServiceException e) {
            result.put("currentEffectiveRuleId", null);
            result.put("currentEffectiveSubject", null);
            result.put("subjectChanged", true);
        }
        return result;
    }

    @Override
    public Page<FeeMappingAuditLog> auditLogs(Long ruleId, PageQuery pageQuery) {
        return auditLogMapper.selectPage(pageQuery.build(), Wrappers.<FeeMappingAuditLog>lambdaQuery()
            .eq(ruleId != null, FeeMappingAuditLog::getRuleId, ruleId)
            .orderByDesc(FeeMappingAuditLog::getCreateTime));
    }

    /**
     * 写规则变更审计日志（失败不阻断主流程）
     */
    private void writeAudit(MappingRule rule, String action, String draft, String publish, String impact) {
        try {
            FeeMappingAuditLog audit = new FeeMappingAuditLog();
            audit.setRuleId(rule.getId());
            audit.setAction(action);
            audit.setDraftContent(draft);
            audit.setPublishContent(publish);
            audit.setImpactAnalysis(impact);
            audit.setOperUserId(LoginHelper.getUserId());
            audit.setOperUserName(LoginHelper.getNickName());
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.warn("映射规则审计日志写入失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private Wrapper<MappingRule> buildQueryWrapper(MappingRuleBo bo) {
        return Wrappers.<MappingRule>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getRuleCode()), MappingRule::getRuleCode, bo.getRuleCode())
            .like(StrUtil.isNotBlank(bo.getItemCategory()), MappingRule::getItemCategory, bo.getItemCategory())
            .eq(StrUtil.isNotBlank(bo.getPurposeTag()), MappingRule::getPurposeTag, bo.getPurposeTag())
            .eq(StrUtil.isNotBlank(bo.getStatus()), MappingRule::getStatus, bo.getStatus())
            .eq(StrUtil.isNotBlank(bo.getPublishStatus()), MappingRule::getPublishStatus, bo.getPublishStatus())
            .orderByDesc(MappingRule::getPriority);
    }
}
