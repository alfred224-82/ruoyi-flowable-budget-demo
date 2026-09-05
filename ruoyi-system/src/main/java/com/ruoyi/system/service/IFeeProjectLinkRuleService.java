package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo;

import java.util.List;
import java.util.Set;

/**
 * 项目关联规则Service接口（T5.7 / ADR-023）
 * 三层内置：角色级硬规则（项目经理必填）+ 大类级软提示 + 财务兜底补关联
 * 未选项目默认公司级（project_id=0）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeProjectLinkRuleService {

    /**
     * 全部规则列表
     */
    List<FeeProjectLinkRuleVo> queryList();

    /**
     * 新增/修改规则（scope+target 唯一）
     */
    Long saveRule(FeeProjectLinkRuleVo vo);

    /**
     * 删除规则
     */
    int deleteRule(Long id);

    /**
     * 匹配当前角色与大类的关联规则
     * 优先级：角色级硬规则 > 大类级软提示；无匹配返回 null（前端兜底公司级）
     *
     * @param roleKeys   当前用户角色标识集合
     * @param categoryId 费用大类ID（可空）
     */
    FeeProjectLinkRuleVo matchRule(Set<String> roleKeys, Long categoryId);

    /**
     * 按物资小类编码匹配大类级规则（大类目标存大类编码，小类前缀匹配，租户级覆盖内置，T6.7）
     */
    FeeProjectLinkRuleVo matchCategoryRule(String itemCategory);

    /**
     * 查询指定项目的严格模式规则（T6.7 / FEAT_LINK_RULE_CUSTOM，无配置返回 null）
     */
    FeeProjectLinkRuleVo queryProjectRule(Long projectId);
}
