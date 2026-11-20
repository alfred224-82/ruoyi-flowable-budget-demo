package com.ruoyi.web.controller.micro;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.AdminDashboardVo;
import com.ruoyi.system.service.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 平台驾驶舱首页Controller（T18.5，小微版 M18）
 * <p>替换 Admin 默认首页为平台驾驶舱：
 * <ul>
 *   <li>租户大盘（总数/活跃度/行业分布柱状图）</li>
 *   <li>系统健康度（服务状态/到期预警/禁用统计）</li>
 *   <li>全局合规风险监控（近 7 天命中趋势）</li>
 * </ul>
 * <p>★Admin 专属，聚合接口跨租户查询。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/dashboard")
public class MicroAdminDashboardController extends BaseController {

    private final IAdminDashboardService dashboardService;

    /**
     * 驾驶舱聚合数据（一次请求返回全部面板数据）
     */
    @GetMapping
    public R<AdminDashboardVo> dashboard() {
        return R.ok(dashboardService.getDashboard());
    }
}
