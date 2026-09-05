package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.FeeDashboardVo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;
import com.ruoyi.system.service.IFeeBossDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 老板看板Controller（T5.10）
 * 总览 + 大类卡片 + 四层下钻 + 项目卡片 + 风险预警区 + 固定成本卡片 + 未归类池
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/dashboard")
@RequireFeature("FEAT_BOSS_DASHBOARD")
public class FeeBossDashboardController extends BaseController {

    private final IFeeBossDashboardService dashboardService;

    /**
     * 看板总数据（总览/大类/风险/固定成本/未归类池）
     */
    @SaCheckPermission("fee:dashboard:query")
    @GetMapping
    public R<FeeDashboardVo> dashboard() {
        return R.ok(dashboardService.getDashboard());
    }

    /**
     * 项目卡片列表（含利润快照，按实际利润升序突出风险项目）
     */
    @SaCheckPermission("fee:dashboard:query")
    @GetMapping("/projects")
    public R<List<FeeProjectVo>> projects() {
        return R.ok(dashboardService.listProjectCards());
    }

    /**
     * 四层下钻：公司→大类→项目→明细
     *
     * @param level     层级 1公司 2大类 3项目 4明细
     * @param category  会计大类（2层及以下传入）
     * @param projectId 项目ID（3层及以下传入）
     */
    @SaCheckPermission("fee:dashboard:query")
    @GetMapping("/drillDown")
    public R<List<Map<String, Object>>> drillDown(
        @RequestParam Integer level,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Long projectId) {
        return R.ok(dashboardService.drillDown(level, category, projectId));
    }

    /**
     * 团队人力分布卡片：成员跨项目投入比例汇总（超限成员靠前，T7.5 / FEAT_MEMBER_LOAD）
     */
    @RequireFeature("FEAT_MEMBER_LOAD")
    @SaCheckPermission("fee:dashboard:memberLoad")
    @GetMapping("/memberLoad")
    public R<List<FeeMemberLoadVo>> memberLoad() {
        return R.ok(dashboardService.memberLoad());
    }
}
