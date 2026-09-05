package com.ruoyi.system.service;

import com.ruoyi.system.domain.FeeIndustryConfig;

import java.util.List;

/**
 * 行业配置Service接口
 * 设计文档 Phase 4 Task 4.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IIndustryConfigService {

    /**
     * 根据行业编码查询配置
     */
    FeeIndustryConfig queryByCode(String industryCode);

    /**
     * 查询全部行业配置列表
     */
    List<FeeIndustryConfig> listAll();

    /**
     * 新增/编辑行业配置
     */
    Boolean save(FeeIndustryConfig config);

    /**
     * 删除行业配置
     */
    Boolean deleteById(Long id);

    /**
     * 获取当前用户所属行业编码
     * 从登录用户部门绑定的industry_code获取，无则返回 "*"
     */
    String getCurrentUserIndustry();
}
