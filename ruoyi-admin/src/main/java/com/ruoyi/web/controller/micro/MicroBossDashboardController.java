package com.ruoyi.web.controller.micro;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.BossDashboardVo;
import com.ruoyi.system.service.IBossDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 老板经营驾驶舱首页Controller（T18.7，小微版 M18）
 * <p>核心经营数据（今日/本月营收/支出/净利）/ 成本构成（按标签占比）/ 业务待办与异常处理中心入口。
 * <p>★老板（boss）专属。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/bossDashboard")
public class MicroBossDashboardController extends BaseController {

    private final IBossDashboardService dashboardService;

    /**
     * 经营驾驶舱聚合数据（一次请求返回全部）
     */
    @GetMapping
    public R<BossDashboardVo> dashboard() {
        return R.ok(dashboardService.getDashboard());
    }
}
