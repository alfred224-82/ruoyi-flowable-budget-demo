package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.MappingRule;
import com.ruoyi.system.domain.bo.MappingRuleBo;
import com.ruoyi.system.domain.vo.BudgetPreviewVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.MappingRuleMapper;
import com.ruoyi.system.service.ICostCenterService;
import com.ruoyi.system.service.IMappingRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;

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
    private final ICostCenterService costCenterService;
    private final BudgetSubjectMapper budgetSubjectMapper;
    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;

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
        boolean flag = mappingRuleMapper.insert(mappingRule) > 0;
        if (flag) {
            bo.setId(mappingRule.getId());
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(MappingRuleBo bo) {
        MappingRule mappingRule = BeanCopyUtils.copy(bo, MappingRule.class);
        boolean flag = mappingRuleMapper.updateById(mappingRule) > 0;
        if (flag) {
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        boolean flag = mappingRuleMapper.deleteBatchIds(ids) > 0;
        if (flag) {
            // 清除相关缓存
            RedisUtils.deleteKeys("fee:mapping_rule:*");
        }
        return flag;
    }

    @Override
    public MappingRuleVo matchRule(String itemCategory, String purposeTag, Long deptId) {
        // 1. 部门ID → 责任中心
        String centerCode = costCenterService.getCenterCodeByDeptId(deptId);
        if (centerCode == null) {
            centerCode = "PENDING"; // 未映射部门进入待分摊
        }

        // 2. 尝试缓存
        String cacheKey = "fee:mapping_rule:" + itemCategory + ":" + purposeTag + ":" + centerCode;
        MappingRuleVo cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3. 精确匹配
        MappingRule rule = mappingRuleMapper.matchExact(itemCategory, purposeTag, centerCode);

        // 4. 降级匹配
        if (rule == null) {
            rule = mappingRuleMapper.matchFallback(itemCategory, purposeTag);
        }

        // 5. 未匹配
        if (rule == null) {
            throw new ServiceException("未找到匹配的预算科目规则，请联系财务管理员配置映射规则");
        }

        MappingRuleVo vo = BeanCopyUtils.copy(rule, MappingRuleVo.class);
        RedisUtils.setCacheObject(cacheKey, vo, Duration.ofMinutes(30));
        return vo;
    }

    @Override
    public BudgetPreviewVo previewBudgetSubject(String itemCategory, String purposeTag, Long deptId) {
        BudgetPreviewVo preview = new BudgetPreviewVo();
        try {
            MappingRuleVo rule = matchRule(itemCategory, purposeTag, deptId);
            preview.setPassed(true);
            preview.setBudgetSubject(rule.getBudgetSubject());
            preview.setAccountingClass(rule.getAccountingClass());
            preview.setCostCenterCode(rule.getCostCenterGroup());
            // 查询预算科目名称
            BudgetSubject subject = budgetSubjectMapper.selectOne(
                Wrappers.<BudgetSubject>lambdaQuery()
                    .eq(BudgetSubject::getSubjectCode, rule.getBudgetSubject())
                    .last("LIMIT 1"));
            if (subject != null) {
                preview.setBudgetSubjectName(subject.getSubjectName());
            }
            // 查询可用余额（当月总预算 - 已使用），按部门+期间隔离
            try {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.math.BigDecimal remaining = budgetPreparationDetailMapper.getRemainingBudgetByPeriod(
                    rule.getBudgetSubject(), deptId, now.getYear(), now.getMonthValue());
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

    /**
     * 构建查询条件
     */
    private Wrapper<MappingRule> buildQueryWrapper(MappingRuleBo bo) {
        return Wrappers.<MappingRule>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getRuleCode()), MappingRule::getRuleCode, bo.getRuleCode())
            .like(StrUtil.isNotBlank(bo.getItemCategory()), MappingRule::getItemCategory, bo.getItemCategory())
            .eq(StrUtil.isNotBlank(bo.getPurposeTag()), MappingRule::getPurposeTag, bo.getPurposeTag())
            .eq(StrUtil.isNotBlank(bo.getCostCenterGroup()), MappingRule::getCostCenterGroup, bo.getCostCenterGroup())
            .eq(StrUtil.isNotBlank(bo.getStatus()), MappingRule::getStatus, bo.getStatus())
            .orderByDesc(MappingRule::getPriority);
    }
}
