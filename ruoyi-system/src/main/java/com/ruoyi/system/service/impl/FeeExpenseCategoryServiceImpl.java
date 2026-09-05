package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeCategorySplitRatio;
import com.ruoyi.system.domain.FeeExpenseCategory;
import com.ruoyi.system.domain.FeeTenantCategory;
import com.ruoyi.system.domain.vo.FeeCategorySplitRatioVo;
import com.ruoyi.system.domain.vo.FeeExpenseCategoryVo;
import com.ruoyi.system.mapper.FeeCategorySplitRatioMapper;
import com.ruoyi.system.mapper.FeeExpenseCategoryMapper;
import com.ruoyi.system.mapper.FeeTenantCategoryMapper;
import com.ruoyi.system.service.IFeeExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 费用大类主数据Service实现（T5.13 / T1.5 三层角色分工）
 * fee_expense_category 为系统级共享（admin 超管维护），不按 tenant_id 隔离；
 * queryActiveList 返回当前租户已启用的大类（结合 fee_tenant_category）；
 * 内置大类（is_builtin=1）不可删除、编码不可改，仅可改名/启停
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeExpenseCategoryServiceImpl implements IFeeExpenseCategoryService {

    /** 拆分比例合计基准（%） */
    private static final BigDecimal RATIO_TOTAL = new BigDecimal("100");

    private final FeeExpenseCategoryMapper categoryMapper;
    private final FeeCategorySplitRatioMapper ratioMapper;
    private final FeeTenantCategoryMapper tenantCategoryMapper;

    @Override
    public List<FeeExpenseCategoryVo> queryList() {
        return categoryMapper.selectVoList(Wrappers.<FeeExpenseCategory>lambdaQuery()
            .orderByAsc(FeeExpenseCategory::getSortOrder)
            .orderByAsc(FeeExpenseCategory::getId));
    }

    @Override
    public List<FeeExpenseCategoryVo> queryActiveList() {
        // 三层角色分工：返回当前租户已启用的大类（platform_admin 勾选 + 存量兼容全部启用）
        List<FeeTenantCategory> tenantCategories = tenantCategoryMapper.selectList(
            Wrappers.<FeeTenantCategory>lambdaQuery()
                .eq(FeeTenantCategory::getIsEnabled, 1));
        boolean hasConfig = tenantCategoryMapper.selectCount(
            Wrappers.<FeeTenantCategory>lambdaQuery().select(FeeTenantCategory::getId)) > 0;
        // 存量兼容：无配置时视为全部启用
        if (!hasConfig) {
            return categoryMapper.selectVoList(Wrappers.<FeeExpenseCategory>lambdaQuery()
                .eq(FeeExpenseCategory::getIsActive, 1)
                .orderByAsc(FeeExpenseCategory::getSortOrder)
                .orderByAsc(FeeExpenseCategory::getId));
        }
        Set<String> enabledCodes = tenantCategories.stream()
            .map(FeeTenantCategory::getCategoryCode)
            .collect(Collectors.toSet());
        return categoryMapper.selectVoList(Wrappers.<FeeExpenseCategory>lambdaQuery()
            .eq(FeeExpenseCategory::getIsActive, 1)
            .in(FeeExpenseCategory::getCategoryCode, enabledCodes)
            .orderByAsc(FeeExpenseCategory::getSortOrder)
            .orderByAsc(FeeExpenseCategory::getId));
    }

    @Override
    public FeeExpenseCategoryVo queryById(Long id) {
        return categoryMapper.selectVoById(id);
    }

    @Override
    public Long saveCategory(FeeExpenseCategoryVo vo) {
        if (StrUtil.isBlank(vo.getCategoryCode()) || StrUtil.isBlank(vo.getCategoryName())) {
            throw new ServiceException("大类编码与名称不能为空");
        }
        // 编码全局唯一（系统级共享，不按租户隔离）
        FeeExpenseCategory exist = categoryMapper.selectOne(Wrappers.<FeeExpenseCategory>lambdaQuery()
            .eq(FeeExpenseCategory::getCategoryCode, vo.getCategoryCode())
            .ne(vo.getId() != null, FeeExpenseCategory::getId, vo.getId())
            .last("LIMIT 1"));
        if (exist != null) {
            throw new ServiceException("大类编码已存在：" + vo.getCategoryCode());
        }
        FeeExpenseCategory category = new FeeExpenseCategory();
        category.setCategoryName(vo.getCategoryName());
        category.setSortOrder(vo.getSortOrder() == null ? 0 : vo.getSortOrder());
        category.setIsActive(vo.getIsActive() == null ? 1 : vo.getIsActive());
        category.setRemark(vo.getRemark());
        if (vo.getId() != null) {
            FeeExpenseCategory old = categoryMapper.selectById(vo.getId());
            if (old == null) {
                throw new ServiceException("费用大类不存在");
            }
            // 内置大类编码固定（映射B依赖编码稳定，保历史数据可审计）
            if (old.getIsBuiltin() != null && old.getIsBuiltin() == 1
                && !old.getCategoryCode().equals(vo.getCategoryCode())) {
                throw new ServiceException("内置大类编码不可修改");
            }
            category.setId(vo.getId());
            category.setCategoryCode(vo.getCategoryCode());
            categoryMapper.updateById(category);
            return vo.getId();
        }
        category.setCategoryCode(vo.getCategoryCode());
        category.setIsBuiltin(0);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategory(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        List<FeeExpenseCategory> categories = categoryMapper.selectBatchIds(ids);
        if (categories.size() != ids.size()) {
            throw new ServiceException("部分费用大类不存在");
        }
        for (FeeExpenseCategory category : categories) {
            if (category.getIsBuiltin() != null && category.getIsBuiltin() == 1) {
                throw new ServiceException("内置大类「" + category.getCategoryName() + "」不可删除，可停用代替");
            }
        }
        // 级联删除默认拆分比例
        ratioMapper.delete(Wrappers.<FeeCategorySplitRatio>lambdaQuery()
            .in(FeeCategorySplitRatio::getCategoryId, ids));
        return categoryMapper.deleteBatchIds(ids);
    }

    @Override
    public List<FeeCategorySplitRatioVo> listRatios(Long categoryId) {
        return ratioMapper.selectVoList(Wrappers.<FeeCategorySplitRatio>lambdaQuery()
            .eq(FeeCategorySplitRatio::getCategoryId, categoryId)
            .orderByDesc(FeeCategorySplitRatio::getRatioPercent));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRatios(Long categoryId, List<FeeCategorySplitRatioVo> ratios) {
        FeeExpenseCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("费用大类不存在");
        }
        if (CollUtil.isEmpty(ratios)) {
            // 允许清空比例（回退为无默认配置）
            ratioMapper.delete(Wrappers.<FeeCategorySplitRatio>lambdaQuery()
                .eq(FeeCategorySplitRatio::getCategoryId, categoryId));
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (FeeCategorySplitRatioVo vo : ratios) {
            if (StrUtil.isBlank(vo.getSubjectCode()) || vo.getRatioPercent() == null) {
                throw new ServiceException("拆分比例行的科目编码与比例不能为空");
            }
            if (vo.getRatioPercent().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("拆分比例必须大于0");
            }
            total = total.add(vo.getRatioPercent());
        }
        if (total.compareTo(RATIO_TOTAL) != 0) {
            throw new ServiceException("拆分比例合计必须等于100%，当前合计：" + total + "%");
        }
        // 整体替换：先删后插，保证唯一约束与幂等
        ratioMapper.delete(Wrappers.<FeeCategorySplitRatio>lambdaQuery()
            .eq(FeeCategorySplitRatio::getCategoryId, categoryId));
        for (FeeCategorySplitRatioVo vo : ratios) {
            FeeCategorySplitRatio ratio = new FeeCategorySplitRatio();
            ratio.setCategoryId(categoryId);
            ratio.setCategoryCode(category.getCategoryCode());
            ratio.setSubjectCode(vo.getSubjectCode());
            ratio.setSubjectName(vo.getSubjectName());
            ratio.setRatioPercent(vo.getRatioPercent());
            ratioMapper.insert(ratio);
        }
    }
}
