package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeAiPromptConfig;
import com.ruoyi.system.domain.bo.FeeAiPromptConfigBo;
import com.ruoyi.system.domain.vo.FeeAiPromptConfigVo;
import com.ruoyi.system.mapper.FeeAiPromptConfigMapper;
import com.ruoyi.system.service.IAiPromptConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI引导话术配置Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptConfigServiceImpl implements IAiPromptConfigService {

    private final FeeAiPromptConfigMapper aiPromptConfigMapper;

    /**
     * 按场景+行业获取话术模板
     * 策略：行业专属优先 → 通用回退
     */
    @Override
    public FeeAiPromptConfigVo getPrompt(String sceneCode, String industryCode) {
        // 1. 优先查询行业专属配置
        if (StrUtil.isNotBlank(industryCode) && !"*".equals(industryCode)) {
            FeeAiPromptConfig config = aiPromptConfigMapper.selectOne(
                Wrappers.<FeeAiPromptConfig>lambdaQuery()
                    .eq(FeeAiPromptConfig::getSceneCode, sceneCode)
                    .eq(FeeAiPromptConfig::getIndustryCode, industryCode)
                    .eq(FeeAiPromptConfig::getStatus, "ENABLED")
                    .last("LIMIT 1")
            );
            if (config != null) {
                return BeanUtil.toBean(config, FeeAiPromptConfigVo.class);
            }
        }
        // 2. 回退通用配置
        FeeAiPromptConfig config = aiPromptConfigMapper.selectOne(
            Wrappers.<FeeAiPromptConfig>lambdaQuery()
                .eq(FeeAiPromptConfig::getSceneCode, sceneCode)
                .eq(FeeAiPromptConfig::getIndustryCode, "*")
                .eq(FeeAiPromptConfig::getStatus, "ENABLED")
                .last("LIMIT 1")
        );
        return config != null ? BeanUtil.toBean(config, FeeAiPromptConfigVo.class) : null;
    }

    @Override
    public Page<FeeAiPromptConfigVo> selectPageList(FeeAiPromptConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeAiPromptConfig> lqw = buildQueryWrapper(bo);
        return aiPromptConfigMapper.selectVoPage(pageQuery.build(), lqw);
    }

    @Override
    public FeeAiPromptConfigVo queryById(Long id) {
        return aiPromptConfigMapper.selectVoById(id);
    }

    @Override
    public List<FeeAiPromptConfigVo> listEnabled() {
        return aiPromptConfigMapper.selectVoList(
            Wrappers.<FeeAiPromptConfig>lambdaQuery()
                .eq(FeeAiPromptConfig::getStatus, "ENABLED")
                .orderByAsc(FeeAiPromptConfig::getSceneCode)
        );
    }

    @Override
    public Boolean insertByBo(FeeAiPromptConfigBo bo) {
        // 校验场景编码+行业编码唯一性
        checkUnique(bo.getSceneCode(), bo.getIndustryCode(), null);
        FeeAiPromptConfig entity = BeanUtil.toBean(bo, FeeAiPromptConfig.class);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("ENABLED");
        }
        if (StrUtil.isBlank(entity.getIndustryCode())) {
            entity.setIndustryCode("*");
        }
        return aiPromptConfigMapper.insert(entity) > 0;
    }

    @Override
    public Boolean updateByBo(FeeAiPromptConfigBo bo) {
        // 校验场景编码+行业编码唯一性（排除自身）
        checkUnique(bo.getSceneCode(), bo.getIndustryCode(), bo.getId());
        FeeAiPromptConfig entity = BeanUtil.toBean(bo, FeeAiPromptConfig.class);
        return aiPromptConfigMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(List<Long> ids) {
        return aiPromptConfigMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<FeeAiPromptConfig> buildQueryWrapper(FeeAiPromptConfigBo bo) {
        LambdaQueryWrapper<FeeAiPromptConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StrUtil.isNotBlank(bo.getSceneCode()), FeeAiPromptConfig::getSceneCode, bo.getSceneCode());
        lqw.like(StrUtil.isNotBlank(bo.getSceneName()), FeeAiPromptConfig::getSceneName, bo.getSceneName());
        lqw.eq(StrUtil.isNotBlank(bo.getIndustryCode()), FeeAiPromptConfig::getIndustryCode, bo.getIndustryCode());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), FeeAiPromptConfig::getStatus, bo.getStatus());
        lqw.orderByAsc(FeeAiPromptConfig::getSceneCode);
        return lqw;
    }

    /**
     * 校验场景编码+行业编码唯一性
     */
    private void checkUnique(String sceneCode, String industryCode, Long excludeId) {
        LambdaQueryWrapper<FeeAiPromptConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(FeeAiPromptConfig::getSceneCode, sceneCode);
        lqw.eq(FeeAiPromptConfig::getIndustryCode, StrUtil.isBlank(industryCode) ? "*" : industryCode);
        lqw.ne(excludeId != null, FeeAiPromptConfig::getId, excludeId);
        if (aiPromptConfigMapper.selectCount(lqw) > 0) {
            throw new ServiceException("场景编码[" + sceneCode + "]在行业[" + industryCode + "]下已存在配置");
        }
    }
}
