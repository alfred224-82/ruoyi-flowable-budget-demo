package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeExpenseCategory;
import com.ruoyi.system.domain.FeeTenantCategory;
import com.ruoyi.system.domain.vo.FeeTenantCategoryVo;
import com.ruoyi.system.mapper.FeeExpenseCategoryMapper;
import com.ruoyi.system.mapper.FeeTenantCategoryMapper;
import com.ruoyi.system.service.IFeeTenantCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户大类启用Service业务层处理（T5.13 三层角色分工）
 * platform_admin 从 admin 定义的大类中勾选租户需要启用的大类
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeTenantCategoryServiceImpl implements IFeeTenantCategoryService {

    private final FeeExpenseCategoryMapper categoryMapper;
    private final FeeTenantCategoryMapper tenantCategoryMapper;

    @Override
    public List<FeeTenantCategoryVo> queryTenantCategoryList() {
        // 1. 查询所有系统级启用的费用大类（admin 维护的主数据）
        List<FeeExpenseCategory> allCategories = categoryMapper.selectList(
            Wrappers.<FeeExpenseCategory>lambdaQuery()
                .eq(FeeExpenseCategory::getIsActive, 1)
                .orderByAsc(FeeExpenseCategory::getSortOrder)
                .orderByAsc(FeeExpenseCategory::getId));

        // 2. 查询当前租户已启用的大类编码集合
        Set<String> enabledCodes = getEnabledCategoryCodes();

        // 3. 合并：如果租户无任何配置记录（存量兼容），视为全部启用
        boolean hasConfig = tenantCategoryMapper.selectCount(
            Wrappers.<FeeTenantCategory>lambdaQuery().select(FeeTenantCategory::getId)) > 0;
        boolean allEnabled = !hasConfig;

        List<FeeTenantCategoryVo> result = new ArrayList<>();
        for (FeeExpenseCategory cat : allCategories) {
            FeeTenantCategoryVo vo = new FeeTenantCategoryVo();
            vo.setCategoryCode(cat.getCategoryCode());
            vo.setCategoryName(cat.getCategoryName());
            if (allEnabled) {
                vo.setIsEnabled(1);
            } else {
                vo.setIsEnabled(enabledCodes.contains(cat.getCategoryCode()) ? 1 : 0);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<FeeTenantCategoryVo> queryEnabledList() {
        // 查询当前租户已启用的大类
        List<FeeTenantCategory> tenantCategories = tenantCategoryMapper.selectList(
            Wrappers.<FeeTenantCategory>lambdaQuery()
                .eq(FeeTenantCategory::getIsEnabled, 1));

        // 存量兼容：如果租户无任何配置，视为全部启用
        if (tenantCategories.isEmpty()) {
            boolean hasConfig = tenantCategoryMapper.selectCount(
                Wrappers.<FeeTenantCategory>lambdaQuery().select(FeeTenantCategory::getId)) > 0;
            if (!hasConfig) {
                // 返回所有系统大类作为默认启用
                List<FeeExpenseCategory> allCategories = categoryMapper.selectList(
                    Wrappers.<FeeExpenseCategory>lambdaQuery()
                        .eq(FeeExpenseCategory::getIsActive, 1)
                        .orderByAsc(FeeExpenseCategory::getSortOrder));
                return allCategories.stream().map(cat -> {
                    FeeTenantCategoryVo vo = new FeeTenantCategoryVo();
                    vo.setCategoryCode(cat.getCategoryCode());
                    vo.setCategoryName(cat.getCategoryName());
                    vo.setIsEnabled(1);
                    return vo;
                }).collect(Collectors.toList());
            }
        }

        return tenantCategories.stream().map(tc -> {
            FeeTenantCategoryVo vo = new FeeTenantCategoryVo();
            vo.setId(tc.getId());
            vo.setCategoryCode(tc.getCategoryCode());
            vo.setCategoryName(tc.getCategoryName());
            vo.setIsEnabled(tc.getIsEnabled());
            vo.setCreateTime(tc.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTenantCategories(List<String> categoryCodes) {
        // 1. 校验传入的大类编码均存在于系统主数据中
        List<FeeExpenseCategory> allCategories = categoryMapper.selectList(
            Wrappers.<FeeExpenseCategory>lambdaQuery()
                .eq(FeeExpenseCategory::getIsActive, 1));
        Set<String> validCodes = allCategories.stream()
            .map(FeeExpenseCategory::getCategoryCode)
            .collect(Collectors.toSet());

        if (categoryCodes != null) {
            for (String code : categoryCodes) {
                if (!validCodes.contains(code)) {
                    throw new ServiceException("无效的大类编码：" + code);
                }
            }
        }

        // 2. 删除当前租户的全部旧配置
        tenantCategoryMapper.delete(
            Wrappers.<FeeTenantCategory>lambdaQuery());

        // 3. 插入新的启用配置
        if (categoryCodes != null && !categoryCodes.isEmpty()) {
            for (String code : categoryCodes) {
                // 查找大类名称
                String catName = allCategories.stream()
                    .filter(c -> c.getCategoryCode().equals(code))
                    .map(FeeExpenseCategory::getCategoryName)
                    .findFirst().orElse(code);

                FeeTenantCategory tc = new FeeTenantCategory();
                tc.setCategoryCode(code);
                tc.setCategoryName(catName);
                tc.setIsEnabled(1);
                tenantCategoryMapper.insert(tc);
            }
        }
        log.info("[TenantCategory] 租户大类启用配置已更新，启用数量: {}", categoryCodes != null ? categoryCodes.size() : 0);
    }

    @Override
    public boolean isCategoryEnabled(String categoryCode) {
        if (StrUtil.isBlank(categoryCode)) {
            return false;
        }
        // 存量兼容：无配置时视为全部启用
        boolean hasConfig = tenantCategoryMapper.selectCount(
            Wrappers.<FeeTenantCategory>lambdaQuery().select(FeeTenantCategory::getId)) > 0;
        if (!hasConfig) {
            return true;
        }
        Long count = tenantCategoryMapper.selectCount(
            Wrappers.<FeeTenantCategory>lambdaQuery()
                .eq(FeeTenantCategory::getCategoryCode, categoryCode)
                .eq(FeeTenantCategory::getIsEnabled, 1));
        return count != null && count > 0;
    }

    /**
     * 获取当前租户已启用的大类编码集合
     */
    private Set<String> getEnabledCategoryCodes() {
        List<FeeTenantCategory> list = tenantCategoryMapper.selectList(
            Wrappers.<FeeTenantCategory>lambdaQuery()
                .eq(FeeTenantCategory::getIsEnabled, 1));
        return list.stream()
            .map(FeeTenantCategory::getCategoryCode)
            .collect(Collectors.toSet());
    }
}
