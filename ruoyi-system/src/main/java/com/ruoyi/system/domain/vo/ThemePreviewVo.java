package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 主题装修三层融合预览视图对象（T12.5 渲染融合）
 * <p>三层：底层 Design Token（系统级预设色板/字体/圆角）+ 中层租户/活动素材（Logo/海报成品）+
 * 上层内容占位；前端（PC/小程序）按同一 Token 渲染，禁止硬编码色值。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ThemePreviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 生效主题预设 key */
    private String presetKey;

    /** 底层：Design Token（CSS 变量键值） */
    private Map<String, String> tokens = Collections.emptyMap();

    /** 中层：租户/活动素材（Logo/海报成品等） */
    private TenantThemeVo theme;

    /** 素材来源层级：activity(活动级)/tenant(租户级默认)/none(未装修) */
    private String themeLevel;
}
