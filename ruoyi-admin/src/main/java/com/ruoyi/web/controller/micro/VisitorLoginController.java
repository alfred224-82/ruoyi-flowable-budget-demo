package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.sms.config.properties.SmsProperties;
import com.ruoyi.sms.numberauth.NumberAuthService;
import com.ruoyi.sms.numberauth.NumberAuthToken;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.domain.TempVisitorAccount;
import com.ruoyi.system.domain.VisitorProfile;
import com.ruoyi.system.domain.bo.VisitorLoginBo;
import com.ruoyi.system.domain.bo.VisitorOneClickLoginBo;
import com.ruoyi.system.domain.bo.VisitorPasswordLoginBo;
import com.ruoyi.system.domain.vo.VisitorLoginVo;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.mapper.TempVisitorAccountMapper;
import com.ruoyi.system.mapper.VisitorProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.secure.BCrypt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 游客扫码登录Controller（M31 T31.4/T31.5，匿名端点）
 * <p>顾客扫描活动海报二维码 → 小程序启动 → 解析 scene 参数 → 手机号验证码登录 → 创建临时游客账号 → 返回 JWT。
 * <p>★幂等性：同一手机号后 4 位在同一租户下只能生成一个有效账号。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/login")
public class VisitorLoginController extends BaseController {

    private final SysUserMapper sysUserMapper;
    private final SysTenantMapper sysTenantMapper;
    private final TempVisitorAccountMapper tempVisitorAccountMapper;
    private final VisitorProfileMapper visitorProfileMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SmsProperties smsProperties;
    private final NumberAuthService numberAuthService;

    /** 临时账号有效期（天） */
    private static final int VISITOR_EXPIRE_DAYS = 30;

    /**
     * ★顾客角色 ID（customer=9207，全局共享角色，tenant_id=NULL，见 M15_07）。
     * <p>游客扫码登录（短信/一键）落地的人员【固定】为顾客角色，绝不分配任何其他角色。
     */
    private static final long CUSTOMER_ROLE_ID = 9207L;

    /** 游客短信验证码缓存key前缀（Redis: sms:visitor:code:{phone}） */
    private static final String VISITOR_SMS_CODE_KEY = "sms:visitor:code:";

    /** 游客短信日发送计数key前缀（Redis: sms:visitor:day:{phone}） */
    private static final String VISITOR_SMS_DAY_KEY = "sms:visitor:day:";

    /** 验证码有效期（分钟） */
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;

    /** 同一手机号每日发送上限默认值（未配置时兆底） */
    private static final int DEFAULT_SMS_DAY_LIMIT = 10;

    /**
     * 发送游客短信验证码（匿名端点）
     * <p>① 防刷：同一手机号每日最多 {@link #DEFAULT_SMS_DAY_LIMIT} 次 + IP 限流；
     * <p>② 优先走 PNVS 短信认证（number-auth.sms-enabled=true）：调 SendSmsVerifyCode 由阿里云赠送签名+模板真实下发，
     *    验证码由平台生成并托管，本项目不再存 Redis 码，登录时用 CheckSmsVerifyCode 核验；
     * <p>③ 未开通 PNVS 时降级开发模式：仅 dev-code-enabled=true 才生成 devCode 存 Redis 并返回供联调；
     *    生产必须为 false → 直接报错，杜绝「假校验」（任意6位即可登录的漏洞）。
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @GetMapping("/visitorSmsCode")
    public R<Map<String, Object>> visitorSmsCode(@NotBlank(message = "手机号不能为空") String phone) {
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return R.fail("请输入正确的手机号");
        }
        // ① 日发送上限（PNVS 按发送计费，本地再兜一层防刷；上限值从配置文件读取，未配置时兆底 10）
        int smsDayLimit = smsProperties.getSmsDayLimit() != null ? smsProperties.getSmsDayLimit() : DEFAULT_SMS_DAY_LIMIT;
        String dayKey = VISITOR_SMS_DAY_KEY + phone;
        long dayCount = RedisUtils.incrAtomicValue(dayKey);
        if (dayCount == 1) {
            RedisUtils.expire(dayKey, Duration.ofDays(1));
        }
        if (dayCount > smsDayLimit) {
            return R.fail("今日验证码发送次数已达上限（每日" + smsDayLimit + "次），请明天再试");
        }
        String maskedPhone = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        Map<String, Object> data = new HashMap<>(2);
        // ② PNVS 短信认证（赠送签名+模板）：平台生成并托管验证码，本项目不存 Redis 码
        if (numberAuthService.isSmsEnabled()) {
            try {
                numberAuthService.sendSmsVerifyCode(phone);
            } catch (Exception e) {
                // ★发送失败不占用每日配额（通道故障时避免失败请求耗尽用户当日次数），回滚计数后再抛错
                RedisUtils.decrAtomicValue(dayKey);
                throw e;
            }
            data.put("sent", true);
            return R.ok(data);
        }
        // ③ 未开通 PNVS：仅 dev-code-enabled=true 时返回 devCode 供联调；生产必须 false → 报错杜绝假校验
        if (!Boolean.TRUE.equals(smsProperties.getDevCodeEnabled())) {
            log.warn("[VisitorSms] 短信通道未开启且开发模式未开放，拒绝发送验证码 phone={}", maskedPhone);
            return R.fail("短信通道未开通，请联系管理员配置阿里云号码认证短信");
        }
        String code = RandomUtil.randomNumbers(6);
        RedisUtils.setCacheObject(VISITOR_SMS_CODE_KEY + phone, code, Duration.ofMinutes(SMS_CODE_EXPIRE_MINUTES));
        log.info("[VisitorSms] 开发模式验证码 phone={} code={}", maskedPhone, code);
        data.put("sent", false);
        data.put("devCode", code);
        return R.ok(data);
    }

    /**
     * 游客扫码登录（匿名端点）
     * <p>① 校验短信验证码（PNVS CheckSmsVerifyCode / 开发模式 Redis devCode）；
     * ② 按【手机号 + 租户】定位 sys_user（顾客角色）→ 存在则复用，不存在则新建；
     * ③ 新建时【固定】分配顾客角色 customer(9207)，并建 temp_visitor_account + visitor_profile；
     * ④ 返回 JWT token + isNewAccount 标志。
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @Log(title = "游客扫码登录", businessType = BusinessType.OTHER)
    @PostMapping("/visitorLogin")
    public R<VisitorLoginVo> visitorLogin(@Validated @RequestBody VisitorLoginBo bo) {
        String phone = bo.getPhone();
        String scene = bo.getScene();
        String smsCode = bo.getSmsCode();

        // ① 解析 scene 参数（t/a/u/p 编码）
        SceneParam param = parseScene(scene);
        if (param == null || param.tenantId == null || param.activityId == null) {
            throw new ServiceException("scene 参数格式不正确");
        }
        if (!"visitor".equals(param.userType)) {
            throw new ServiceException("非游客类型 scene");
        }

        // ② 校验短信验证码：PNVS 开启时用 CheckSmsVerifyCode 核验（验证码由平台托管）；否则校验 Redis devCode（开发模式）
        //    ★保底策略：sms-verify-fallback-enabled=true 时，若 PNVS 核验接口抛异常（云接口抖动/超时），
        //      只要用户输入的是 6 位数字即视为通过并放行登录（避免"核验服务不可用"阻断用户）。
        if (StrUtil.isBlank(smsCode) || smsCode.length() != 6 || !smsCode.matches("^\\d{6}$")) {
            throw new ServiceException("短信验证码格式不正确（需6位数字）");
        }
        if (numberAuthService.isSmsEnabled()) {
            boolean smsVerifyPassed = false;
            boolean smsServiceDown = false;
            try {
                smsVerifyPassed = numberAuthService.checkSmsVerifyCode(phone, smsCode);
            } catch (ServiceException e) {
                // PNVS 核验接口异常（云接口抖动/超时/配置缺失）：标记服务不可用
                smsServiceDown = true;
                log.warn("[VisitorLogin] PNVS核验服务异常 phone={} msg={}", phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"), e.getMessage());
            }
            if (!smsVerifyPassed) {
                // ★保底策略：服务不可用 + 配置开启 fallback → 6位数字直接放行
                if (smsServiceDown && Boolean.TRUE.equals(smsProperties.getSmsVerifyFallbackEnabled())) {
                    log.warn("[VisitorLogin] ★保底放行：PNVS核验服务不可用且sms-verify-fallback-enabled=true，6位数字直接通过 phone={}",
                        phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                } else {
                    throw new ServiceException("验证码错误或已过期，请重新获取");
                }
            }
        } else {
            String cachedCode = RedisUtils.getCacheObject(VISITOR_SMS_CODE_KEY + phone);
            if (StrUtil.isBlank(cachedCode) || !cachedCode.equals(smsCode)) {
                throw new ServiceException("验证码错误或已过期，请重新获取");
            }
            RedisUtils.deleteObject(VISITOR_SMS_CODE_KEY + phone);
        }

        // ③ 签发游客会话（临时账号幂等复用/新建 → JWT）
        return R.ok(issueVisitorSession(phone, param.tenantId));
    }

    /**
     * 游客密码登录（匿名端点，备用方案）
     * <p>★短信通道不可用时的兆底方案：用户输入手机号 + 密码 → 校验 sys_user 密码 → 签发游客会话。
     * <p>密码校验使用 BCrypt.checkpw，与 SysLoginService.login 保持一致。
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @Log(title = "游客密码登录", businessType = BusinessType.OTHER)
    @PostMapping("/visitorPasswordLogin")
    public R<VisitorLoginVo> visitorPasswordLogin(@Validated @RequestBody VisitorPasswordLoginBo bo) {
        String phone = bo.getPhone();
        String password = bo.getPassword();
        String scene = bo.getScene();

        // ① 解析 scene 参数
        SceneParam param = parseScene(scene);
        if (param == null || param.tenantId == null || param.activityId == null) {
            throw new ServiceException("scene 参数格式不正确");
        }
        if (!"visitor".equals(param.userType)) {
            throw new ServiceException("非游客类型 scene");
        }

        // ② 按手机号 + 租户查找用户
        SysUser user = sysUserMapper.selectUserByPhonenumberAndTenant(phone, param.tenantId);
        if (user == null) {
            throw new ServiceException("该手机号未注册，请先使用短信验证码登录");
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException("账号已被停用，请联系管理员");
        }

        // ③ 校验密码（BCrypt）
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ServiceException("密码错误，请重试");
        }

        // ④ 签发游客会话（复用已有账号，不新建）
        log.info("[VisitorPasswordLogin] 密码登录成功 userId={} phone={}", user.getUserId(),
            phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        return R.ok(issueVisitorSession(phone, param.tenantId));
    }

    /**
     * 游客一键登录（匿名端点，M35）：前端授权页取得 spToken → 后端 GetPhoneWithToken 换明文手机号 → 签发游客会话。
     * <p>★证明链：spToken 由运营商数据网关签发（本机 SIM + 用户手输中间4位双重确认），取号成功即真实手机证明；
     * 取号失败（WiFi/未开通）时前端自动降级短信验证码。
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @Log(title = "游客一键登录", businessType = BusinessType.OTHER)
    @PostMapping("/visitorOneClickLogin")
    public R<VisitorLoginVo> visitorOneClickLogin(@Validated @RequestBody VisitorOneClickLoginBo bo) {
        // ① 解析 scene 参数（一键登录场景 p 段为 0000 占位）
        SceneParam param = parseScene(bo.getScene());
        if (param == null || param.tenantId == null || param.activityId == null) {
            throw new ServiceException("scene 参数格式不正确");
        }
        if (!"visitor".equals(param.userType)) {
            throw new ServiceException("非游客类型 scene");
        }
        // ② spToken 换明文手机号（一次性 Token，立即取号）
        String phone = numberAuthService.getPhoneWithToken(bo.getSpToken());
        // ③ 签发游客会话
        return R.ok(issueVisitorSession(phone, param.tenantId));
    }

    /**
     * 获取 H5 一键登录鉴权 Token（匿名端点，M35）：前端 SDK checkLoginAvailable 鉴权用。
     * <p>★该端点为登录页加载时的「探测」调用：未开启或取 Token 失败时仍返回 {@code R.ok()}（code=200，避免进页面弹 toast），
     * 但通过 {@code failReason} 下发失败原因，前端降级同时在面板展示，便于截图定位是 SDK/后端/运营商哪一段失败。
     */
    @SaIgnore
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/numberAuthToken")
    public R<NumberAuthToken> numberAuthToken() {
        if (!numberAuthService.isEnabled()) {
            return R.ok(failToken("后端未开启一键登录（number-auth.enabled=false）"));
        }
        try {
            return R.ok(numberAuthService.getAuthToken());
        } catch (Exception e) {
            // 已开启但取鉴权 Token 失败（配置不全 / 云接口异常）：记录日志并静默降级，不阻断登录页
            log.error("[NumberAuth] 获取 H5 鉴权 Token 失败，前端降级短信验证码", e);
            String msg = e.getMessage();
            return R.ok(failToken("获取鉴权Token失败：" + StrUtil.blankToDefault(msg, e.getClass().getSimpleName())));
        }
    }

    /**
     * 构造仅含失败原因的探测响应（token 为空 → 前端降级短信，failReason 供展示定位）
     */
    private NumberAuthToken failToken(String reason) {
        NumberAuthToken token = new NumberAuthToken();
        token.setFailReason(reason);
        return token;
    }

    /**
     * 签发游客登录会话（短信验证码登录 / 一键登录共用）：
     * <p>★身份锚点【固定】为「手机号 + 顾客角色」：按 手机号+租户 定位 sys_user，命中即复用（并自愈补挂顾客角色）；
     * <p>① 未命中 → 新建 sys_user + 顾客角色(9207) + temp_visitor_account + visitor_profile；
     * ② 命中 → 复用 user_id 并对 temp_visitor_account 续期；
     * ③ 生成 JWT token 并构建响应。
     */
    private VisitorLoginVo issueVisitorSession(String phone, Long tenantId) {
        // 手机号后 4 位（仅用于昵称/临时账号展示，不作为身份锚点）
        String phoneLast4 = phone.substring(phone.length() - 4);

        // ★身份锚点固定为「手机号 + 顾客角色」：按 手机号+租户 定位 sys_user（不再用 phone_last4，后4位会跨号碰撞）
        SysUser existingUser = sysUserMapper.selectUserByPhonenumberAndTenant(phone, tenantId);

        boolean isNewAccount = false;
        Long userId;
        TempVisitorAccount existing;

        if (existingUser != null) {
            // 复用已有顾客人员：固定顾客角色（历史无角色数据自愈补挂 customer）
            userId = existingUser.getUserId();
            ensureCustomerRole(userId);
            existing = tempVisitorAccountMapper.selectOne(
                Wrappers.<TempVisitorAccount>lambdaQuery()
                    .eq(TempVisitorAccount::getUserId, userId)
                    .orderByDesc(TempVisitorAccount::getId)
                    .last("limit 1"));
            if (existing != null) {
                if (existing.getStatus() == null || existing.getStatus() != TempVisitorAccount.STATUS_EXPIRED) {
                    // 续期 30 天
                    existing.setExpireTime(DateUtils.addDays(new Date(), VISITOR_EXPIRE_DAYS));
                    tempVisitorAccountMapper.updateById(existing);
                }
            } else {
                // 历史遗留：有 sys_user 但无临时账号记录，补建
                existing = insertTempVisitorAccount(tenantId, phone, phoneLast4, userId);
            }
            log.info("[VisitorLogin] 复用顾客账号 userId={} phoneLast4={}", userId, phoneLast4);
        } else {
            // 新建顾客人员（sys_user + customer 角色 + temp_visitor_account + visitor_profile）
            isNewAccount = true;
            userId = createTempVisitor(tenantId, phone, phoneLast4);
            existing = tempVisitorAccountMapper.selectOne(
                Wrappers.<TempVisitorAccount>lambdaQuery()
                    .eq(TempVisitorAccount::getUserId, userId)
                    .orderByDesc(TempVisitorAccount::getId)
                    .last("limit 1"));
            log.info("[VisitorLogin] 新建顾客账号 userId={} phoneLast4={}", userId, phoneLast4);
        }

        // 生成 JWT token
        SysUser user = existingUser != null ? existingUser : sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // 构建登录用户（轻量模式，仅含基本信息）
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickName(user.getNickName());
        loginUser.setUserType(user.getUserType());
        LoginHelper.loginByDevice(loginUser, DeviceType.XCX);
        String token = StpUtil.getTokenValue();

        // 构建响应
        VisitorLoginVo vo = new VisitorLoginVo();
        vo.setToken(token);
        vo.setUserId(userId);
        vo.setIsNewAccount(isNewAccount);
        vo.setNickName(user.getNickName());
        vo.setPhone(phone);
        vo.setVisitorStatus(existing != null ? existing.getStatus() : TempVisitorAccount.STATUS_ACTIVE);
        return vo;
    }

    /**
     * 创建游客对应的【顾客】人员（sys_user + customer 角色 + temp_visitor_account + visitor_profile）
     * <p>★身份锚点=手机号；角色【固定】为顾客 customer(9207)，绝不分配其他角色。
     */
    private Long createTempVisitor(Long tenantId, String phone, String phoneLast4) {
        // ① 新建 sys_user（phonenumber=完整手机号，作为后续「按手机号直接登录」的锚点）
        String username = "VISITOR_" + tenantId + "_" + phoneLast4 + "_" + System.currentTimeMillis();
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUserName(username);
        user.setNickName("游客" + phoneLast4);
        user.setPhonenumber(phone);
        user.setPassword(RandomUtil.randomString(16)); // 随机密码（游客不通过密码登录）
        user.setStatus("0"); // 0=正常（TEMPORARY 语义由 temp_visitor_account 管理）
        user.setCreateTime(new Date());
        // 查询租户根部门
        SysTenant tenant = sysTenantMapper.selectById(tenantId);
        if (tenant != null) {
            // dept_id 取租户根部门（简化处理）
            user.setDeptId(100L); // 默认根部门
        }
        sysUserMapper.insert(user);

        // ② ★固定分配「顾客」角色（customer=9207），补全权限链 sys_user_role→sys_role_menu→sys_menu
        ensureCustomerRole(user.getUserId());

        // ③ 新建 temp_visitor_account（有效期/状态跟踪，按 user_id 关联）
        insertTempVisitorAccount(tenantId, phone, phoneLast4, user.getUserId());

        // ④ 新建 visitor_profile（空记录）
        VisitorProfile profile = new VisitorProfile();
        profile.setUserId(user.getUserId());
        profile.setTenantId(tenantId);
        profile.setIsComplete(0);
        visitorProfileMapper.insert(profile);

        return user.getUserId();
    }

    /**
     * 新建临时游客账号记录（temp_visitor_account，按 user_id 关联，有效期 30 天）
     */
    private TempVisitorAccount insertTempVisitorAccount(Long tenantId, String phone, String phoneLast4, Long userId) {
        TempVisitorAccount account = new TempVisitorAccount();
        account.setTenantId(tenantId);
        account.setPhoneLast4(phoneLast4);
        account.setUserId(userId);
        account.setPhone(phone);
        account.setExpireTime(DateUtils.addDays(new Date(), VISITOR_EXPIRE_DAYS));
        account.setStatus(TempVisitorAccount.STATUS_ACTIVE);
        tempVisitorAccountMapper.insert(account);
        return account;
    }

    /**
     * ★固定顾客角色：确保该用户拥有 customer(9207) 角色。
     * <p>与 {@code SysLoginService.registerByPhone} 口径一致——仅当用户【无任何角色】时补挂顾客角色，
     * 既能自愈历史无角色的游客数据，又不会污染已是 boss/salesman 的账号。
     */
    private void ensureCustomerRole(Long userId) {
        Long roleCount = sysUserRoleMapper.selectCount(
            Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleCount == null || roleCount == 0) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(CUSTOMER_ROLE_ID);
            sysUserRoleMapper.insert(userRole);
            log.info("[VisitorLogin] 分配顾客角色 userId={} roleId={}", userId, CUSTOMER_ROLE_ID);
        }
    }

    /**
     * 解析 scene 参数（t/a/u/p 编码）
     * <p>格式：{@code t{tenantId}a{activityId}u{userType}p{phoneLast4}}
     */
    private SceneParam parseScene(String scene) {
        if (StrUtil.isBlank(scene)) {
            return null;
        }
        try {
            SceneParam param = new SceneParam();
            // 简单解析：t123a456uvp0000
            int tIdx = scene.indexOf('t');
            int aIdx = scene.indexOf('a');
            int uIdx = scene.indexOf('u');
            int pIdx = scene.indexOf('p');
            if (tIdx < 0 || aIdx < 0 || uIdx < 0 || pIdx < 0) {
                return null;
            }
            param.tenantId = Long.parseLong(scene.substring(tIdx + 1, aIdx));
            param.activityId = Long.parseLong(scene.substring(aIdx + 1, uIdx));
            param.userType = scene.substring(uIdx + 1, pIdx);
            param.phoneLast4 = scene.substring(pIdx + 1);
            return param;
        } catch (Exception e) {
            log.warn("[VisitorLogin] scene 解析失败: {}", scene);
            return null;
        }
    }

    /**
     * scene 参数解析结果
     */
    private static class SceneParam {
        Long tenantId;
        Long activityId;
        String userType;
        String phoneLast4;
    }
}
