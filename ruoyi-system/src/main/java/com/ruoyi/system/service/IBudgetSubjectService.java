package com.ruoyi.system.service;

import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.domain.vo.BudgetSubjectVo;
import com.ruoyi.system.domain.bo.BudgetSubjectBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 预算科目Service接口
 *
 * @author ruoyi
 * @date 2026-05-30
 */
public interface IBudgetSubjectService {

    /**
     * 查询预算科目
     */
    BudgetSubjectVo queryById(Long id);

    /**
     * 查询预算科目列表
     */
    TableDataInfo<BudgetSubjectVo> queryPageList(BudgetSubjectBo bo, PageQuery pageQuery);

    /**
     * 查询预算科目列表
     */
    List<BudgetSubjectVo> queryList(BudgetSubjectBo bo);

    /**
     * 新增预算科目
     */
    Boolean insertByBo(BudgetSubjectBo bo);

    /**
     * 修改预算科目
     */
    Boolean updateByBo(BudgetSubjectBo bo);

    /**
     * 校验并批量删除预算科目信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询一级科目列表（启用状态）
     */
    List<BudgetSubjectVo> listFirstLevel();

    /**
     * 根据父级ID查询二级科目列表（启用状态）
     */
    List<BudgetSubjectVo> listSecondLevel(Long parentId);

    /**
     * 查询所有启用的科目（用于树形展示）
     */
    List<BudgetSubjectVo> listAllSubjects();
}
