package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.EmailLoginBody;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.core.domain.model.XcxLoginBody;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.system.service.SysPermissionService;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * 登录验证
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
public class SysLoginController {

    private final SysLoginService loginService;
    private final ISysMenuService menuService;
    private final ISysUserService userService;
    private final ILicenseService licenseService;
    private final ISysTenantService tenantService;
    private final IUserTenantRoleService userTenantRoleService;
    private final SysPermissionService permissionService;

    /** 小微版标准角色优先级（boss > salesman > customer）：user_tenant_role 缺失时按 sys_user_role role_key 回退解析 */
    private static final List<String> MICRO_ROLE_PRIORITY = Arrays.asList("boss", "salesman", "customer");
    /** 无 user_tenant_role 的顾客默认角色编码 */
    private static final String DEFAULT_CUSTOMER_ROLE = "customer";

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginBody loginBody) {
        Map<String, Object> ajax = new HashMap<>();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
            loginBody.getUuid(), loginBody.getTenantId(), loginBody.getOpenid());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * 短信登录
     *
     * @param smsLoginBody 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/smsLogin")
    public R<Map<String, Object>> smsLogin(@Validated @RequestBody SmsLoginBody smsLoginBody) {
        Map<String, Object> ajax = new HashMap<>();
        // 生成令牌
        String token = loginService.smsLogin(smsLoginBody.getPhonenumber(), smsLoginBody.getSmsCode());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * 邮件登录
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/emailLogin")
    public R<Map<String, Object>> emailLogin(@Validated @RequestBody EmailLoginBody body) {
        Map<String, Object> ajax = new HashMap<>();
        // 生成令牌
        String token = loginService.emailLogin(body.getEmail(), body.getEmailCode());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * 小程序登录（T13.1 code2session + 双阶段租户识别）
     *
     * @param xcxLoginBody 小程序登录信息（wx.login code + 选定门店 tenantId）
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/xcxLogin")
    public R<Map<String, Object>> xcxLogin(@Validated @RequestBody XcxLoginBody xcxLoginBody) {
        Map<String, Object> ajax = new HashMap<>();
        // 生成令牌
        String token = loginService.xcxLogin(xcxLoginBody.getXcxCode(), xcxLoginBody.getTenantId());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * 退出登录
     */
    @SaIgnore
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public R<Map<String, Object>> getInfo() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        SysUser user = userService.selectUserById(loginUser.getUserId());
        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("roles", loginUser.getRolePermission());
        // 过期租户管理员仅保留版本升级权限，前端据此只渲染升级按钮
        ajax.put("permissions", menuService.filterPermissionsForExpiredTenant(loginUser.getMenuPermission()));
        // License功能列表（前端据此过滤菜单/路由）
        ajax.put("features", licenseService.getActiveFeatures());
        ajax.put("edition", licenseService.getCurrentEdition());
        // T10.5：下发当前租户业态包编码，前端据此加载 biz_copy 文案映射
        ajax.put("bizCode", licenseService.getCurrentBizCode());
        // 租户名称 + 版本档位：前端按「租户名称+管理系统」拼接控制台标题，按 versionType 区分 lite/pro 首页
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId != null) {
            SysTenantVo tenant = tenantService.queryById(tenantId);
            if (tenant != null) {
                ajax.put("tenantName", tenant.getCompanyName());
                ajax.put("versionType", tenant.getVersionType());
            }
        }
        // ★小微版角色分流：解析当前用户的小微业务角色（boss/salesman/customer），
        //   账号密码/微信登录时前端据此判断 isMicroVersion() 并路由到对应角色首页
        ajax.put("roleCode", resolveMicroRoleCode(user, tenantId));
        return R.ok(ajax);
    }

    /**
     * 解析当前用户的小微业务角色编码（boss/salesman/customer）：
     * 优先取 user_tenant_role（按当前租户过滤），无匹配时回退 sys_user_role 的 role_key 按优先级解析，
     * 两者均无则默认 customer（与 SysLoginService.resolveRoleCode 逻辑一致）。
     */
    private String resolveMicroRoleCode(SysUser user, Long tenantId) {
        if (user == null) {
            return DEFAULT_CUSTOMER_ROLE;
        }
        try {
            List<UserTenantRoleVo> tenantRoles = userTenantRoleService.listByUser(user.getUserId()).stream()
                .filter(r -> !"1".equals(r.getStatus()))
                .collect(Collectors.toList());
            if (tenantId != null) {
                List<UserTenantRoleVo> inTenant = tenantRoles.stream()
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .collect(Collectors.toList());
                if (!inTenant.isEmpty()) {
                    return inTenant.stream()
                        .filter(r -> Integer.valueOf(1).equals(r.getIsPrimary()))
                        .map(UserTenantRoleVo::getRoleCode)
                        .findFirst()
                        .orElse(inTenant.get(0).getRoleCode());
                }
            }
            // 回退 sys_user_role
            Set<String> roleKeys = permissionService.getRolePermission(user);
            if (roleKeys != null) {
                for (String code : MICRO_ROLE_PRIORITY) {
                    if (roleKeys.contains(code)) {
                        return code;
                    }
                }
            }
        } catch (Exception e) {
            // 角色解析失败不阻断登录，fallback customer
        }
        return DEFAULT_CUSTOMER_ROLE;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public R<List<RouterVo>> getRouters() {
        Long userId = LoginHelper.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return R.ok(menuService.buildMenus(menus));
    }
}
