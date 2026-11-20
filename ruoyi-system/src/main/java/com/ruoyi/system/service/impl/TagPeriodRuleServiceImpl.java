package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FixedCostTag;
import com.ruoyi.system.domain.PeriodRule;
import com.ruoyi.system.domain.bo.FixedCostTagBo;
import com.ruoyi.system.domain.bo.PeriodRuleBo;
import com.ruoyi.system.domain.vo.FixedCostTagVo;
import com.ruoyi.system.domain.vo.PeriodRuleVo;
import com.ruoyi.system.mapper.FixedCostTagMapper;
import com.ruoyi.system.mapper.PeriodRuleMapper;
import com.ruoyi.system.service.ITagPeriodRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 标签与周期规则服务实现（T18.9，小微版 M18）
 * <p>★fail-fast：入口判空 + DB 返回判空 + UNIQUE 防重。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagPeriodRuleServiceImpl implements ITagPeriodRuleService {

    private final FixedCostTagMapper tagMapper;
    private final PeriodRuleMapper ruleMapper;

    /** 合法时间单位白名单 */
    private static final Set<String> VALID_UNITS = new HashSet<>(
        Arrays.asList("DAY", "WEEK", "MONTH", "QUARTER", "YEAR"));

    /** 单位→天数换算系数 */
    private static final Map<String, Integer> UNIT_TO_DAYS = new HashMap<>();
    static {
        UNIT_TO_DAYS.put("DAY", 1);
        UNIT_TO_DAYS.put("WEEK", 7);
        UNIT_TO_DAYS.put("MONTH", 30);
        UNIT_TO_DAYS.put("QUARTER", 90);
        UNIT_TO_DAYS.put("YEAR", 365);
    }

    // ===== 标签管理 =====

    @Override
    public TableDataInfo<FixedCostTagVo> queryTagPage(FixedCostTagBo bo, PageQuery pageQuery) {
        if (bo == null) { bo = new FixedCostTagBo(); }
        if (pageQuery == null) { throw new ServiceException("分页参数不能为空"); }
        LambdaQueryWrapper<FixedCostTag> wrapper = Wrappers.<FixedCostTag>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getTagName()), FixedCostTag::getTagName, bo.getTagName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), FixedCostTag::getStatus, bo.getStatus())
            .orderByAsc(FixedCostTag::getSortOrder)
            .orderByDesc(FixedCostTag::getCreateTime);
        Page<FixedCostTagVo> page = tagMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<FixedCostTagVo> queryTagAll(FixedCostTagBo bo) {
        if (bo == null) { bo = new FixedCostTagBo(); }
        LambdaQueryWrapper<FixedCostTag> wrapper = Wrappers.<FixedCostTag>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getStatus()), FixedCostTag::getStatus, bo.getStatus())
            .orderByAsc(FixedCostTag::getSortOrder);
        List<FixedCostTagVo> list = tagMapper.selectVoList(wrapper);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public FixedCostTagVo queryTagById(Long id) {
        if (id == null) { throw new ServiceException("标签ID不能为空"); }
        FixedCostTagVo vo = tagMapper.selectVoById(id);
        if (vo == null) { throw new ServiceException("标签不存在"); }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertTag(FixedCostTagBo bo) {
        if (bo == null) { throw new ServiceException("标签参数不能为空"); }
        if (StrUtil.isBlank(bo.getTagName())) { throw new ServiceException("标签名称不能为空"); }
        if (bo.getTagName().length() > 50) { throw new ServiceException("标签名称不能超过50字"); }
        // ★UNIQUE 防重
        Long existCount = tagMapper.selectCount(Wrappers.<FixedCostTag>lambdaQuery()
            .eq(FixedCostTag::getTagName, bo.getTagName()));
        if (existCount > 0) { throw new ServiceException("该标签名称已存在"); }
        FixedCostTag entity = new FixedCostTag();
        entity.setTagName(bo.getTagName());
        entity.setTagCode(bo.getTagCode());
        entity.setColor(bo.getColor());
        entity.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : 0);
        entity.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "ENABLED");
        entity.setRemark(bo.getRemark());
        tagMapper.insert(entity);
        log.info("[insertTag] tagName={}", bo.getTagName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTag(FixedCostTagBo bo) {
        if (bo == null || bo.getId() == null) { throw new ServiceException("标签参数不能为空"); }
        FixedCostTag exist = tagMapper.selectById(bo.getId());
        if (exist == null) { throw new ServiceException("标签不存在"); }
        FixedCostTag entity = new FixedCostTag();
        entity.setId(bo.getId());
        if (StrUtil.isNotBlank(bo.getTagName())) { entity.setTagName(bo.getTagName()); }
        if (bo.getTagCode() != null) { entity.setTagCode(bo.getTagCode()); }
        if (bo.getColor() != null) { entity.setColor(bo.getColor()); }
        if (bo.getSortOrder() != null) { entity.setSortOrder(bo.getSortOrder()); }
        if (StrUtil.isNotBlank(bo.getStatus())) { entity.setStatus(bo.getStatus()); }
        if (bo.getRemark() != null) { entity.setRemark(bo.getRemark()); }
        log.info("[updateTag] id={}", bo.getId());
        return tagMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTag(Long id) {
        if (id == null) { throw new ServiceException("标签ID不能为空"); }
        FixedCostTag exist = tagMapper.selectById(id);
        if (exist == null) { throw new ServiceException("标签不存在"); }
        log.info("[deleteTag] id={}, tagName={}", id, exist.getTagName());
        return tagMapper.deleteById(id);
    }

    // ===== 周期规则管理 =====

    @Override
    public TableDataInfo<PeriodRuleVo> queryRulePage(PeriodRuleBo bo, PageQuery pageQuery) {
        if (bo == null) { bo = new PeriodRuleBo(); }
        if (pageQuery == null) { throw new ServiceException("分页参数不能为空"); }
        LambdaQueryWrapper<PeriodRule> wrapper = Wrappers.<PeriodRule>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getRuleName()), PeriodRule::getRuleName, bo.getRuleName())
            .eq(StrUtil.isNotBlank(bo.getPeriodUnit()), PeriodRule::getPeriodUnit, bo.getPeriodUnit())
            .eq(bo.getTagId() != null, PeriodRule::getTagId, bo.getTagId())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PeriodRule::getStatus, bo.getStatus())
            .orderByDesc(PeriodRule::getCreateTime);
        Page<PeriodRuleVo> page = ruleMapper.selectVoPage(pageQuery.build(), wrapper);
        // 回填标签名称
        for (PeriodRuleVo vo : page.getRecords()) {
            if (vo.getTagId() != null) {
                FixedCostTag tag = tagMapper.selectById(vo.getTagId());
                if (tag != null) { vo.setTagName(tag.getTagName()); }
            }
        }
        return TableDataInfo.build(page);
    }

    @Override
    public List<PeriodRuleVo> queryRuleAll(PeriodRuleBo bo) {
        if (bo == null) { bo = new PeriodRuleBo(); }
        LambdaQueryWrapper<PeriodRule> wrapper = Wrappers.<PeriodRule>lambdaQuery()
            .eq(bo.getTagId() != null, PeriodRule::getTagId, bo.getTagId())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PeriodRule::getStatus, bo.getStatus())
            .orderByDesc(PeriodRule::getCreateTime);
        List<PeriodRuleVo> list = ruleMapper.selectVoList(wrapper);
        // 回填标签名称
        if (list != null) {
            for (PeriodRuleVo vo : list) {
                if (vo.getTagId() != null) {
                    FixedCostTag tag = tagMapper.selectById(vo.getTagId());
                    if (tag != null) { vo.setTagName(tag.getTagName()); }
                }
            }
        }
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public PeriodRuleVo queryRuleById(Long id) {
        if (id == null) { throw new ServiceException("规则ID不能为空"); }
        PeriodRuleVo vo = ruleMapper.selectVoById(id);
        if (vo == null) { throw new ServiceException("周期规则不存在"); }
        if (vo.getTagId() != null) {
            FixedCostTag tag = tagMapper.selectById(vo.getTagId());
            if (tag != null) { vo.setTagName(tag.getTagName()); }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertRule(PeriodRuleBo bo) {
        if (bo == null) { throw new ServiceException("规则参数不能为空"); }
        if (StrUtil.isBlank(bo.getRuleName())) { throw new ServiceException("规则名称不能为空"); }
        if (bo.getPeriodValue() == null) { throw new ServiceException("周期数值不能为空"); }
        if (StrUtil.isBlank(bo.getPeriodUnit())) { throw new ServiceException("时间单位不能为空"); }
        if (!VALID_UNITS.contains(bo.getPeriodUnit())) { throw new ServiceException("时间单位不合法：" + bo.getPeriodUnit()); }
        if (bo.getPeriodValue().compareTo(BigDecimal.ZERO) <= 0) { throw new ServiceException("周期数值必须大于0"); }
        // ★UNIQUE 防重
        Long existCount = ruleMapper.selectCount(Wrappers.<PeriodRule>lambdaQuery()
            .eq(PeriodRule::getRuleName, bo.getRuleName()));
        if (existCount > 0) { throw new ServiceException("该规则名称已存在"); }
        PeriodRule entity = new PeriodRule();
        entity.setRuleName(bo.getRuleName());
        entity.setPeriodValue(bo.getPeriodValue());
        entity.setPeriodUnit(bo.getPeriodUnit());
        entity.setTotalDays(calculateTotalDays(bo.getPeriodValue(), bo.getPeriodUnit()));
        entity.setTagId(bo.getTagId());
        entity.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "ENABLED");
        entity.setRemark(bo.getRemark());
        ruleMapper.insert(entity);
        log.info("[insertRule] ruleName={}, period={}{}", bo.getRuleName(), bo.getPeriodValue(), bo.getPeriodUnit());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRule(PeriodRuleBo bo) {
        if (bo == null || bo.getId() == null) { throw new ServiceException("规则参数不能为空"); }
        PeriodRule exist = ruleMapper.selectById(bo.getId());
        if (exist == null) { throw new ServiceException("周期规则不存在"); }
        if (StrUtil.isNotBlank(bo.getPeriodUnit()) && !VALID_UNITS.contains(bo.getPeriodUnit())) {
            throw new ServiceException("时间单位不合法：" + bo.getPeriodUnit());
        }
        PeriodRule entity = new PeriodRule();
        entity.setId(bo.getId());
        if (StrUtil.isNotBlank(bo.getRuleName())) { entity.setRuleName(bo.getRuleName()); }
        if (bo.getPeriodValue() != null) { entity.setPeriodValue(bo.getPeriodValue()); }
        if (StrUtil.isNotBlank(bo.getPeriodUnit())) { entity.setPeriodUnit(bo.getPeriodUnit()); }
        // 重算 totalDays
        BigDecimal val = bo.getPeriodValue() != null ? bo.getPeriodValue() : exist.getPeriodValue();
        String unit = StrUtil.isNotBlank(bo.getPeriodUnit()) ? bo.getPeriodUnit() : exist.getPeriodUnit();
        entity.setTotalDays(calculateTotalDays(val, unit));
        if (bo.getTagId() != null) { entity.setTagId(bo.getTagId()); }
        if (StrUtil.isNotBlank(bo.getStatus())) { entity.setStatus(bo.getStatus()); }
        if (bo.getRemark() != null) { entity.setRemark(bo.getRemark()); }
        log.info("[updateRule] id={}", bo.getId());
        return ruleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRule(Long id) {
        if (id == null) { throw new ServiceException("规则ID不能为空"); }
        PeriodRule exist = ruleMapper.selectById(id);
        if (exist == null) { throw new ServiceException("周期规则不存在"); }
        log.info("[deleteRule] id={}, ruleName={}", id, exist.getRuleName());
        return ruleMapper.deleteById(id);
    }

    // ===== 私有方法 =====

    private int calculateTotalDays(BigDecimal value, String unit) {
        Integer factor = UNIT_TO_DAYS.get(unit);
        if (factor == null) { return 0; }
        return value.multiply(new BigDecimal(factor)).intValue();
    }
}
