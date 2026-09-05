package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户主题视图对象 tenant_theme（T12.2 / T12.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class TenantThemeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 活动ID（NULL=租户级默认主题） */
    private Long activityId;

    /** 主题预设 key */
    private String presetKey;

    /** Logo 原图URL */
    private String logoUrl;

    /** Logo 成品URL */
    private String logoRenderedUrl;

    /** 海报原图URL */
    private String posterUrl;

    /** 海报成品URL */
    private String renderedPosterUrl;

    private Date createTime;

    private Date updateTime;
}
