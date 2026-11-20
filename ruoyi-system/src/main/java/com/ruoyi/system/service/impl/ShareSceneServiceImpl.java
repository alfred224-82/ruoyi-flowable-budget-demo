package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.OssService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.ShareScene;
import com.ruoyi.system.domain.bo.ShareSceneBo;
import com.ruoyi.system.domain.vo.ShareSceneVo;
import com.ruoyi.system.mapper.ShareSceneMapper;
import com.ruoyi.system.service.IShareSceneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;

/**
 * 小程序场景/分享服务实现（T15.3/T15.19/T15.24）
 * <p>share_scene 租户级表；但 scene_id 全局唯一（uk_scene），扫码进入时尚未知晓租户，
 * 故 {@link #resolveBySceneId} 走 Mapper 的 {@code @InterceptorIgnore(tenantLine=true)} 跨租户反查。
 * <p>管理端（save/queryPageList）仍受租户插件隔离，仅操作本租户场景。
 * <p>★scene_id 全局唯一由调用方（T15.24 生成海报）保证——建议以「租户号 + 随机串」构造，避免跨租户碰撞。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareSceneServiceImpl implements IShareSceneService {

    private final ShareSceneMapper shareSceneMapper;

    /** OSS 服务：qrCodeUrl 存 ossId 时服务端翻译为可渲染 URL */
    private final OssService ossService;

    @Override
    public ShareSceneVo resolveBySceneId(String sceneId) {
        if (StrUtil.isBlank(sceneId)) {
            return null;
        }
        // 跨租户反查（扫码/分享进入锁定租户）
        ShareScene e = shareSceneMapper.selectBySceneIdIgnoreTenant(sceneId);
        if (e == null) {
            return null;
        }
        if (e.getExpireTime() != null && e.getExpireTime().before(new Date())) {
            throw new ServiceException("该分享场景已过期");
        }
        ShareSceneVo vo = BeanCopyUtils.copy(e, ShareSceneVo.class);
        vo.setQrCodeUrl(resolveOssUrls(vo.getQrCodeUrl()));
        return vo;
    }

    @Override
    public Long save(ShareSceneBo bo) {
        if (StrUtil.isBlank(bo.getSceneId())) {
            throw new ServiceException("场景标识不能为空");
        }
        // 本租户内按 sceneId 幂等 upsert
        ShareScene exist = shareSceneMapper.selectOne(Wrappers.<ShareScene>lambdaQuery()
            .eq(ShareScene::getSceneId, bo.getSceneId())
            .last("LIMIT 1"));
        ShareScene e = BeanCopyUtils.copy(bo, ShareScene.class);
        if (exist != null) {
            e.setId(exist.getId());
            shareSceneMapper.updateById(e);
            return exist.getId();
        }
        e.setId(null);
        e.setCreateBy(safeUsername());
        shareSceneMapper.insert(e);
        return e.getId();
    }

    @Override
    public TableDataInfo<ShareSceneVo> queryPageList(ShareSceneBo bo, PageQuery pageQuery) {
        Page<ShareSceneVo> page = shareSceneMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        // ★qrCodeUrl 库内可能存 ossId：服务端统一翻译，PC 列表 image-preview 直接渲染
        page.getRecords().forEach(vo -> vo.setQrCodeUrl(resolveOssUrls(vo.getQrCodeUrl())));
        return TableDataInfo.build(page);
    }

    @Override
    public ShareSceneVo queryById(Long id) {
        ShareSceneVo vo = shareSceneMapper.selectVoById(id);
        if (vo != null) {
            vo.setQrCodeUrl(resolveOssUrls(vo.getQrCodeUrl()));
        }
        return vo;
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return shareSceneMapper.deleteBatchIds(ids);
    }

    /**
     * 逗号分隔 ossId 串翻译为完整 URL 串；仅纯数字 token 才翻译，已是 URL 原样返回；翻译失败 fail-soft 返回原值
     */
    private String resolveOssUrls(String ossIds) {
        if (StrUtil.isBlank(ossIds)) {
            return ossIds;
        }
        for (String token : ossIds.split(",")) {
            if (!token.trim().matches("\\d+")) {
                return ossIds;
            }
        }
        try {
            return ossService.selectUrlByIds(ossIds);
        } catch (Exception e) {
            log.warn("OSS url 翻译失败，ossIds={}", ossIds, e);
            return ossIds;
        }
    }

    /**
     * 构建查询条件（sceneId/bizType/bizId 精确，id 倒序）
     */
    private LambdaQueryWrapper<ShareScene> buildWrapper(ShareSceneBo bo) {
        return Wrappers.<ShareScene>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getSceneId()), ShareScene::getSceneId, bo.getSceneId())
            .eq(StrUtil.isNotBlank(bo.getBizType()), ShareScene::getBizType, bo.getBizType())
            .eq(bo.getBizId() != null, ShareScene::getBizId, bo.getBizId())
            .orderByDesc(ShareScene::getId);
    }

    /**
     * 安全获取当前登录用户名（无登录态返回 null，供系统调用兜底）
     */
    private String safeUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
