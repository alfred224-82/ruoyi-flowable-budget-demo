package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.MicroLoginBody;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.MicroSceneBo;
import com.ruoyi.system.domain.vo.MicroLoginVo;
import com.ruoyi.system.domain.vo.MicroSceneVo;
import com.ruoyi.system.domain.vo.MicroTenantOptionVo;
import com.ruoyi.system.domain.vo.PortalStoreVo;
import com.ruoyi.system.service.IPortalService;
import com.ruoyi.system.service.SysLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手机号统一登录Controller（T15.8）
 * <p>PC 短信验证码登录 / 小程序 wx.login + getPhoneNumber 一键登录共用 {@code POST /micro/login} 入口。
 * <p>登录流程：① 手机号 → sys_user（不存在自动注册）；② 解析 tenantId（场景带入 / 用户选择，跨多租户返回候选引导选择页）；
 * ③ 查 user_tenant_role → role_code（顾客无角色默认 customer，支持免密轻量浏览）；④ 按 client_type 过滤菜单；
 * ⑤ 返回 token + perms + 菜单树。
 * <p>★全程无 appid 业务判断：请求体无 appid 字段，client_type 由前端壳明确传入作为菜单端过滤唯一依据，
 * appid/登录渠道校验在网关/拦截器单独完成，不参与身份权限判定。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro")
public class MicroLoginController {

    private final SysLoginService loginService;
    private final IPortalService portalService;

    /**
     * 手机号统一登录
     *
     * @param body 手机号登录请求体（phonenumber + smsCode/xcxCode + 可选 tenantId + clientType，无 appid）
     * @return 登录结果：needSelectTenant=true 时返回租户候选（token 为空，前端弹选择页）；否则返回 token + perms + 菜单树
     */
    @SaIgnore
    @PostMapping("/login")
    public R<MicroLoginVo> login(@Validated @RequestBody MicroLoginBody body) {
        return R.ok(loginService.phoneLogin(body));
    }

    /**
     * 我的店铺（多租户切换，T15.36§6.2）：查询当前登录用户可切换的租户列表
     * <p>登录态访问；返回 user_tenant_role 归属的全部启用租户（门店名/角色/档位/行业），
     * 供小程序「我的 → 切换店铺」与 {@code pages/micro/tenant/select.vue} 消费。
     *
     * @return 租户候选列表
     */
    @GetMapping("/tenant/myTenants")
    public R<List<MicroTenantOptionVo>> myTenants() {
        return R.ok(loginService.listMyTenants(LoginHelper.getUserId()));
    }

    /**
     * T29.2 按租户号查询门店列表（Boss 手动输租户号登录，匿名端点）
     * <p>登录前调用，校验租户号合法性（存在/未停用/未过期）后返回门店公开信息；
     * 非法租户号抛业务异常，前端提示「租户不存在或已停用」。供 {@code pages/micro/tenant/input.vue} 消费。
     *
     * @param tenantNo 平台提供的租户号（sys_tenant.tenant_code）
     * @return 门店公开列表
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/tenant/stores")
    public R<List<PortalStoreVo>> tenantStores(@RequestParam String tenantNo) {
        return R.ok(portalService.listStoresByTenantNo(tenantNo));
    }

    /**
     * T29.2 租户号合法性校验（匿名端点）：命中且状态正常/未到期返回门店公开信息，否则抛业务异常。
     *
     * @param tenantNo 平台提供的租户号（sys_tenant.tenant_code）
     * @return 门店公开信息
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/tenant/resolveNo")
    public R<PortalStoreVo> resolveNo(@RequestParam String tenantNo) {
        return R.ok(portalService.resolveTenantNo(tenantNo));
    }

    /**
     * T29.3 扫码 scene 解析 + 校验（员工/顾客扫码带租户直接登录，匿名端点）
     * <p>scene 编码 {@code t={tenantId}&s={storeId}&r={role}}；校验租户存在/未停用/未过期、门店归属该租户，
     * 通过后返回门店上下文，前端据此跳过租户号输入 + 门店选择直接进入登录页；非法抛业务异常。
     *
     * @param bo scene 请求体
     * @return scene 解析结果（tenantId/storeId/role + 门店公开信息）
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/login/resolveScene")
    public R<MicroSceneVo> resolveScene(@Validated @RequestBody MicroSceneBo bo) {
        return R.ok(portalService.resolveScene(bo.getScene()));
    }
}
