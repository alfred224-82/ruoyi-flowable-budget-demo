package com.ruoyi.web.controller.micro;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.TenantOverviewVo;
import com.ruoyi.system.service.ITenantOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理员企业概览首页Controller（T18.6，小微版 M18）
 * <p>替换 platform_admin 默认首页为企业概览：
 * <ul>
 *   <li>企业基本信息（含 License 有效期与状态）</li>
 *   <li>配置健康度自检（原子数据完整度+周期规则+合规状态，红/黄/绿三态）</li>
 *   <li>系统公告</li>
 *   <li>快捷入口</li>
 * </ul>
 * <p>★租户管理员（platform_admin）专属，不含业务待办。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/tenantOverview")
public class MicroTenantOverviewController extends BaseController {

    private final ITenantOverviewService overviewService;

    /**
     * 企业概览聚合数据（一次请求返回全部）
     */
    @GetMapping
    public R<TenantOverviewVo> overview() {
        return R.ok(overviewService.getOverview());
    }
}
