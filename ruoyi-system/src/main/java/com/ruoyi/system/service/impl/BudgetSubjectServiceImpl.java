package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.bo.BudgetSubjectBo;
import com.ruoyi.system.domain.vo.BudgetSubjectVo;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.service.IBudgetSubjectService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 预算科目Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-30
 */
@RequiredArgsConstructor
@Service
public class BudgetSubjectServiceImpl implements IBudgetSubjectService {

    private final BudgetSubjectMapper baseMapper;

    /**
     * 查询预算科目
     */
    @Override
    public BudgetSubjectVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询预算科目列表
     */
    @Override
    public TableDataInfo<BudgetSubjectVo> queryPageList(BudgetSubjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BudgetSubject> lqw = buildQueryWrapper(bo);
        Page<BudgetSubjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询预算科目列表
     */
    @Override
    public List<BudgetSubjectVo> queryList(BudgetSubjectBo bo) {
        LambdaQueryWrapper<BudgetSubject> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BudgetSubject> buildQueryWrapper(BudgetSubjectBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BudgetSubject> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getSubjectCode()), BudgetSubject::getSubjectCode, bo.getSubjectCode());
        lqw.like(StringUtils.isNotBlank(bo.getSubjectName()), BudgetSubject::getSubjectName, bo.getSubjectName());
        lqw.eq(bo.getParentId() != null, BudgetSubject::getParentId, bo.getParentId());
        lqw.eq(StringUtils.isNotBlank(bo.getParentCode()), BudgetSubject::getParentCode, bo.getParentCode());
        lqw.eq(bo.getLevel() != null, BudgetSubject::getLevel, bo.getLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getAncestors()), BudgetSubject::getAncestors, bo.getAncestors());
        lqw.eq(bo.getIsLeaf() != null, BudgetSubject::getIsLeaf, bo.getIsLeaf());
        lqw.eq(bo.getSortOrder() != null, BudgetSubject::getSortOrder, bo.getSortOrder());
        lqw.eq(bo.getIsActive() != null, BudgetSubject::getIsActive, bo.getIsActive());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), BudgetSubject::getDescription, bo.getDescription());
        return lqw;
    }

    /**
     * 新增预算科目
     */
    @Override
    public Boolean insertByBo(BudgetSubjectBo bo) {
        BudgetSubject add = BeanUtil.toBean(bo, BudgetSubject.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改预算科目
     */
    @Override
    public Boolean updateByBo(BudgetSubjectBo bo) {
        BudgetSubject update = BeanUtil.toBean(bo, BudgetSubject.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BudgetSubject entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除预算科目
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 查询一级科目列表（启用状态）
     */
    @Override
    public List<BudgetSubjectVo> listFirstLevel() {
        return baseMapper.selectVoList(
            Wrappers.<BudgetSubject>lambdaQuery()
                .eq(BudgetSubject::getLevel, 1L)
                .eq(BudgetSubject::getIsActive, 1)
                .orderByAsc(BudgetSubject::getSortOrder)
        );
    }

    /**
     * 根据父级ID查询二级科目列表（启用状态）
     */
    @Override
    public List<BudgetSubjectVo> listSecondLevel(Long parentId) {
        return baseMapper.selectVoList(
            Wrappers.<BudgetSubject>lambdaQuery()
                .eq(BudgetSubject::getParentId, parentId)
                .eq(BudgetSubject::getIsActive, 1)
                .orderByAsc(BudgetSubject::getSortOrder)
        );
    }

    /**
     * 查询所有启用的科目（用于树形展示）
     */
    @Override
    public List<BudgetSubjectVo> listAllSubjects() {
        return baseMapper.selectVoList(
            Wrappers.<BudgetSubject>lambdaQuery()
                .eq(BudgetSubject::getIsActive, 1)
                .orderByAsc(BudgetSubject::getSubjectType)
                .orderByAsc(BudgetSubject::getSortOrder)
        );
    }
}
