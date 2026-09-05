package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeProjectLinkRule;
import com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo;
import com.ruoyi.system.mapper.FeeProjectLinkRuleMapper;
import com.ruoyi.system.service.IFeeProjectLinkRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 项目关联规则Service实现（T5.7 / ADR-023）
 * 匹配优先级：角色级硬规则（REQUIRED）> 大类级软提示；无命中返回 null，前端兜底公司级
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeProjectLinkRuleServiceImpl implements IFeeProjectLinkRuleService {

    /** T6.7 关联规则自定义开关：解锁大类强制关联/项目严格模式 */
    private static final String LINK_RULE_CUSTOM_FEATURE = "FEAT_LINK_RULE_CUSTOM";

    private final FeeProjectLinkRuleMapper ruleMapper;
    private final FeatureChecker featureChecker;

    @Override
    public List<FeeProjectLinkRuleVo> queryList() {
        return ruleMapper.selectVoList(Wrappers.<FeeProjectLinkRule>lambdaQuery()
            .in(FeeProjectLinkRule::getTenantId, visibleTenantIds())
            .orderByAsc(FeeProjectLinkRule::getScope)
            .orderByAsc(FeeProjectLinkRule::getId));
    }

    @Override
    public Long saveRule(FeeProjectLinkRuleVo vo) {
        if (StrUtil.isBlank(vo.getScope()) || StrUtil.isBlank(vo.getTarget()) || StrUtil.isBlank(vo.getRule())) {
            throw new ServiceException("规则作用域、目标与规则内容不能为空");
        }
        // T6.7 开关关闭时仅允许基础版能力：大类软提示（非强制）；项目严格模式与大类强制关联需高级版
        boolean customEnabled = featureChecker.isFeatureEnabled(LINK_RULE_CUSTOM_FEATURE);
        if (!customEnabled && "PROJECT".equals(vo.getScope())) {
            throw new ServiceException("当前版本不支持项目严格模式，请升级 License");
        }
        if (!customEnabled && "CATEGORY".equals(vo.getScope()) && "REQUIRED".equals(vo.getRule())) {
            throw new ServiceException("当前版本不支持大类强制关联，请升级 License");
        }
        // scope+target 唯一（本租户范围内；内置行不改写，租户新增同目标行实现覆盖）
        Long currentTenantId = currentTenantId();
        FeeProjectLinkRule exist = ruleMapper.selectOne(Wrappers.<FeeProjectLinkRule>lambdaQuery()
            .eq(FeeProjectLinkRule::getTenantId, currentTenantId)
            .eq(FeeProjectLinkRule::getScope, vo.getScope())
            .eq(FeeProjectLinkRule::getTarget, vo.getTarget())
            .ne(vo.getId() != null, FeeProjectLinkRule::getId, vo.getId())
            .last("LIMIT 1"));
        FeeProjectLinkRule rule = new FeeProjectLinkRule();
        rule.setScope(vo.getScope());
        rule.setTarget(vo.getTarget());
        rule.setRule(vo.getRule());
        rule.setIsEnabled(vo.getIsEnabled() == null ? 1 : vo.getIsEnabled());
        rule.setTenantId(currentTenantId);
        if (exist != null) {
            rule.setId(exist.getId());
            ruleMapper.updateById(rule);
            return exist.getId();
        }
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public int deleteRule(Long id) {
        FeeProjectLinkRule rule = ruleMapper.selectById(id);
        if (rule == null || !visibleTenantIds().contains(rule.getTenantId())) {
            throw new ServiceException("规则不存在");
        }
        if (rule.getTenantId() != null && rule.getTenantId() == 0L) {
            throw new ServiceException("内置默认规则不可删除，可停用代替");
        }
        return ruleMapper.deleteById(id);
    }

    @Override
    public FeeProjectLinkRuleVo matchRule(Set<String> roleKeys, Long categoryId) {
        // 1. 角色级硬规则（项目经理必填）
        if (CollUtil.isNotEmpty(roleKeys)) {
            List<FeeProjectLinkRule> roleRules = ruleMapper.selectList(
                Wrappers.<FeeProjectLinkRule>lambdaQuery()
                    .in(FeeProjectLinkRule::getTenantId, visibleTenantIds())
                    .eq(FeeProjectLinkRule::getScope, "ROLE")
                    .in(FeeProjectLinkRule::getTarget, roleKeys)
                    .eq(FeeProjectLinkRule::getIsEnabled, 1)
                    .orderByDesc(FeeProjectLinkRule::getTenantId));
            if (CollUtil.isNotEmpty(roleRules)) {
                return toVo(roleRules.get(0));
            }
        }
        // 2. 大类级软提示
        if (categoryId != null) {
            FeeProjectLinkRule categoryRule = ruleMapper.selectOne(
                Wrappers.<FeeProjectLinkRule>lambdaQuery()
                    .in(FeeProjectLinkRule::getTenantId, visibleTenantIds())
                    .eq(FeeProjectLinkRule::getScope, "CATEGORY")
                    .eq(FeeProjectLinkRule::getTarget, String.valueOf(categoryId))
                    .eq(FeeProjectLinkRule::getIsEnabled, 1)
                    .orderByDesc(FeeProjectLinkRule::getTenantId)
                    .last("LIMIT 1"));
            if (categoryRule != null) {
                return toVo(categoryRule);
            }
        }
        // 3. 无匹配 → 前端兜底公司级（project_id=0），财务可后续补关联
        return null;
    }

    @Override
    public FeeProjectLinkRuleVo matchCategoryRule(String itemCategory) {
        if (StrUtil.isBlank(itemCategory)) {
            return null;
        }
        List<FeeProjectLinkRule> rules = ruleMapper.selectList(
            Wrappers.<FeeProjectLinkRule>lambdaQuery()
                .in(FeeProjectLinkRule::getTenantId, visibleTenantIds())
                .eq(FeeProjectLinkRule::getScope, "CATEGORY")
                .eq(FeeProjectLinkRule::getIsEnabled, 1)
                .orderByDesc(FeeProjectLinkRule::getTenantId)
                .orderByAsc(FeeProjectLinkRule::getId));
        for (FeeProjectLinkRule rule : rules) {
            // 大类目标存大类编码，小类前缀匹配；租户级已按 tenant_id 降序优先
            if (StrUtil.isNotBlank(rule.getTarget()) && itemCategory.startsWith(rule.getTarget())) {
                return toVo(rule);
            }
        }
        return null;
    }

    @Override
    public FeeProjectLinkRuleVo queryProjectRule(Long projectId) {
        if (projectId == null || projectId <= 0) {
            return null;
        }
        FeeProjectLinkRule rule = ruleMapper.selectOne(
            Wrappers.<FeeProjectLinkRule>lambdaQuery()
                .in(FeeProjectLinkRule::getTenantId, visibleTenantIds())
                .eq(FeeProjectLinkRule::getScope, "PROJECT")
                .eq(FeeProjectLinkRule::getTarget, String.valueOf(projectId))
                .eq(FeeProjectLinkRule::getIsEnabled, 1)
                .orderByDesc(FeeProjectLinkRule::getTenantId)
                .last("LIMIT 1"));
        return rule == null ? null : toVo(rule);
    }

    /**
     * 可见租户：内置模板(0) + 当前租户（表在租户拦截豁免清单，服务层手工隔离）
     */
    private List<Long> visibleTenantIds() {
        Long current = LoginHelper.getTenantId();
        return current == null ? Arrays.asList(0L) : Arrays.asList(0L, current);
    }

    private Long currentTenantId() {
        Long current = LoginHelper.getTenantId();
        return current == null ? 0L : current;
    }

    private FeeProjectLinkRuleVo toVo(FeeProjectLinkRule rule) {
        FeeProjectLinkRuleVo vo = new FeeProjectLinkRuleVo();
        vo.setId(rule.getId());
        vo.setScope(rule.getScope());
        vo.setTarget(rule.getTarget());
        vo.setRule(rule.getRule());
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setCreateTime(rule.getCreateTime());
        return vo;
    }
}
