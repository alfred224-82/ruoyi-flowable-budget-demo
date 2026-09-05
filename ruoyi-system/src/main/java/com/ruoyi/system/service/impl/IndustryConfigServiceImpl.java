package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeIndustryConfig;
import com.ruoyi.system.mapper.FeeIndustryConfigMapper;
import com.ruoyi.system.service.IIndustryConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行业配置Service业务层处理
 * 设计文档 Phase 4 Task 4.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryConfigServiceImpl implements IIndustryConfigService {

    private final FeeIndustryConfigMapper industryConfigMapper;

    @Override
    public FeeIndustryConfig queryByCode(String industryCode) {
        return industryConfigMapper.selectOne(
            Wrappers.<FeeIndustryConfig>lambdaQuery()
                .eq(FeeIndustryConfig::getIndustryCode, industryCode)
        );
    }

    @Override
    public List<FeeIndustryConfig> listAll() {
        return industryConfigMapper.selectList(
            Wrappers.<FeeIndustryConfig>lambdaQuery()
                .orderByAsc(FeeIndustryConfig::getId)
        );
    }

    @Override
    public Boolean save(FeeIndustryConfig config) {
        if (config.getId() != null) {
            return industryConfigMapper.updateById(config) > 0;
        }
        // 校验编码唯一
        FeeIndustryConfig existing = queryByCode(config.getIndustryCode());
        if (existing != null) {
            config.setId(existing.getId());
            return industryConfigMapper.updateById(config) > 0;
        }
        return industryConfigMapper.insert(config) > 0;
    }

    @Override
    public Boolean deleteById(Long id) {
        return industryConfigMapper.deleteById(id) > 0;
    }

    @Override
    public String getCurrentUserIndustry() {
        // 从登录用户获取部门绑定的行业编码
        // 简化实现：从用户扩展属性或部门配置中获取
        // 当前默认返回通用，后续可通过部门表扩展industry_code字段
        try {
            Long deptId = LoginHelper.getDeptId();
            if (deptId != null) {
                // 查询是否有启用的行业配置，取第一个作为当前行业
                List<FeeIndustryConfig> configs = industryConfigMapper.selectList(
                    Wrappers.<FeeIndustryConfig>lambdaQuery()
                        .eq(FeeIndustryConfig::getStatus, "ENABLED")
                        .last("LIMIT 1")
                );
                if (!configs.isEmpty()) {
                    return configs.get(0).getIndustryCode();
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户行业编码失败，使用通用配置", e);
        }
        return "*";
    }
}
