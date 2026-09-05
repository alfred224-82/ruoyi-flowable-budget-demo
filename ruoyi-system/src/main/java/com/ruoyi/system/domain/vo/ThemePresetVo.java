package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 主题预设视图对象 biz_theme_preset（T12.2）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ThemePresetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主题预设 key */
    private String presetKey;

    /** 主题名称 */
    private String name;

    /** Design Token（CSS 变量键值 JSON） */
    private String tokens;

    /** 主题预览图URL */
    private String previewUrl;

    /** 对比度达标标记 */
    private String contrastRatio;

    /** 排序 */
    private Integer sort;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;
}
