package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SensitiveWord;
import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.domain.bo.SensitiveWordBo;
import com.ruoyi.system.domain.vo.SensitiveWordHitLogVo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;
import com.ruoyi.system.mapper.SensitiveWordHitLogMapper;
import com.ruoyi.system.mapper.SensitiveWordMapper;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.ISensitiveWordTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 租户合规管理服务实现（M18 T18.6）
 * <p>租户自定义词 tenant_id=当前租户、source=tenant；写操作后刷新引擎该租户词缓存。
 * <p>命中记录走标准租户隔离（拦截器自动过滤 tenant_id=当前）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordTenantServiceImpl implements ISensitiveWordTenantService {

    private final SensitiveWordMapper wordMapper;
    private final SensitiveWordHitLogMapper hitLogMapper;
    private final ISensitiveWordEngine sensitiveWordEngine;

    /** 全局底库租户键 */
    private static final long GLOBAL_TENANT = 0L;

    private static final Set<String> VALID_CATEGORIES = new HashSet<>(Arrays.asList(
        "POLITICAL", "PORN", "VIOLENCE", "AD", "LAW", "SUPERSTITION", "ABUSE", "RUMOR", "DIALECT", "CUSTOM"));

    private static final Set<String> VALID_MATCH_MODES = new HashSet<>(Arrays.asList("exact", "fuzzy", "regex"));

    /**
     * 当前租户ID（fail-fast）。
     */
    private Long currentTenantId() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        return tenantId;
    }

    @Override
    public TableDataInfo<SensitiveWordVo> queryTenantWordPage(SensitiveWordBo bo, PageQuery pageQuery) {
        Long tenantId = currentTenantId();
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        LambdaQueryWrapper<SensitiveWord> wrapper = Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, tenantId);
        applyWordFilter(wrapper, bo);
        wrapper.orderByDesc(SensitiveWord::getCreateTime);
        Page<SensitiveWordVo> page = wordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<SensitiveWordVo> queryGlobalWordPage(SensitiveWordBo bo, PageQuery pageQuery) {
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        // 全局底库只读（tenant_id=0）
        LambdaQueryWrapper<SensitiveWord> wrapper = Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, GLOBAL_TENANT);
        applyWordFilter(wrapper, bo);
        wrapper.orderByAsc(SensitiveWord::getCategory).orderByDesc(SensitiveWord::getCreateTime);
        Page<SensitiveWordVo> page = wordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    private void applyWordFilter(LambdaQueryWrapper<SensitiveWord> wrapper, SensitiveWordBo bo) {
        if (bo == null) {
            return;
        }
        if (StrUtil.isNotBlank(bo.getWord())) {
            wrapper.like(SensitiveWord::getWord, bo.getWord());
        }
        if (StrUtil.isNotBlank(bo.getCategory())) {
            wrapper.eq(SensitiveWord::getCategory, bo.getCategory());
        }
        if (StrUtil.isNotBlank(bo.getStatus())) {
            wrapper.eq(SensitiveWord::getStatus, bo.getStatus());
        }
    }

    @Override
    public SensitiveWordVo queryWordById(Long id) {
        Long tenantId = currentTenantId();
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWord exist = wordMapper.selectById(id);
        if (exist == null || exist.getTenantId() == null || exist.getTenantId().longValue() != tenantId.longValue()) {
            throw new ServiceException("自定义敏感词不存在");
        }
        return wordMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertWord(SensitiveWordBo bo) {
        Long tenantId = currentTenantId();
        validateBo(bo, true);
        Long exist = wordMapper.selectCount(Wrappers.<SensitiveWord>lambdaQuery()
            .eq(SensitiveWord::getTenantId, tenantId)
            .eq(SensitiveWord::getWord, bo.getWord())
            .eq(SensitiveWord::getCategory, bo.getCategory()));
        if (exist != null && exist > 0) {
            throw new ServiceException("该敏感词已存在，请勿重复添加");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setTenantId(tenantId);
        entity.setWord(bo.getWord());
        entity.setCategory(bo.getCategory());
        entity.setMatchMode(StrUtil.blankToDefault(bo.getMatchMode(), "exact"));
        entity.setIndustryScope(bo.getIndustryScope());
        entity.setStatus(StrUtil.blankToDefault(bo.getStatus(), "0"));
        entity.setSource("tenant");
        entity.setRemark(bo.getRemark());
        wordMapper.insert(entity);
        sensitiveWordEngine.evictWordCache(tenantId);
        log.info("[tenant.insertWord] tenantId={}, word={}, category={}", tenantId, entity.getWord(), entity.getCategory());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWord(SensitiveWordBo bo) {
        Long tenantId = currentTenantId();
        validateBo(bo, false);
        SensitiveWord exist = wordMapper.selectById(bo.getId());
        if (exist == null || exist.getTenantId() == null || exist.getTenantId().longValue() != tenantId.longValue()) {
            throw new ServiceException("自定义敏感词不存在");
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
        sensitiveWordEngine.evictWordCache(tenantId);
        log.info("[tenant.updateWord] tenantId={}, id={}", tenantId, bo.getId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWord(Long id) {
        Long tenantId = currentTenantId();
        if (id == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        SensitiveWord exist = wordMapper.selectById(id);
        if (exist == null || exist.getTenantId() == null || exist.getTenantId().longValue() != tenantId.longValue()) {
            throw new ServiceException("自定义敏感词不存在");
        }
        int rows = wordMapper.deleteById(id);
        sensitiveWordEngine.evictWordCache(tenantId);
        log.info("[tenant.deleteWord] tenantId={}, id={}, word={}", tenantId, id, exist.getWord());
        return rows;
    }

    @Override
    public TableDataInfo<SensitiveWordHitLogVo> queryHitLogPage(SensitiveWordHitLogVo vo, PageQuery pageQuery) {
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        // sensitive_word_hit_log 走标准租户隔离，拦截器自动追加 tenant_id=当前，无需手工过滤
        LambdaQueryWrapper<SensitiveWordHitLog> wrapper = Wrappers.<SensitiveWordHitLog>lambdaQuery();
        if (vo != null) {
            if (StrUtil.isNotBlank(vo.getInputChannel())) {
                wrapper.eq(SensitiveWordHitLog::getInputChannel, vo.getInputChannel());
            }
            if (StrUtil.isNotBlank(vo.getResult())) {
                wrapper.eq(SensitiveWordHitLog::getResult, vo.getResult());
            }
            if (StrUtil.isNotBlank(vo.getHitCategory())) {
                wrapper.eq(SensitiveWordHitLog::getHitCategory, vo.getHitCategory());
            }
            if (StrUtil.isNotBlank(vo.getBizType())) {
                wrapper.eq(SensitiveWordHitLog::getBizType, vo.getBizType());
            }
        }
        wrapper.orderByDesc(SensitiveWordHitLog::getCreateTime);
        Page<SensitiveWordHitLogVo> page = hitLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

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
