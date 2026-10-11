package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ImageTask;
import com.ruoyi.system.domain.vo.ThemePreviewVo;
import com.ruoyi.system.domain.vo.TenantThemeVo;
import com.ruoyi.system.mapper.ImageTaskMapper;
import com.ruoyi.system.service.IContentSecurityService;
import com.ruoyi.system.service.ITenantThemeService;
import com.ruoyi.system.service.IThemeDecorationService;
import com.ruoyi.system.service.IThemePresetService;
import com.ruoyi.system.service.image.IImagePipeline;
import com.ruoyi.system.service.image.ImageUploadValidator;
import com.ruoyi.system.service.security.ContentCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 主题装修编排服务实现（T12.3 / T12.4 / T12.5）
 * <p>审核不通过一律拒绝落库；衍生图管线默认桩实现同步产出成品 URL 并回写；
 * 接入真实图片处理/内容安全时，仅需替换对应 Provider（@Primary 覆盖），本编排无需改动。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeDecorationServiceImpl implements IThemeDecorationService {

    private final ITenantThemeService tenantThemeService;
    private final IThemePresetService themePresetService;
    private final IContentSecurityService contentSecurityService;
    private final IImagePipeline imagePipeline;
    private final ImageUploadValidator imageUploadValidator;
    private final ImageTaskMapper imageTaskMapper;

    /** 内容安全场景：Logo */
    private static final String SCENE_LOGO = "logo";
    /** 内容安全场景：海报 */
    private static final String SCENE_POSTER = "poster";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long decorate(TenantThemeVo vo) {
        // 1. 图片四重校验（T15.35 §6.1）：前三重 MIME/尺寸比例/大小（ImageUploadValidator）→ 第四重微信 imgSecCheck；任一不过即拒绝落库
        imageUploadValidator.validate(SCENE_LOGO, vo.getLogoUrl());
        imageUploadValidator.validate(SCENE_POSTER, vo.getPosterUrl());
        checkImage(SCENE_LOGO, vo.getLogoUrl());
        checkImage(SCENE_POSTER, vo.getPosterUrl());

        // 2. 落库 tenant_theme（activity_id 为空视为租户级默认主题）
        tenantThemeService.saveTheme(vo);
        TenantThemeVo saved = tenantThemeService.getEffectiveTheme(vo.getActivityId());
        if (saved == null) {
            throw new ServiceException("主题保存失败");
        }
        Long themeId = saved.getId();

        // 3. 触发衍生图管线并回写成品 URL（桩实现同步产出）
        String logoRendered = submitAndFetch(IImagePipeline.TASK_LOGO_RENDER, themeId,
            vo.getLogoUrl(), IImagePipeline.SPEC_LOGO);
        String posterRendered = submitAndFetch(IImagePipeline.TASK_POSTER_CARD, themeId,
            vo.getPosterUrl(), IImagePipeline.SPEC_CARD);

        if (StrUtil.isNotBlank(logoRendered) || StrUtil.isNotBlank(posterRendered)) {
            saved.setLogoRenderedUrl(StrUtil.isNotBlank(logoRendered) ? logoRendered : saved.getLogoRenderedUrl());
            saved.setRenderedPosterUrl(StrUtil.isNotBlank(posterRendered) ? posterRendered : saved.getRenderedPosterUrl());
            tenantThemeService.saveTheme(saved);
        }
        return themeId;
    }

    @Override
    public ThemePreviewVo preview(Long activityId) {
        ThemePreviewVo preview = new ThemePreviewVo();
        TenantThemeVo theme = tenantThemeService.getEffectiveTheme(activityId);
        preview.setTheme(theme);
        if (theme == null) {
            preview.setThemeLevel("none");
            return preview;
        }
        preview.setThemeLevel(activityId != null && activityId.equals(theme.getActivityId()) ? "activity" : "tenant");
        preview.setPresetKey(theme.getPresetKey());
        preview.setTokens(themePresetService.getTokens(theme.getPresetKey()));
        return preview;
    }

    /**
     * 图片内容安全审核，未通过抛业务异常拦截落库
     */
    private void checkImage(String scene, String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return;
        }
        ContentCheckResult result = contentSecurityService.checkImage(scene, imageUrl);
        if (result != null && !result.isPass()) {
            throw new ServiceException(StrUtil.format("{}素材未通过内容安全审核[{}]，请更换后重试",
                scene, result.getLabel() == null ? result.getResult() : result.getLabel()));
        }
    }

    /**
     * 提交衍生图任务并读取成品 URL（桩实现同步 SUCCESS，真实实现应异步回调后再回写）
     */
    private String submitAndFetch(String taskType, Long themeId, String sourceUrl, String spec) {
        if (StrUtil.isBlank(sourceUrl)) {
            return null;
        }
        Long taskId = imagePipeline.submit(taskType, themeId, sourceUrl, spec);
        if (taskId == null) {
            return null;
        }
        ImageTask task = imageTaskMapper.selectById(taskId);
        return task == null ? null : task.getResultUrl();
    }
}
