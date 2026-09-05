package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主题预设对象 biz_theme_preset（T12.2 四套 Design Token）
 * <p>系统级共享表，不含 tenant_id（已加入 IGNORE_TENANT_TABLES）；Token 以 CSS 变量下发，PC 与小程序共用，禁止硬编码色值。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_theme_preset")
public class ThemePreset extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主题预设 key（主键）：dark_boardgame/warm_party/fresh_japan/neon_cyber
     */
    @TableId(value = "preset_key", type = IdType.INPUT)
    private String presetKey;

    /**
     * 主题名称
     */
    private String name;

    /**
     * Design Token（主色/辅色/字体/圆角/阴影/渐变，CSS 变量键值 JSON）
     */
    private String tokens;

    /**
     * 主题预览图URL
     */
    private String previewUrl;

    /**
     * 对比度达标标记（AA/AAA）
     */
    private String contrastRatio;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
