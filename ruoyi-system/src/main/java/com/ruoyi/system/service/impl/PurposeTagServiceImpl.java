package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.PurposeTag;
import com.ruoyi.system.domain.bo.PurposeTagBo;
import com.ruoyi.system.domain.vo.PurposeTagVo;
import com.ruoyi.system.mapper.PurposeTagMapper;
import com.ruoyi.system.service.IPurposeTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * 用途标签Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurposeTagServiceImpl implements IPurposeTagService {

    private final PurposeTagMapper purposeTagMapper;

    @Override
    public PurposeTagVo queryById(Long id) {
        return purposeTagMapper.selectVoById(id);
    }

    @Override
    public PurposeTagVo queryByCode(String tagCode) {
        return purposeTagMapper.selectVoOne(
            Wrappers.<PurposeTag>lambdaQuery()
                .eq(PurposeTag::getTagCode, tagCode));
    }

    @Override
    public Page<PurposeTagVo> selectPageList(PurposeTagBo bo, PageQuery pageQuery) {
        Wrapper<PurposeTag> wrapper = buildQueryWrapper(bo);
        return purposeTagMapper.selectVoPage(pageQuery.build(), wrapper);
    }

    @Override
    public List<PurposeTagVo> queryAllEnabled() {
        List<PurposeTagVo> cached = RedisUtils.getCacheList("fee:purpose_tag:all");
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<PurposeTagVo> list = purposeTagMapper.selectVoList(
            Wrappers.<PurposeTag>lambdaQuery()
                .eq(PurposeTag::getStatus, "ENABLED")
                .orderByAsc(PurposeTag::getSortOrder));
        RedisUtils.setCacheList("fee:purpose_tag:all", list);
        RedisUtils.expire("fee:purpose_tag:all", Duration.ofHours(24));
        return list;
    }

    @Override
    public Boolean insertByBo(PurposeTagBo bo) {
        PurposeTag purposeTag = BeanCopyUtils.copy(bo, PurposeTag.class);
        boolean flag = purposeTagMapper.insert(purposeTag) > 0;
        if (flag) {
            bo.setId(purposeTag.getId());
            // 清除缓存
            RedisUtils.deleteObject("fee:purpose_tag:all");
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PurposeTagBo bo) {
        PurposeTag purposeTag = BeanCopyUtils.copy(bo, PurposeTag.class);
        boolean flag = purposeTagMapper.updateById(purposeTag) > 0;
        if (flag) {
            // 清除缓存
            RedisUtils.deleteObject("fee:purpose_tag:all");
        }
        return flag;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        boolean flag = purposeTagMapper.deleteBatchIds(ids) > 0;
        if (flag) {
            // 清除缓存
            RedisUtils.deleteObject("fee:purpose_tag:all");
        }
        return flag;
    }

    /**
     * 构建查询条件
     */
    private Wrapper<PurposeTag> buildQueryWrapper(PurposeTagBo bo) {
        return Wrappers.<PurposeTag>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getTagCode()), PurposeTag::getTagCode, bo.getTagCode())
            .like(StrUtil.isNotBlank(bo.getTagName()), PurposeTag::getTagName, bo.getTagName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PurposeTag::getStatus, bo.getStatus())
            .orderByAsc(PurposeTag::getSortOrder);
    }
}
