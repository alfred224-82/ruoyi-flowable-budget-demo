package com.ruoyi.system.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.event.LogininforEvent;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.MicroLoginBody;
import com.ruoyi.common.core.domain.model.XcxLoginUser;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.LoginType;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.MicroLoginVo;
import com.ruoyi.system.domain.vo.MicroTenantOptionVo;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.domain.vo.UserTenantBindVo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.wx.IWxMiniappService;
import com.ruoyi.system.service.wx.WxCode2SessionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 登录校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginService {

    /** 客户端类型：小程序（T15.8，前端壳传入，非 appid 推导） */
    private static final String CLIENT_MINI = "mini";
    /** 客户端类型：PC */
    private static final String CLIENT_PC = "pc";
    /** 无 user_tenant_role 的顾客默认角色编码（轻量档案免密浏览，T15.8） */
    private static final String DEFAULT_CUSTOMER_ROLE = "customer";
    /** 小微版标准角色优先级（boss > salesman > customer）：user_tenant_role 缺失时按 sys_user_role role_key 回退解析 */
    private static final List<String> MICRO_ROLE_PRIORITY = Arrays.asList("boss", "salesman", "customer");

    /** 顾客全局共享角色 ID（M15_07 创建，tenant_id=NULL，T15.8 免密登录默认角色） */
    private static final long CUSTOMER_ROLE_ID = 9207L;

    private final SysUserMapper userMapper;
    private final ISysConfigService configService;
    private final SysPermissionService permissionService;
    private final TenantStatusChecker tenantStatusChecker;
    private final IUserTenantBindService userTenantBindService;
    private final IWxMiniappService wxMiniappService;
    private final IUserTenantRoleService userTenantRoleService;
    private final ISysMenuService menuService;
    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Value("${user.password.maxRetryCount}")
    private Integer maxRetryCount;

    @Value("${user.password.lockTime}")
    private Integer lockTime;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @param tenantId 选定门店租户ID（可选）：非空时校验账号归属，防跨门店串号登录（T13.1）
     * @param openid   微信 openid（可选）：非空时登录后回填 user_tenant_bind.userId（T13.1 第二阶段）
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid, Long tenantId, String openid) {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        // 验证码开关
        if (captchaEnabled) {
            validateCaptcha(username, code, uuid);
        }
        // PC 端登录租户ID强制校验：admin 超管可留空（全局唯一用户名，无需租户隔离），
        // 其他所有用户必须填写 tenantId 以区分租户（tenantId 由平台管理员分配）
        if (tenantId == null && !"admin".equals(username)) {
            log.info("登录用户：{} 未填写租户ID.", username);
            recordLogininfor(username, Constants.LOGIN_FAIL, "未填写租户ID");
            throw new ServiceException("请填写租户ID（admin可留空，其他用户必填，向平台管理员获取）");
        }
        SysUser user = loadUserByUsername(username, tenantId);
        // T13.1 门店约束校验：引导态显式选定门店后，账号必须归属该门店，防跨租户串号登录；
        // tenantId 为空（PC 端/未选店）则维持原行为，按全局唯一用户名登录
        if (tenantId != null && !tenantId.equals(user.getTenantId())) {
            log.info("登录用户：{} 不属于所选门店 {}.", username, tenantId);
            recordLogininfor(username, Constants.LOGIN_FAIL, "账号不属于所选门店");
            throw new ServiceException("该账号不属于所选门店，请重新选择门店或使用对应门店的账号登录");
        }
        checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, user.getPassword()));
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        LoginUser loginUser = buildLoginUser(user);
        // 生成token
        LoginHelper.loginByDevice(loginUser, DeviceType.PC);

        // T13.1 第二阶段：登录成功后回填 openid→userId 绑定（openid 依赖 code2session，缺省则跳过；失败不阻断登录）
        if (StringUtils.isNotBlank(openid)) {
            try {
                userTenantBindService.fillUserId(openid, user.getTenantId(), user.getUserId());
            } catch (Exception e) {
                log.warn("回填用户租户绑定失败 openid={}, userId={}", openid, user.getUserId(), e);
            }
        }

        recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), username);
        return StpUtil.getTokenValue();
    }

    public String smsLogin(String phonenumber, String smsCode) {
        // 通过手机号查找用户
        SysUser user = loadUserByPhonenumber(phonenumber);

        checkLogin(LoginType.SMS, user.getUserName(), () -> !validateSmsCode(phonenumber, smsCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        LoginUser loginUser = buildLoginUser(user);
        // 生成token
        LoginHelper.loginByDevice(loginUser, DeviceType.APP);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    public String emailLogin(String email, String emailCode) {
        // 通过手机号查找用户
        SysUser user = loadUserByEmail(email);

        checkLogin(LoginType.EMAIL, user.getUserName(), () -> !validateEmailCode(email, emailCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        LoginUser loginUser = buildLoginUser(user);
        // 生成token
        LoginHelper.loginByDevice(loginUser, DeviceType.APP);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    /**
     * 小程序登录（T13.1 code2session + 双阶段租户识别）
     *
     * @param xcxCode  wx.login 返回的临时登录凭证 code
     * @param tenantId 选定门店租户ID（必填）：按 openid+tenantId 查绑定用户，禁止自动猜测租户
     * @return 结果
     */
    public String xcxLogin(String xcxCode, Long tenantId) {
        // code2session：以 code 换取真实 openid（未配置 wx.miniapp.appid/secret 时抛 ServiceException）
        WxCode2SessionResult session = wxMiniappService.code2session(xcxCode);
        String openid = session.getOpenid();
        // 双阶段租户识别：按 openid + 选定门店查已绑定用户，未绑定则引导先用账号密码登录回填
        SysUser user = loadUserByOpenid(openid, tenantId);
        // T3.4 租户到期/停用拦截：到期租户禁止登录（防"过期续用"），租户管理员豁免
        if (ObjectUtil.isNotNull(user.getTenantId())) {
            tenantStatusChecker.checkTenant(user.getTenantId(), user.getUserId());
        }
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        XcxLoginUser loginUser = new XcxLoginUser();
        populateLoginUser(loginUser, user);
        loginUser.setOpenid(openid);
        // 生成token
        LoginHelper.loginByDevice(loginUser, DeviceType.XCX);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    /**
     * 手机号统一登录（T15.8）
     * <p>① 手机号 → sys_user（不存在自动注册，手机号为唯一身份锚点）；② 解析 tenantId（场景带入 / 用户选择，
     * 跨多租户且未指定门店时返回租户候选引导选择）；③ 查 user_tenant_role → role_code（无角色的顾客默认 customer，
     * 支持免密轻量浏览）；④ 按 client_type（前端壳传入，非 appid 推导）过滤菜单；⑤ 返回 token + perms + 菜单树。
     * <p>★方法签名无 appid 参数：appid/登录渠道不参与身份权限判定，其校验在网关/拦截器单独完成。
     *
     * @param body 手机号登录请求体
     * @return 登录结果（需选租户时 needSelectTenant=true 且不下发 token）
     */
    public MicroLoginVo phoneLogin(MicroLoginBody body) {
        String phonenumber = body.getPhonenumber();
        String clientType = body.getClientType();

        // ① 小程序一键登录：前端传 phoneCode（wx.getPhoneNumber 加密 code），需解密为真实手机号
        if (StringUtils.isBlank(phonenumber) && CLIENT_MINI.equalsIgnoreCase(clientType)) {
            if (StringUtils.isBlank(body.getPhoneCode())) {
                throw new ServiceException("手机号不能为空");
            }
            try {
                phonenumber = wxMiniappService.getPhoneNumber(body.getPhoneCode());
                log.info("[MicroLogin] phoneCode 解密成功 phone={}", phonenumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("[MicroLogin] phoneCode 解密失败", e);
                throw new ServiceException("手机号解密失败，请重试");
            }
        }
        final String finalPhonenumber = phonenumber;

        // ② 校验：PC/both 走短信验证码；mini 由 wx.getPhoneNumber 完成手机号校验（本接口无 appid 业务判断）
        if (!CLIENT_MINI.equalsIgnoreCase(clientType)) {
            checkLogin(LoginType.SMS, finalPhonenumber, () -> !validateSmsCode(finalPhonenumber, body.getSmsCode()));
        }

        // ③ 手机号 → sys_user，不存在则自动注册
        // ★租户隔离：sys_user 在多租户插件忽略表中，同手机号可能跨租户存在多条，必须带 tenant_id 精确匹配，
        // 避免 selectOne 命中多条抛 TooManyResultsException（tenant_id 由场景带入，登录时必然存在）。
        Long reqTenantId = body.getTenantId();
        SysUser user = userMapper.selectUserByPhonenumberAndTenant(phonenumber, reqTenantId);
        if (ObjectUtil.isNull(user)) {
            user = registerByPhone(phonenumber, reqTenantId);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", phonenumber);
            throw new UserException("user.blocked", phonenumber);
        }

        // ③ 查 user_tenant_role（登录态无租户上下文，跨租户返回该手机号全部启用角色）
        List<UserTenantRoleVo> tenantRoles = userTenantRoleService.listByUser(user.getUserId()).stream()
            .filter(r -> !"1".equals(r.getStatus()))
            .collect(Collectors.toList());

        // ③a 解析 tenantId：未指定且跨多租户 → 返回租户候选，引导前端弹选择页（不下发 token）
        Long tenantId = body.getTenantId();
        if (tenantId == null) {
            Set<Long> candidateIds = tenantRoles.stream()
                .map(UserTenantRoleVo::getTenantId)
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (candidateIds.isEmpty() && ObjectUtil.isNotNull(user.getTenantId())) {
                candidateIds.add(user.getTenantId());
            }
            if (candidateIds.size() > 1) {
                return buildTenantSelectVo(candidateIds, tenantRoles);
            }
            tenantId = candidateIds.isEmpty() ? user.getTenantId() : candidateIds.iterator().next();
        }

        // ③b 选定租户状态校验（到期/停用禁止登录，租户管理员豁免）+ 解析该租户下主角色 role_code
        if (ObjectUtil.isNotNull(tenantId)) {
            tenantStatusChecker.checkTenant(tenantId, user.getUserId());
        }
        String roleCode = resolveRoleCode(tenantRoles, tenantId, user);

        // ④ 构建登录用户，以选定租户登录（覆盖 home 租户，支持一人多店）
        LoginUser loginUser = new LoginUser();
        populateLoginUser(loginUser, user);
        if (ObjectUtil.isNotNull(tenantId)) {
            loginUser.setTenantId(tenantId);
        }
        DeviceType deviceType = CLIENT_MINI.equalsIgnoreCase(clientType) ? DeviceType.XCX
            : (CLIENT_PC.equalsIgnoreCase(clientType) ? DeviceType.PC : DeviceType.APP);
        LoginHelper.loginByDevice(loginUser, deviceType);

        // 小程序可选回填 openid（xcxCode 换取，失败不阻断登录，与账号密码登录第二阶段一致）
        if (StringUtils.isNotBlank(body.getXcxCode()) && ObjectUtil.isNotNull(tenantId)) {
            try {
                WxCode2SessionResult session = wxMiniappService.code2session(body.getXcxCode());
                userTenantBindService.fillUserId(session.getOpenid(), tenantId, user.getUserId());
            } catch (Exception e) {
                log.warn("手机号登录回填 openid 失败 phone={}, tenantId={}", phonenumber, tenantId, e);
            }
        }

        recordLogininfor(phonenumber, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), phonenumber);

        // ⑤ 返回 token + perms + 菜单树（按 client_type 过滤）
        MicroLoginVo vo = new MicroLoginVo();
        vo.setNeedSelectTenant(false);
        vo.setToken(StpUtil.getTokenValue());
        vo.setTenantId(tenantId);
        vo.setRoleCode(roleCode);
        vo.setPermissions(menuService.filterPermissionsForExpiredTenant(loginUser.getMenuPermission()));
        vo.setRoles(loginUser.getRolePermission());
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(user.getUserId(), clientType);
        vo.setMenus(menuService.buildMenus(menus));
        // ★T15.36 下发当前租户版本档位（前端据此条件渲染 UI，如 bossHome 区分 micro_pro/micro_lite）
        if (ObjectUtil.isNotNull(tenantId)) {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null) {
                vo.setVersionType(tenant.getVersionType());
            }
        }
        return vo;
    }

    /**
     * 手机号自动注册（T15.8）：userName=手机号（唯一身份锚点），随机不可用密码（免密登录，仅凭手机号+验证码），
     * tenantId 由场景带入（可空，sys_user.tenant_id 允许 NULL，空则为平台级轻量档案/顾客）。
     * <p>★v1.71 修复：tenantId 非空时自动分配 customer 角色 (9207)，确保 selectMenuPermsByUserId
     * 权限链 sys_user_role → sys_role_menu → sys_menu 完整，否则 @SaCheckPermission 必 403。
     */
    private SysUser registerByPhone(String phonenumber, Long tenantId) {
        SysUser user = new SysUser();
        user.setUserName(phonenumber);
        user.setNickName(phonenumber);
        user.setPhonenumber(phonenumber);
        // 免密登录：随机不可用密码（不对外暴露，登录仅凭手机号+验证码）
        user.setPassword(BCrypt.hashpw(RandomUtil.randomString(16)));
        user.setUserType(UserType.SYS_USER.getUserType());
        user.setStatus("0");
        if (ObjectUtil.isNotNull(tenantId)) {
            user.setTenantId(tenantId);
        }
        user.setCreateBy(phonenumber);
        user.setUpdateBy(phonenumber);
        userMapper.insert(user);
        log.info("[MicroLogin] 手机号 {} 不存在，已自动注册 userId={}, tenantId={}", phonenumber, user.getUserId(), tenantId);
        // ★v1.71：tenantId 非空时自动分配 customer 角色，补全权限链（M20_14）
        if (ObjectUtil.isNotNull(tenantId)) {
            try {
                Long existCount = userRoleMapper.selectCount(
                    Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getUserId()));
                if (existCount == null || existCount == 0) {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(user.getUserId());
                    userRole.setRoleId(CUSTOMER_ROLE_ID);
                    userRoleMapper.insert(userRole);
                    log.info("[MicroLogin] 自动分配 customer 角色：userId={}, roleId={}, tenantId={}",
                        user.getUserId(), CUSTOMER_ROLE_ID, tenantId);
                }
            } catch (Exception e) {
                log.warn("[MicroLogin] 自动分配 customer 角色失败 userId={}: {}", user.getUserId(), e.getMessage());
            }
        }
        return user;
    }

    /**
     * 解析选定租户下的主角色编码：主角色(isPrimary=1)优先，否则取该租户第一个启用角色；
     * 无 user_tenant_role 时回退按 sys_user_role 的 role_key 解析小微主角色（PC 侧创建的老板/员工仅有克隆角色关联）；
     * 两者均无（顾客轻量档案）→ 默认 customer（T15.8 免密浏览，无需 user_tenant_role）。
     */
    private String resolveRoleCode(List<UserTenantRoleVo> tenantRoles, Long tenantId, SysUser user) {
        if (tenantId == null) {
            return DEFAULT_CUSTOMER_ROLE;
        }
        List<UserTenantRoleVo> inTenant = tenantRoles.stream()
            .filter(r -> tenantId.equals(r.getTenantId()))
            .collect(Collectors.toList());
        if (inTenant.isEmpty()) {
            // ★回退解析：PC 侧创建的 boss/salesman 仅写 sys_user_role 克隆角色、无 user_tenant_role，
            // 原逻辑一律兜底 customer 导致老板手机号登录被误分流到顾客首页；现按微角色集优先级回退（v1.42 修复）
            Set<String> roleKeys = permissionService.getRolePermission(user);
            if (roleKeys != null) {
                for (String code : MICRO_ROLE_PRIORITY) {
                    if (roleKeys.contains(code)) {
                        log.info("[MicroLogin] user_tenant_role 缺失，回退 sys_user_role 解析主角色 userId={}, roleCode={}",
                            user.getUserId(), code);
                        return code;
                    }
                }
            }
            return DEFAULT_CUSTOMER_ROLE;
        }
        return inTenant.stream()
            .filter(r -> Integer.valueOf(1).equals(r.getIsPrimary()))
            .map(UserTenantRoleVo::getRoleCode)
            .findFirst()
            .orElse(inTenant.get(0).getRoleCode());
    }

    /**
     * 构建租户候选响应（手机号跨多租户且未指定门店）：委托 {@link #buildTenantOptions} 构建候选项，
     * 供前端弹「租户/门店选择页」；用户选定后携 tenantId 二次调用本接口完成登录。
     */
    private MicroLoginVo buildTenantSelectVo(Set<Long> tenantIds, List<UserTenantRoleVo> tenantRoles) {
        MicroLoginVo vo = new MicroLoginVo();
        vo.setNeedSelectTenant(true);
        vo.setTenants(buildTenantOptions(tenantIds, tenantRoles));
        return vo;
    }

    /**
     * 构建租户候选项列表：查 sys_tenant 回填门店名/Logo/城市/版本档位/行业 + 该租户主角色，
     * 供「我的店铺」选择页展示（T15.36§6.1 每项含 租户名 + 角色展示名 + 版本档位 + 行业图标）。
     */
    private List<MicroTenantOptionVo> buildTenantOptions(Set<Long> tenantIds, List<UserTenantRoleVo> tenantRoles) {
        Map<Long, SysTenant> tenantMap = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .in(SysTenant::getId, tenantIds)).stream()
            .collect(Collectors.toMap(SysTenant::getId, t -> t, (a, b) -> a));
        List<MicroTenantOptionVo> options = new ArrayList<>();
        for (Long tid : tenantIds) {
            MicroTenantOptionVo opt = new MicroTenantOptionVo();
            opt.setTenantId(tid);
            SysTenant t = tenantMap.get(tid);
            if (t != null) {
                opt.setTenantName(t.getCompanyName());
                opt.setLogoUrl(t.getLogoUrl());
                opt.setCity(t.getCity());
                opt.setVersionType(t.getVersionType());
                opt.setIndustryCode(t.getIndustry());
            }
            // 该租户下主角色（用于选择页展示身份）
            UserTenantRoleVo primary = tenantRoles.stream()
                .filter(r -> tid.equals(r.getTenantId()))
                .filter(r -> Integer.valueOf(1).equals(r.getIsPrimary()))
                .findFirst()
                .orElseGet(() -> tenantRoles.stream()
                    .filter(r -> tid.equals(r.getTenantId()))
                    .findFirst().orElse(null));
            if (primary != null) {
                opt.setRoleCode(primary.getRoleCode());
                opt.setRoleName(primary.getRoleName());
            }
            options.add(opt);
        }
        return options;
    }

    /**
     * 查询当前登录用户可切换的多租户列表（T15.36§6.2「我的 → 切换店铺」）：
     * 按 user_tenant_role 取该用户全部启用归属租户，回填门店信息 + 档位 + 行业 + 主角色。
     *
     * @param userId 登录用户ID
     * @return 租户候选列表（单租户也返回，前端据此判断是否展示切换入口）
     */
    public List<MicroTenantOptionVo> listMyTenants(Long userId) {
        List<UserTenantRoleVo> tenantRoles = userTenantRoleService.listByUser(userId).stream()
            .filter(r -> !"1".equals(r.getStatus()))
            .collect(Collectors.toList());
        Set<Long> tenantIds = tenantRoles.stream()
            .map(UserTenantRoleVo::getTenantId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (tenantIds.isEmpty()) {
            return new ArrayList<>();
        }
        return buildTenantOptions(tenantIds, tenantRoles);
    }

    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            StpUtil.logout();
            recordLogininfor(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     */
    private void recordLogininfor(String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String phonenumber, String smsCode) {
        String code = RedisUtils.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + phonenumber);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(phonenumber, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

    /**
     * 校验邮箱验证码
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = RedisUtils.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.defaultString(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    private SysUser loadUserByUsername(String username, Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getStatus, SysUser::getTenantId)
            .eq(SysUser::getUserName, username)
            .eq(SysUser::getDelFlag, "0");
        // 多租户同名用户隔离：tenantId 非空时精确匹配所属租户
        if (tenantId != null) {
            wrapper.eq(SysUser::getTenantId, tenantId);
        }
        List<SysUser> users = userMapper.selectList(wrapper);
        if (users.isEmpty()) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        }
        SysUser user;
        if (users.size() == 1) {
            user = users.get(0);
        } else {
            // 多租户同名用户：tenantId 未传时自动解析
            if (tenantId != null) {
                // 已指定租户但仍有多个匹配（数据异常），取第一条
                user = users.get(0);
            } else {
                // 收集去重租户ID
                Set<Long> tenantIds = users.stream()
                    .map(SysUser::getTenantId)
                    .filter(ObjectUtil::isNotNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                if (tenantIds.size() == 1) {
                    // 唯一租户 → 自动选中
                    user = users.get(0);
                    log.info("登录用户：{} 跨 {} 个租户但归属唯一 tenantId={}，自动选中", username, users.size(), tenantIds.iterator().next());
                } else if (tenantIds.size() > 1) {
                    // 多租户 → 抛候选列表，前端引导选择
                    String candidates = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                    log.info("登录用户：{} 存在于 {} 个租户 {}，需前端引导选择", username, tenantIds.size(), candidates);
                    throw new ServiceException("NEED_SELECT_TENANT:" + candidates);
                } else {
                    // 所有匹配用户 tenantId 均为 null（平台级账号），取第一条
                    user = users.get(0);
                }
            }
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        // 按主键获取完整用户对象（user_id 全局唯一，不受租户拦截器影响）
        return userMapper.selectUserById(user.getUserId());
    }

    private SysUser loadUserByPhonenumber(String phonenumber) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber, SysUser::getStatus)
            .eq(SysUser::getPhonenumber, phonenumber));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", phonenumber);
            throw new UserException("user.not.exists", phonenumber);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", phonenumber);
            throw new UserException("user.blocked", phonenumber);
        }
        return userMapper.selectUserByPhonenumber(phonenumber);
    }

    private SysUser loadUserByEmail(String email) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber, SysUser::getStatus)
            .eq(SysUser::getEmail, email));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", email);
            throw new UserException("user.not.exists", email);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", email);
            throw new UserException("user.blocked", email);
        }
        return userMapper.selectUserByEmail(email);
    }

    /**
     * 按 openid + 选定门店加载绑定用户（T13.1 双阶段租户识别）
     * <p>openid→userId 映射存于 user_tenant_bind；必须显式选定门店（tenantId），禁止按 openid 自动猜测租户。
     * 首次微信登录时绑定记录 userId 为空（仅引导态绑定），须先用账号密码登录回填 userId 后方可一键登录。
     */
    private SysUser loadUserByOpenid(String openid, Long tenantId) {
        if (tenantId == null) {
            throw new ServiceException("请先选择门店后再使用微信一键登录");
        }
        UserTenantBindVo bind = userTenantBindService.listByOpenid(openid).stream()
            .filter(b -> tenantId.equals(b.getTenantId()))
            .findFirst().orElse(null);
        if (bind == null || bind.getUserId() == null) {
            log.info("微信 openid={} 在门店 {} 未绑定系统用户.", openid, tenantId);
            throw new ServiceException("该微信尚未绑定此门店账号，请先用账号密码登录完成绑定");
        }
        SysUser user = userMapper.selectUserById(bind.getUserId());
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", bind.getUserId());
            throw new UserException("user.not.exists", openid);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", bind.getUserId());
            throw new UserException("user.blocked", openid);
        }
        return user;
    }

    /**
     * 构建登录用户
     */
    private LoginUser buildLoginUser(SysUser user) {
        // T3.4 租户到期/停用拦截：到期租户禁止登录（防"过期续用"），租户管理员豁免
        if (ObjectUtil.isNotNull(user.getTenantId())) {
            tenantStatusChecker.checkTenant(user.getTenantId(), user.getUserId());
        }
        LoginUser loginUser = new LoginUser();
        populateLoginUser(loginUser, user);
        return loginUser;
    }

    /**
     * 填充登录用户身份权限（PC/APP/小程序登录共用）
     */
    private void populateLoginUser(LoginUser loginUser, SysUser user) {
        loginUser.setUserId(user.getUserId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickName(user.getNickName());
        loginUser.setUserType(user.getUserType());
        loginUser.setMenuPermission(permissionService.getMenuPermission(user));
        loginUser.setRolePermission(permissionService.getRolePermission(user));
        loginUser.setDeptName(ObjectUtil.isNull(user.getDept()) ? "" : user.getDept().getDeptName());
        List<RoleDTO> roles = BeanUtil.copyToList(user.getRoles(), RoleDTO.class);
        loginUser.setRoles(roles);
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId, String username) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ServletUtils.getClientIP());
        sysUser.setLoginDate(DateUtils.getNowDate());
        sysUser.setUpdateBy(username);
        userMapper.updateById(sysUser);
    }

    /**
     * 登录校验
     */
    private void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = CacheConstants.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(errorKey);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(maxRetryCount)) {
            recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(maxRetryCount)) {
                RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数 则递增
                RedisUtils.setCacheObject(errorKey, errorNumber);
                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // 登录成功 清空错误次数
        RedisUtils.deleteObject(errorKey);
    }
}
