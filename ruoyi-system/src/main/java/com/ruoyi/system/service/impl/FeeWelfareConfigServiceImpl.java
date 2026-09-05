package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.FeeWelfareConfig;
import com.ruoyi.system.domain.bo.FeeWelfareConfigBo;
import com.ruoyi.system.domain.vo.FeeWelfareConfigVo;
import com.ruoyi.system.mapper.FeeWelfareConfigMapper;
import com.ruoyi.system.service.IFeeWelfareConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公司福利政策配置Service实现
 * 设计文档 Phase 17 Task 17.3
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeWelfareConfigServiceImpl implements IFeeWelfareConfigService {

    private final FeeWelfareConfigMapper welfareConfigMapper;

    @Override
    public TableDataInfo<FeeWelfareConfigVo> queryPageList(FeeWelfareConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeWelfareConfig> wrapper = buildQueryWrapper(bo);
        Page<FeeWelfareConfigVo> page = welfareConfigMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public FeeWelfareConfigVo queryById(Long id) {
        return welfareConfigMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(FeeWelfareConfigBo bo) {
        // 福利编码唯一性校验（租户内）
        Long count = welfareConfigMapper.selectCount(
            Wrappers.<FeeWelfareConfig>lambdaQuery()
                .eq(FeeWelfareConfig::getTenantId, bo.getTenantId())
                .eq(FeeWelfareConfig::getWelfareCode, bo.getWelfareCode()));
        if (count > 0) {
            throw new ServiceException("福利编码已存在：" + bo.getWelfareCode());
        }

        FeeWelfareConfig config = BeanCopyUtils.copy(bo, FeeWelfareConfig.class);
        config.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "ENABLED");
        return welfareConfigMapper.insert(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(FeeWelfareConfigBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("福利配置ID不能为空");
        }
        FeeWelfareConfig existing = welfareConfigMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("福利配置不存在");
        }

        FeeWelfareConfig config = BeanCopyUtils.copy(bo, FeeWelfareConfig.class);
        return welfareConfigMapper.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        return welfareConfigMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importWelfare(List<FeeWelfareConfigBo> list) {
        if (list == null || list.isEmpty()) {
            throw new ServiceException("导入数据不能为空");
        }

        int successCount = 0;
        for (FeeWelfareConfigBo bo : list) {
            try {
                // 检查是否已存在
                FeeWelfareConfig existing = welfareConfigMapper.selectOne(
                    Wrappers.<FeeWelfareConfig>lambdaQuery()
                        .eq(FeeWelfareConfig::getTenantId, bo.getTenantId())
                        .eq(FeeWelfareConfig::getWelfareCode, bo.getWelfareCode()));

                FeeWelfareConfig config = BeanCopyUtils.copy(bo, FeeWelfareConfig.class);
                if (existing != null) {
                    config.setId(existing.getId());
                    welfareConfigMapper.updateById(config);
                } else {
                    config.setStatus("ENABLED");
                    welfareConfigMapper.insert(config);
                }
                successCount++;
            } catch (Exception e) {
                log.warn("[Welfare] 导入福利配置失败：{} - {}", bo.getWelfareCode(), e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public List<FeeWelfareConfigVo> exportWelfare(FeeWelfareConfigBo bo) {
        LambdaQueryWrapper<FeeWelfareConfig> wrapper = buildQueryWrapper(bo);
        return welfareConfigMapper.selectVoList(wrapper);
    }

    @Override
    public List<FeeWelfareConfigVo> matchWelfare(String region, Integer tripDays, String cityLevel, String countryGroup) {
        LambdaQueryWrapper<FeeWelfareConfig> wrapper = Wrappers.lambdaQuery();

        // 基础条件：启用状态 + 差旅类 + 有效期内
        wrapper.eq(FeeWelfareConfig::getStatus, "ENABLED")
               .eq(FeeWelfareConfig::getCategory, "TRAVEL")
               .le(FeeWelfareConfig::getEffectiveDate, new Date())
               .and(w -> w.isNull(FeeWelfareConfig::getExpiryDate)
                          .or().ge(FeeWelfareConfig::getExpiryDate, new Date()));

        // 区域匹配
        wrapper.and(w -> w.eq(FeeWelfareConfig::getRegion, region)
                          .or().eq(FeeWelfareConfig::getRegion, "ALL"));

        // 国内：按城市等级匹配
        if ("DOMESTIC".equals(region)) {
            wrapper.and(w -> w.eq(FeeWelfareConfig::getCityLevel, cityLevel)
                              .or().eq(FeeWelfareConfig::getCityLevel, "ANY"));
        }

        // 国外：按国家分组匹配
        if ("INTERNATIONAL".equals(region)) {
            wrapper.and(w -> w.eq(FeeWelfareConfig::getCountryGroup, countryGroup)
                              .or().eq(FeeWelfareConfig::getCountryGroup, "ANY"));
        }

        List<FeeWelfareConfig> configs = welfareConfigMapper.selectList(wrapper);

        // 短期/长期自动切换：超过30天返回长期标准
        return configs.stream()
            .filter(c -> matchDuration(c, tripDays))
            .map(c -> BeanCopyUtils.copy(c, FeeWelfareConfigVo.class))
            .collect(Collectors.toList());
    }

    /**
     * 匹配出差时长
     */
    private boolean matchDuration(FeeWelfareConfig config, Integer tripDays) {
        if (tripDays == null) {
            return true;
        }
        String duration = config.getTripDuration();
        if ("ANY".equals(duration) || StrUtil.isBlank(duration)) {
            return true;
        }
        if ("SHORT".equals(duration)) {
            return tripDays <= 30;
        }
        if ("LONG".equals(duration)) {
            return tripDays > 30;
        }
        return false;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<FeeWelfareConfig> buildQueryWrapper(FeeWelfareConfigBo bo) {
        LambdaQueryWrapper<FeeWelfareConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(bo.getTenantId() != null, FeeWelfareConfig::getTenantId, bo.getTenantId())
               .like(StrUtil.isNotBlank(bo.getWelfareName()), FeeWelfareConfig::getWelfareName, bo.getWelfareName())
               .eq(StrUtil.isNotBlank(bo.getWelfareCode()), FeeWelfareConfig::getWelfareCode, bo.getWelfareCode())
               .eq(StrUtil.isNotBlank(bo.getCategory()), FeeWelfareConfig::getCategory, bo.getCategory())
               .eq(StrUtil.isNotBlank(bo.getSubCategory()), FeeWelfareConfig::getSubCategory, bo.getSubCategory())
               .eq(StrUtil.isNotBlank(bo.getRegion()), FeeWelfareConfig::getRegion, bo.getRegion())
               .eq(StrUtil.isNotBlank(bo.getStatus()), FeeWelfareConfig::getStatus, bo.getStatus())
               .orderByDesc(FeeWelfareConfig::getCreateTime);
        return wrapper;
    }
}
