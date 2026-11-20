package com.ruoyi.web.controller.fee;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.FeeLightOverviewVo;
import com.ruoyi.system.service.IFeeBossDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 轻量收支概览Controller（T5.19④，基础版首页标配）
 * <p>基础版标配能力，不设功能开关、不做 @RequireFeature 拦截（登录态全角色可用）；
 * 与老板看板（/fee/dashboard，FEAT_BOSS_DASHBOARD）严格区分：仅三卡片口径，
 * 不含大类下钻/项目利润拆解/风险预警区/固定成本卡片。
 *
 * @author ruoyi
 * @date 2026-09-06
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/overview")
public class FeeLightOverviewController extends BaseController {

    private final IFeeBossDashboardService dashboardService;

    /**
     * 轻量收支概览：本月收入/本月支出/本月结余
     */
    @GetMapping("/light")
    public R<FeeLightOverviewVo> light() {
        return R.ok(dashboardService.getLightOverview());
    }
}
