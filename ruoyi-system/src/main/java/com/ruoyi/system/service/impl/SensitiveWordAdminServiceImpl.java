package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ImageComplianceConfig;
import com.ruoyi.system.domain.SensitiveWord;
import com.ruoyi.system.domain.bo.ImageComplianceConfigBo;
import com.ruoyi.system.domain.bo.SensitiveWordBo;
import com.ruoyi.system.domain.vo.ImageComplianceConfigVo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;
import com.ruoyi.system.mapper.ImageComplianceConfigMapper;
import com.ruoyi.system.mapper.SensitiveWordMapper;
import com.ruoyi.system.service.ISensitiveWordAdminService;
import com.ruoyi.system.service.ISensitiveWordEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin 全局合规管理服务实现（M18 T18.3/T18.4，platform_admin 专用）
 * <p>全局敏感词库 tenant_id=0；写操作后刷新引擎全局词缓存（evictWordCache(0)）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordAdminServiceImpl implements ISensitiveWordAdminService {

    private final SensitiveWordMapper wordMapper;
    private final ImageComplianceConfigMapper configMapper;
    private final ISensitiveWordEngine sensitiveWordEngine;

    /** 全局底库租户键 */
    private static final long GLOBAL_TENANT = 0L;

    /** 合法分类白名单（V3.8 附录 A 八大类 + CUSTOM） */
    private static final Set<String> VALID_CATEGORIES = new HashSet<>(Arrays.asList(
        "POLITICAL", "PORN", "VIOLENCE", "AD", "LAW", "SUPERSTITION", "ABUSE", "RUMOR", "DIALECT", "CUSTOM"));

    /** 合法匹配模式 */
    private static final Set<String> VALID_MATCH_MODES = new HashSet<>(Arrays.asList("exact", "fuzzy", "regex"));

    /** 合法图片违规类型 */
    private static final Set<String> VALID_VIOLATION_TYPES = new HashSet<>(Arrays.asList("porn", "violence", "politics"));

    @Override
    public TableDataInfo<SensitiveWordVo> queryWordPage(SensitiveWordBo bo, PageQuery pageQuery) {
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        LambdaQueryWrapper<SensitiveWord> wrapper = Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, GLOBAL_TENANT);
        if (bo != null) {
            if (StrUtil.isNotBlank(bo.getWord())) {
                wrapper.like(SensitiveWord::getWord, bo.getWord());
            }
            if (StrUtil.isNotBlank(bo.getCategory())) {
                wrapper.eq(SensitiveWord::getCategory, bo.getCategory());
            }
            if (StrUtil.isNotBlank(bo.getStatus())) {
                wrapper.eq(SensitiveWord::getStatus, bo.getStatus());
            }
            if (StrUtil.isNotBlank(bo.getMatchMode())) {
                wrapper.eq(SensitiveWord::getMatchMode, bo.getMatchMode());
            }
        }
        wrapper.orderByDesc(SensitiveWord::getCreateTime);
        Page<SensitiveWordVo> page = wordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SensitiveWordVo queryWordById(Long id) {
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWordVo vo = wordMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("敏感词不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertWord(SensitiveWordBo bo) {
        validateBo(bo, true);
        // ★UNIQUE 防重：tenant_id=0 下同 word+category 不重复
        Long exist = wordMapper.selectCount(Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, GLOBAL_TENANT)
            .eq(SensitiveWord::getWord, bo.getWord())
            .eq(SensitiveWord::getCategory, bo.getCategory()));
        if (exist != null && exist > 0) {
            throw new ServiceException("该敏感词已存在，请勿重复添加");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setTenantId(GLOBAL_TENANT);
        entity.setWord(bo.getWord());
        entity.setCategory(bo.getCategory());
        entity.setMatchMode(StrUtil.blankToDefault(bo.getMatchMode(), "exact"));
        entity.setIndustryScope(bo.getIndustryScope());
        entity.setStatus(StrUtil.blankToDefault(bo.getStatus(), "0"));
        entity.setSource("global");
        entity.setRemark(bo.getRemark());
        wordMapper.insert(entity);
        sensitiveWordEngine.evictWordCache(GLOBAL_TENANT);
        log.info("[insertWord] word={}, category={}, matchMode={}", entity.getWord(), entity.getCategory(), entity.getMatchMode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWord(SensitiveWordBo bo) {
        validateBo(bo, false);
        SensitiveWord exist = wordMapper.selectById(bo.getId());
        if (exist == null || exist.getTenantId() == null || exist.getTenantId() != GLOBAL_TENANT) {
            throw new ServiceException("全局敏感词不存在");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setId(bo.getId());
        entity.setWord(bo.getWord());
        entity.setCategory(bo.getCategory());
        entity.setMatchMode(bo.getMatchMode());
        entity.setIndustryScope(bo.getIndustryScope());
        entity.setStatus(bo.getStatus());
        entity.setRemark(bo.getRemark());
        int rows = wordMapper.updateById(entity);
        sensitiveWordEngine.evictWordCache(GLOBAL_TENANT);
        log.info("[updateWord] id={}", bo.getId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWord(Long id) {
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWord exist = wordMapper.selectById(id);
        if (exist == null || exist.getTenantId() == null || exist.getTenantId() != GLOBAL_TENANT) {
            throw new ServiceException("全局敏感词不存在");
        }
        int rows = wordMapper.deleteById(id);
        sensitiveWordEngine.evictWordCache(GLOBAL_TENANT);
        log.info("[deleteWord] id={}, word={}", id, exist.getWord());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importWords(List<String> words, String category, String matchMode) {
        if (words == null || words.isEmpty()) {
            throw new ServiceException("导入词列表不能为空");
        }
        String cat = StrUtil.blankToDefault(category, "CUSTOM");
        if (!VALID_CATEGORIES.contains(cat)) {
            throw new ServiceException("敏感词分类不合法：" + cat);
        }
        String mode = StrUtil.blankToDefault(matchMode, "exact");
        if (!VALID_MATCH_MODES.contains(mode)) {
            throw new ServiceException("匹配模式不合法：" + mode);
        }
        int count = 0;
        for (String word : words) {
            if (StrUtil.isBlank(word) || word.length() > 100) {
                continue;
            }
            Long exist = wordMapper.selectCount(Wrappers.<SensitiveWord>lambdaQuery()
                .eq(SensitiveWord::getTenantId, GLOBAL_TENANT)
                .eq(SensitiveWord::getWord, word)
                .eq(SensitiveWord::getCategory, cat));
            if (exist != null && exist > 0) {
                continue;
            }
            SensitiveWord entity = new SensitiveWord();
            entity.setTenantId(GLOBAL_TENANT);
            entity.setWord(word);
            entity.setCategory(cat);
            entity.setMatchMode(mode);
            entity.setStatus("0");
            entity.setSource("global");
            wordMapper.insert(entity);
            count++;
        }
        sensitiveWordEngine.evictWordCache(GLOBAL_TENANT);
        log.info("[importWords] category={}, matchMode={}, imported={}", cat, mode, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleSensitiveWord(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态不合法，可选：0启用/1禁用");
        }
        LambdaUpdateWrapper<SensitiveWord> wrapper = Wrappers.<SensitiveWord>lambdaUpdate()
            .eq(SensitiveWord::getTenantId, GLOBAL_TENANT)
            .set(SensitiveWord::getStatus, String.valueOf(status));
        int rows = wordMapper.update(null, wrapper);
        sensitiveWordEngine.evictWordCache(GLOBAL_TENANT);
        log.info("[toggleSensitiveWord] status={}, affected={}", status, rows);
        return rows;
    }

    @Override
    public List<ImageComplianceConfigVo> queryImageConfigs() {
        LambdaQueryWrapper<ImageComplianceConfig> wrapper = Wrappers.<ImageComplianceConfig>lambdaQuery()
            .eq(ImageComplianceConfig::getTenantId, GLOBAL_TENANT)
            .orderByAsc(ImageComplianceConfig::getViolationType);
        return configMapper.selectVoList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveImageConfig(ImageComplianceConfigBo bo) {
        if (bo == null || StrUtil.isBlank(bo.getViolationType())) {
            throw new ServiceException("违规类型不能为空");
        }
        if (!VALID_VIOLATION_TYPES.contains(bo.getViolationType())) {
            throw new ServiceException("违规类型不合法：" + bo.getViolationType());
        }
        ImageComplianceConfig exist = configMapper.selectOne(Wrappers.<ImageComplianceConfig>lambdaQuery()
            .eq(ImageComplianceConfig::getTenantId, GLOBAL_TENANT)
            .eq(ImageComplianceConfig::getViolationType, bo.getViolationType()));
        ImageComplianceConfig entity = exist != null ? exist : new ImageComplianceConfig();
        entity.setTenantId(GLOBAL_TENANT);
        entity.setViolationType(bo.getViolationType());
        if (bo.getEnabled() != null) {
            entity.setEnabled(bo.getEnabled());
        }
        if (bo.getConfidenceThreshold() != null) {
            entity.setConfidenceThreshold(bo.getConfidenceThreshold());
        }
        if (bo.getOcrEnabled() != null) {
            entity.setOcrEnabled(bo.getOcrEnabled());
        }
        if (bo.getVisualApiEnabled() != null) {
            entity.setVisualApiEnabled(bo.getVisualApiEnabled());
        }
        if (bo.getMaxFileSizeMb() != null) {
            entity.setMaxFileSizeMb(bo.getMaxFileSizeMb());
        }
        if (StrUtil.isNotBlank(bo.getAllowedFormats())) {
            entity.setAllowedFormats(bo.getAllowedFormats());
        }
        entity.setRemark(bo.getRemark());
        int rows = exist != null ? configMapper.updateById(entity) : configMapper.insert(entity);
        log.info("[saveImageConfig] violationType={}", bo.getViolationType());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleImageConfig(String violationType, Integer enabled) {
        if (StrUtil.isBlank(violationType) || !VALID_VIOLATION_TYPES.contains(violationType)) {
            throw new ServiceException("违规类型不合法：" + violationType);
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new ServiceException("状态不合法，可选：0关闭/1启用");
        }
        LambdaUpdateWrapper<ImageComplianceConfig> wrapper = Wrappers.<ImageComplianceConfig>lambdaUpdate()
            .eq(ImageComplianceConfig::getTenantId, GLOBAL_TENANT)
            .eq(ImageComplianceConfig::getViolationType, violationType)
            .set(ImageComplianceConfig::getEnabled, enabled);
        int rows = configMapper.update(null, wrapper);
        log.info("[toggleImageConfig] violationType={}, enabled={}, affected={}", violationType, enabled, rows);
        return rows;
    }

    /**
     * 敏感词 Bo 校验（fail-fast + 白名单）。
     */
    private void validateBo(SensitiveWordBo bo, boolean isAdd) {
        if (bo == null) {
            throw new ServiceException("敏感词参数不能为空");
        }
        if (!isAdd && bo.getId() == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        if (StrUtil.isBlank(bo.getWord())) {
            throw new ServiceException("敏感词内容不能为空");
        }
        if (bo.getWord().length() > 100) {
            throw new ServiceException("敏感词长度不能超过100");
        }
        if (StrUtil.isBlank(bo.getCategory())) {
            throw new ServiceException("敏感词分类不能为空");
        }
        if (!VALID_CATEGORIES.contains(bo.getCategory())) {
            throw new ServiceException("敏感词分类不合法：" + bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getMatchMode()) && !VALID_MATCH_MODES.contains(bo.getMatchMode())) {
            throw new ServiceException("匹配模式不合法：" + bo.getMatchMode());
        }
    }
}
