package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户主题对象 tenant_theme（T1.7 / T12.2 / T12.3）
 * <p>Logo/海报成品与主题预设；activity_id 为 NULL 表示租户级默认主题，非 NULL 表示活动级主题。
 * 主题预设 preset_key 对应系统级 biz_theme_preset 的四套 Design Token（T12.2）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_theme")
public class TenantTheme extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID（NULL=租户级默认主题，非NULL=活动级主题）
     */
    private Long activityId;

    /**
     * 主题预设：dark_boardgame/warm_party/fresh_japan/neon_cyber（T12.2 四套 Design Token）
     */
    private String presetKey;

    /**
     * Logo 原图URL（必须透明底 PNG）
     */
    private String logoUrl;

    /**
     * Logo 成品URL（自动加衬底，输出 80×80px）
     */
    private String logoRenderedUrl;

    /**
     * 海报原图URL
     */
    private String posterUrl;

    /**
     * 海报成品URL（750×400 列表卡 / 750×1000 详情，叠加渐变遮罩）
     */
    private String renderedPosterUrl;

    /**
     * 海报主色 hex（k-means k=3 提取，如 #2B6CFF，供前端卡片配色推导，T15.35§6.3）
     */
    private String posterDominantColor;

    /**
     * 海报辅色 hex（k-means k=3 提取，内容卡片强调色，T15.35§6.3）
     */
    private String posterAccentColor;
}
