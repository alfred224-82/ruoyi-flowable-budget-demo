package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.vo.PageBlockConfigVo;
import com.ruoyi.system.service.IPageBlockConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块化页面运行时Controller（T15.16，小程序页面壳）
 * <p>{@code GET /page/config?pageKey=home}：按 token 中 tenantId + 主标准角色查 page_block_config，
 * 返回有序区块清单（micro、show_flag=1），并做「配置 ∩ 权限」二次过滤（配置了但无权限的区块不返回）。
 * <p>★单一页面壳：小程序 home 页循环渲染 {@code <component is="{{item.blockKey}}" />}，
 * boss/salesman/customer 三角色首页由同一 home 页 + 不同配置渲染，不存在多套页面。
 * <p>仅需登录（任意 mini 角色均调用），无独立 perm；区块组件只增不改，配置永不突破权限。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/page")
public class PageConfigController extends BaseController {

    private final IPageBlockConfigService pageBlockConfigService;

    /**
     * 页面区块配置（小程序页面壳数据源）
     *
     * @param pageKey 页面标识(home/mine/booking/report)
     */
    @SaCheckLogin
    @GetMapping("/config")
    public R<List<PageBlockConfigVo>> config(@RequestParam String pageKey) {
        return R.ok(pageBlockConfigService.getPageConfig(pageKey));
    }
}
