package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeeTenantCategoryVo;

import java.util.List;

/**
 * 租户大类启用Service接口（T5.13 三层角色分工）
 * platform_admin 从 admin 定义的大类中勾选租户需要启用的大类
 *
 * @author ruoyi
 * @date 2026-08-30
 */
public interface IFeeTenantCategoryService {

    /**
     * 查询当前租户已启用的大类列表（含未启用，platform_admin 勾选管理用）
     * 返回所有系统大类 + 当前租户启用状态
     */
    List<FeeTenantCategoryVo> queryTenantCategoryList();

    /**
     * 查询当前租户已启用的大类（仅启用状态，业务下拉用）
     */
    List<FeeTenantCategoryVo> queryEnabledList();

    /**
     * 批量保存租户大类启用配置（整体替换）
     * platform_admin 勾选后提交，传入需要启用的大类编码列表
     *
     * @param categoryCodes 需要启用的大类编码列表
     */
    void saveTenantCategories(List<String> categoryCodes);

    /**
     * 检查指定大类是否在当前租户启用
     *
     * @param categoryCode 大类编码
     * @return true=已启用
     */
    boolean isCategoryEnabled(String categoryCode);
}
