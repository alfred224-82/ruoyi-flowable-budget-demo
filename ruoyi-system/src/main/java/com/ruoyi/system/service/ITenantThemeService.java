package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.TenantThemeVo;

/**
 * 租户主题服务接口（T12.2 Design Token / T12.3 Logo 与海报处理管线）
 * <p>主题解析优先级：活动级主题(activity_id 非空) &gt; 租户级默认主题(activity_id 为空)。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ITenantThemeService {

    /**
     * 解析生效主题：优先取活动级主题，缺失回落租户级默认主题
     *
     * @param activityId 活动ID（可空，空则直接取租户级默认主题）
     * @return 主题视图对象，均无则返回 null
     */
    TenantThemeVo getEffectiveTheme(Long activityId);

    /**
     * 保存（新增或更新）主题：activity_id 为空视为租户级默认主题（每租户唯一）
     *
     * @param vo 主题视图对象
     * @return 影响行数
     */
    int saveTheme(TenantThemeVo vo);
}
