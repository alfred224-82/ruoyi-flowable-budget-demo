package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FixedCostTagBo;
import com.ruoyi.system.domain.bo.PeriodRuleBo;
import com.ruoyi.system.domain.vo.FixedCostTagVo;
import com.ruoyi.system.domain.vo.PeriodRuleVo;

import java.util.List;

/**
 * 标签与周期规则服务接口（T18.9，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface ITagPeriodRuleService {

    // ===== 标签管理 =====
    TableDataInfo<FixedCostTagVo> queryTagPage(FixedCostTagBo bo, PageQuery pageQuery);
    List<FixedCostTagVo> queryTagAll(FixedCostTagBo bo);
    FixedCostTagVo queryTagById(Long id);
    Long insertTag(FixedCostTagBo bo);
    int updateTag(FixedCostTagBo bo);
    int deleteTag(Long id);

    // ===== 周期规则管理 =====
    TableDataInfo<PeriodRuleVo> queryRulePage(PeriodRuleBo bo, PageQuery pageQuery);
    List<PeriodRuleVo> queryRuleAll(PeriodRuleBo bo);
    PeriodRuleVo queryRuleById(Long id);
    Long insertRule(PeriodRuleBo bo);
    int updateRule(PeriodRuleBo bo);
    int deleteRule(Long id);
}
