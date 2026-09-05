package com.ruoyi.web.controller.portal;

import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.system.domain.bo.PortalBindBo;
import com.ruoyi.system.domain.vo.PortalStoreVo;
import com.ruoyi.system.domain.vo.PortalTenantVo;
import com.ruoyi.system.service.IPortalService;
import com.ruoyi.system.service.IUserTenantBindService;
import com.ruoyi.system.service.wecom.IWecomIntegration;
import com.ruoyi.system.service.wecom.WecomUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 引导态门户Controller（T13.1 公众号总入口 / T13.2 企微OAuth / T13.4 端适配 / T8.8 限流审计 / T8.10 门店搜索 / T13.1 引导态绑定）
 * <p>路径 /portal/** 已加入安全白名单（application.yml security.excludes），无需登录；
 * 【严禁】在此写 TenantContext / 签发会话——做只读门店反查、OAuth 引导与引导态 openid→门店绑定落库，选定门店后走正常登录进入鉴权态。
 * <p>T8.8：全端点独立限流（按来源IP，防篡改 ?tenant= 探测与批量爬取）+ 审计日志（服务层写 portal_access_log）；
 * 只返回公开展示字段，篡改参数仅能看到别家公开首页，登录后数据仍限本租户；绑定关系不授予任何权限。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal")
public class PortalController extends BaseController {

    private final IPortalService portalService;
    private final IWecomIntegration wecomIntegration;
    private final IUserTenantBindService userTenantBindService;

    /**
     * 按 openid 反查可进入的门店列表（登录页显式选定，不自动猜测租户）
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/tenants")
    public R<List<PortalTenantVo>> tenants(@RequestParam String openid) {
        return R.ok(portalService.listTenantsByOpenid(openid));
    }

    /**
     * 引导态绑定：用户显式选定门店后，记录 openid→门店关联（userId 空，登录后由 /login 回填）。
     * <p>供下次引导态 {@link #tenants} 按 openid 快速展示已绑定门店；【严禁】写 TenantContext / 签发会话，
     * 绑定关系不授予任何权限，登录仍须账号密码 + 门店归属校验。
     * <p>依赖 code2session 获取真实 openid；桩阶段前端可暂不调用。
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/tenant/bind")
    public R<Void> bind(@Validated @RequestBody PortalBindBo bo) {
        userTenantBindService.bind(bo.getOpenid(), bo.getTenantId(), null, bo.getIsDefault());
        return R.ok();
    }

    /**
     * 公众号总入口：按公众号 AppID 解析归属主体（一个公众号对应一个租户）
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/tenant/resolve")
    public R<PortalTenantVo> resolve(@RequestParam String mpAppId) {
        return R.ok(portalService.resolveByMpAppId(mpAppId));
    }

    /**
     * 门店搜索（T8.10 门店选择/搜索首页）：关键字 + 城市过滤，只返回公开展示字段；
     * 传入用户经纬度则按距离就近排序（定位授权），否则按名称排序（定位拒绝时降级为城市选择）。
     * <p>共用小程序首页不可空白：无关键字时返回城市/全量门店列表兜底。
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/stores")
    public R<List<PortalStoreVo>> stores(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) BigDecimal longitude,
                                         @RequestParam(required = false) BigDecimal latitude,
                                         @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return R.ok(portalService.searchStores(keyword, city, longitude, latitude, limit == null ? 50 : limit));
    }

    /**
     * 企微自建应用网页授权链接（引导用户跳转授权，桩实现按企微标准拼接）
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/wecom/authUrl")
    public R<String> wecomAuthUrl(@RequestParam String corpId,
                                  @RequestParam(required = false) String agentId,
                                  @RequestParam String redirectUri,
                                  @RequestParam(required = false) String state) {
        return R.ok(wecomIntegration.buildAuthUrl(corpId, agentId, redirectUri, state));
    }

    /**
     * 企微 OAuth 回调：以 code 换取企微用户身份（桩实现返回模拟成员）
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/wecom/userInfo")
    public R<WecomUserInfo> wecomUserInfo(@RequestParam String corpId, @RequestParam String code) {
        return R.ok(wecomIntegration.getUserInfo(corpId, code));
    }
}
