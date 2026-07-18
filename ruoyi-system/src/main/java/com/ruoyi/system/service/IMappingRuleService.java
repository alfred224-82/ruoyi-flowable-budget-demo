package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.MappingRuleBo;
import com.ruoyi.system.domain.vo.BudgetPreviewVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;

import java.util.Collection;

/**
 * 映射规则Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IMappingRuleService {

    /**
     * 查询映射规则详情
     *
     * @param id 主键ID
     * @return 映射规则VO
     */
    MappingRuleVo queryById(Long id);

    /**
     * 分页查询映射规则列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<MappingRuleVo> selectPageList(MappingRuleBo bo, PageQuery pageQuery);

    /**
     * 新增映射规则
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean insertByBo(MappingRuleBo bo);

    /**
     * 修改映射规则
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean updateByBo(MappingRuleBo bo);

    /**
     * 批量删除映射规则
     *
     * @param ids 主键集合
     * @return 影响行数
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 匹配映射规则
     *
     * @param itemCategory 物资小类编码
     * @param purposeTag   用途标签编码
     * @param deptId       部门ID
     * @return 匹配的规则VO
     */
    MappingRuleVo matchRule(String itemCategory, String purposeTag, Long deptId);

    /**
     * 预览预算归属（不抛异常，返回完整预览信息）
     *
     * @param itemCategory 物资小类编码
     * @param purposeTag   用途标签编码
     * @param deptId       部门ID
     * @return 预算归属预览结果
     */
    BudgetPreviewVo previewBudgetSubject(String itemCategory, String purposeTag, Long deptId);
}
