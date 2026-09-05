package com.ruoyi.common.core.service;

/**
 * 功能开关检查接口
 * 由 LicenseServiceImpl 实现，供 FeatureInterceptor 调用
 * 解耦 framework 层对 system 层的直接依赖
 *
 * @author ruoyi
 * @date 2026-07-24
 */
public interface FeatureChecker {

    /**
     * 检查指定功能是否已授权
     *
     * @param featureCode 功能开关编码
     * @return true=已授权 false=未授权
     */
    boolean isFeatureEnabled(String featureCode);

    /**
     * 获取当前授权版本名称
     *
     * @return 版本名称（如 BASIC/ADVANCED/PROFESSIONAL，兼容存量 ENTERPRISE）
     */
    String getCurrentEdition();
}
