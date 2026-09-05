package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.TenantTheme;
import com.ruoyi.system.domain.vo.TenantThemeVo;
import com.ruoyi.system.mapper.TenantThemeMapper;
import com.ruoyi.system.service.ITenantThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户主题服务实现（T12.2 / T12.3）
 * <p>tenant_theme 继承 TenantEntity，tenant_id 由多租户插件自动填充与隔离；
 * 活动级主题缺失时回落租户级默认主题（activity_id IS NULL）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantThemeServiceImpl implements ITenantThemeService {

    private final TenantThemeMapper tenantThemeMapper;

    @Override
    public TenantThemeVo getEffectiveTheme(Long activityId) {
        if (activityId != null) {
            TenantThemeVo activityTheme = tenantThemeMapper.selectVoOne(
                Wrappers.<TenantTheme>lambdaQuery().eq(TenantTheme::getActivityId, activityId));
            if (activityTheme != null) {
                return activityTheme;
            }
        }
        // 回落租户级默认主题
        return tenantThemeMapper.selectVoOne(
            Wrappers.<TenantTheme>lambdaQuery().isNull(TenantTheme::getActivityId));
    }

    @Override
    public int saveTheme(TenantThemeVo vo) {
        TenantTheme entity = BeanCopyUtils.copy(vo, TenantTheme.class);
        TenantTheme exist = tenantThemeMapper.selectOne(
            Wrappers.<TenantTheme>lambdaQuery()
                .eq(vo.getActivityId() != null, TenantTheme::getActivityId, vo.getActivityId())
                .isNull(vo.getActivityId() == null, TenantTheme::getActivityId));
        if (exist == null) {
            entity.setId(null);
            return tenantThemeMapper.insert(entity);
        }
        entity.setId(exist.getId());
        return tenantThemeMapper.updateById(entity);
    }
}
