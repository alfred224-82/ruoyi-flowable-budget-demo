package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ThemePreviewVo;
import com.ruoyi.system.domain.vo.TenantThemeVo;

/**
 * 主题装修编排服务接口（T12.3 素材管线 / T12.4 内容安全 / T12.5 三层融合）
 * <p>保存装修素材时统一编排：内容安全审核（imgSecCheck 桩）→ 落库 tenant_theme →
 * 提交图片处理管线（Logo 衬底/海报遮罩衍生图）→ 回写成品 URL；预览时融合 Design Token 与素材。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IThemeDecorationService {

    /**
     * 装修保存编排：审核 → 落库 → 触发衍生图管线 → 回写成品 URL
     *
     * @param vo 主题素材（activity_id 为空视为租户级默认主题）
     * @return 主题ID（tenant_theme.id）
     */
    Long decorate(TenantThemeVo vo);

    /**
     * 三层融合预览：底层 Design Token + 中层租户/活动素材（活动级优先，缺失回落租户级）
     *
     * @param activityId 活动ID（可空）
     * @return 融合预览
     */
    ThemePreviewVo preview(Long activityId);
}
