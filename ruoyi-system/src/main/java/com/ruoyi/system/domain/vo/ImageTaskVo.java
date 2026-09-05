package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 异步衍生图任务视图对象 biz_image_task（T12.7）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ImageTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联 tenant_theme.id */
    private Long themeId;

    /** 任务类型：logo_render/poster_card/poster_detail */
    private String taskType;

    /** 原图URL */
    private String sourceUrl;

    /** 目标规格 */
    private String spec;

    /** 状态：PENDING/PROCESSING/SUCCESS/FAILED */
    private String status;

    /** 成品URL */
    private String resultUrl;

    /** 重试次数 */
    private Integer retryCount;

    /** 失败原因 */
    private String errorMsg;

    private Date createTime;

    private Date updateTime;
}
