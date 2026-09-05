package com.ruoyi.system.service.image;

/**
 * 图片处理管线抽象（T12.3 Logo/海报处理 + T12.7 异步衍生图）
 * <p>扩展点模式：定义衍生图生成管线，默认提供桩实现（同步生成占位成品 URL）；
 * 接入真实图片处理（OSS 图片处理/自建渲染服务）时新增实现并标注 @Primary 覆盖即可。
 * <p>语义：上传即返回任务ID，成品生成完成后回写 biz_image_task.result_url，
 * 再由主题服务回写 tenant_theme.logo_rendered_url / rendered_poster_url。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IImagePipeline {

    /** 任务类型：Logo 成品（80×80，自动加衬底） */
    String TASK_LOGO_RENDER = "logo_render";
    /** 任务类型：海报列表卡（750×400，叠加渐变遮罩） */
    String TASK_POSTER_CARD = "poster_card";
    /** 任务类型：海报详情（750×1000） */
    String TASK_POSTER_DETAIL = "poster_detail";

    /** 规格：Logo */
    String SPEC_LOGO = "80x80";
    /** 规格：列表卡 */
    String SPEC_CARD = "750x400";
    /** 规格：详情 */
    String SPEC_DETAIL = "750x1000";

    /**
     * 提交衍生图任务
     *
     * @param taskType  任务类型（TASK_*）
     * @param themeId   关联 tenant_theme.id（可空）
     * @param sourceUrl 原图URL
     * @param spec      目标规格（SPEC_*）
     * @return 任务ID（biz_image_task.id）
     */
    Long submit(String taskType, Long themeId, String sourceUrl, String spec);
}
