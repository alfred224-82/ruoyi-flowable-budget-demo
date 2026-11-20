package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SensitiveWordLibrary;
import com.ruoyi.system.domain.bo.SensitiveWordLibraryBo;
import com.ruoyi.system.domain.vo.SensitiveWordLibraryVo;
import com.ruoyi.system.mapper.SensitiveWordLibraryMapper;
import com.ruoyi.system.service.ISensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 敏感词库服务实现（T18.1~T18.3，小微版 M18）
 * <p>★Admin 管理平台级（tenant_id=NULL）；租户管理员管理自定义（tenant_id=当前租户）。
 * <p>★fail-fast：入口判空 + DB 返回判空 + UNIQUE 防重。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements ISensitiveWordService {

    private final SensitiveWordLibraryMapper wordMapper;

    /** 合法敏感词分类白名单 */
    private static final Set<String> VALID_CATEGORIES = new HashSet<>(
        Arrays.asList("POLITICAL", "PORN", "VIOLENCE", "ABUSE", "CUSTOM"));

    /** 合法级别白名单 */
    private static final Set<String> VALID_LEVELS = new HashSet<>(
        Arrays.asList("WARN", "BLOCK"));

    @Override
    public TableDataInfo<SensitiveWordLibraryVo> queryWordPage(SensitiveWordLibraryBo bo, PageQuery pageQuery, boolean platformLevel) {
        // ★fail-fast
        if (bo == null) {
            bo = new SensitiveWordLibraryBo();
        }
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        LambdaQueryWrapper<SensitiveWordLibrary> wrapper = Wrappers.<SensitiveWordLibrary>lambdaQuery();
        if (platformLevel) {
            // Admin：只查平台级（tenant_id IS NULL）
            wrapper.isNull(SensitiveWordLibrary::getTenantId);
        } else {
            // 租户管理员：查平台底库 + 本租户自定义（OR 条件）
            Long tenantId = LoginHelper.getTenantId();
            wrapper.and(w -> w.isNull(SensitiveWordLibrary::getTenantId)
                .or().eq(SensitiveWordLibrary::getTenantId, tenantId));
        }
        if (StrUtil.isNotBlank(bo.getWord())) {
            wrapper.like(SensitiveWordLibrary::getWord, bo.getWord());
        }
        if (StrUtil.isNotBlank(bo.getCategory())) {
            wrapper.eq(SensitiveWordLibrary::getCategory, bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getLevel())) {
            wrapper.eq(SensitiveWordLibrary::getLevel, bo.getLevel());
        }
        if (StrUtil.isNotBlank(bo.getStatus())) {
            wrapper.eq(SensitiveWordLibrary::getStatus, bo.getStatus());
        }
        wrapper.orderByDesc(SensitiveWordLibrary::getCreateTime);
        Page<SensitiveWordLibraryVo> page = wordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SensitiveWordLibraryVo queryWordById(Long id) {
        // ★fail-fast
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWordLibraryVo vo = wordMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("敏感词不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertWord(SensitiveWordLibraryBo bo, boolean platformLevel) {
        // ★fail-fast
        if (bo == null) {
            throw new ServiceException("敏感词参数不能为空");
        }
        if (StrUtil.isBlank(bo.getWord())) {
            throw new ServiceException("敏感词内容不能为空");
        }
        if (bo.getWord().length() > 100) {
            throw new ServiceException("敏感词长度不能超过100");
        }
        if (StrUtil.isNotBlank(bo.getCategory()) && !VALID_CATEGORIES.contains(bo.getCategory())) {
            throw new ServiceException("敏感词分类不合法：" + bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getLevel()) && !VALID_LEVELS.contains(bo.getLevel())) {
            throw new ServiceException("敏感词级别不合法：" + bo.getLevel());
        }
        // ★UNIQUE 防重：同租户/平台下同词不重复
        LambdaQueryWrapper<SensitiveWordLibrary> existWrapper = Wrappers.<SensitiveWordLibrary>lambdaQuery()
            .eq(SensitiveWordLibrary::getWord, bo.getWord());
        if (platformLevel) {
            existWrapper.isNull(SensitiveWordLibrary::getTenantId);
        } else {
            existWrapper.eq(SensitiveWordLibrary::getTenantId, LoginHelper.getTenantId());
        }
        Long existCount = wordMapper.selectCount(existWrapper);
        if (existCount > 0) {
            throw new ServiceException("该敏感词已存在，请勿重复添加");
        }
        SensitiveWordLibrary entity = new SensitiveWordLibrary();
        entity.setTenantId(platformLevel ? null : LoginHelper.getTenantId());
        entity.setWord(bo.getWord());
        entity.setCategory(StrUtil.isNotBlank(bo.getCategory()) ? bo.getCategory() : "CUSTOM");
        entity.setLevel(StrUtil.isNotBlank(bo.getLevel()) ? bo.getLevel() : "WARN");
        entity.setSource("MANUAL");
        entity.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "ENABLED");
        entity.setRemark(bo.getRemark());
        wordMapper.insert(entity);
        log.info("[insertWord] word={}, platformLevel={}, category={}", bo.getWord(), platformLevel, entity.getCategory());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWord(SensitiveWordLibraryBo bo) {
        // ★fail-fast
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("敏感词参数不能为空");
        }
        SensitiveWordLibrary exist = wordMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("敏感词不存在");
        }
        if (StrUtil.isNotBlank(bo.getCategory()) && !VALID_CATEGORIES.contains(bo.getCategory())) {
            throw new ServiceException("敏感词分类不合法：" + bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getLevel()) && !VALID_LEVELS.contains(bo.getLevel())) {
            throw new ServiceException("敏感词级别不合法：" + bo.getLevel());
        }
        SensitiveWordLibrary entity = new SensitiveWordLibrary();
        entity.setId(bo.getId());
        if (StrUtil.isNotBlank(bo.getCategory())) {
            entity.setCategory(bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getLevel())) {
            entity.setLevel(bo.getLevel());
        }
        if (StrUtil.isNotBlank(bo.getStatus())) {
            entity.setStatus(bo.getStatus());
        }
        if (bo.getRemark() != null) {
            entity.setRemark(bo.getRemark());
        }
        log.info("[updateWord] id={}", bo.getId());
        return wordMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWord(Long id) {
        // ★fail-fast
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWordLibrary exist = wordMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("敏感词不存在");
        }
        // ★系统内置敏感词不允许删除
        if ("SYSTEM".equals(exist.getSource())) {
            throw new ServiceException("系统内置敏感词不允许删除");
        }
        log.info("[deleteWord] id={}, word={}", id, exist.getWord());
        return wordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importWords(List<String> words, String category, String level, boolean platformLevel) {
        // ★fail-fast
        if (words == null || words.isEmpty()) {
            throw new ServiceException("导入词列表不能为空");
        }
        if (StrUtil.isBlank(category)) {
            category = "CUSTOM";
        }
        if (!VALID_CATEGORIES.contains(category)) {
            throw new ServiceException("敏感词分类不合法：" + category);
        }
        if (StrUtil.isBlank(level)) {
            level = "WARN";
        }
        if (!VALID_LEVELS.contains(level)) {
            throw new ServiceException("敏感词级别不合法：" + level);
        }
        int count = 0;
        Long tenantId = platformLevel ? null : LoginHelper.getTenantId();
        for (String word : words) {
            if (StrUtil.isBlank(word)) {
                continue;
            }
            word = word.trim();
            if (word.length() > 100) {
                continue;
            }
            // ★幂等：跳过已存在的词
            Long existCount = wordMapper.selectCount(Wrappers.<SensitiveWordLibrary>lambdaQuery()
                .eq(SensitiveWordLibrary::getWord, word)
                .eq(platformLevel, SensitiveWordLibrary::getTenantId, null)
                .eq(!platformLevel, SensitiveWordLibrary::getTenantId, tenantId));
            // ★注意：tenant_id IS NULL 的查询需要特殊处理
            LambdaQueryWrapper<SensitiveWordLibrary> checkWrapper = Wrappers.<SensitiveWordLibrary>lambdaQuery()
                .eq(SensitiveWordLibrary::getWord, word);
            if (platformLevel) {
                checkWrapper.isNull(SensitiveWordLibrary::getTenantId);
            } else {
                checkWrapper.eq(SensitiveWordLibrary::getTenantId, tenantId);
            }
            existCount = wordMapper.selectCount(checkWrapper);
            if (existCount > 0) {
                continue;
            }
            SensitiveWordLibrary entity = new SensitiveWordLibrary();
            entity.setTenantId(tenantId);
            entity.setWord(word);
            entity.setCategory(category);
            entity.setLevel(level);
            entity.setSource("IMPORT");
            entity.setStatus("ENABLED");
            wordMapper.insert(entity);
            count++;
        }
        log.info("[importWords] count={}, platformLevel={}, category={}", count, platformLevel, category);
        return count;
    }

    @Override
    public List<SensitiveWordLibraryVo> getEffectiveWords(String sensitiveWordOverride) {
        // ★fail-fast
        if (StrUtil.isBlank(sensitiveWordOverride)) {
            sensitiveWordOverride = "INHERIT";
        }
        LambdaQueryWrapper<SensitiveWordLibrary> wrapper = Wrappers.<SensitiveWordLibrary>lambdaQuery()
            .eq(SensitiveWordLibrary::getStatus, "ENABLED");

        switch (sensitiveWordOverride) {
            case "INHERIT":
                // 仅平台底库
                wrapper.isNull(SensitiveWordLibrary::getTenantId);
                break;
            case "OVERRIDE":
                // 仅租户自定义
                wrapper.eq(SensitiveWordLibrary::getTenantId, LoginHelper.getTenantId());
                break;
            case "MERGE":
                // 平台底库 + 租户自定义
                Long tenantId = LoginHelper.getTenantId();
                wrapper.and(w -> w.isNull(SensitiveWordLibrary::getTenantId)
                    .or().eq(SensitiveWordLibrary::getTenantId, tenantId));
                break;
            default:
                wrapper.isNull(SensitiveWordLibrary::getTenantId);
                break;
        }
        List<SensitiveWordLibraryVo> list = wordMapper.selectVoList(wrapper);
        // ★DB 返回保护
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
