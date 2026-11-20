package com.ruoyi.sms.numberauth;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.GetAuthTokenRequest;
import com.aliyun.dypnsapi20170525.models.GetAuthTokenResponse;
import com.aliyun.dypnsapi20170525.models.GetPhoneWithTokenRequest;
import com.aliyun.dypnsapi20170525.models.GetPhoneWithTokenResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云号码认证服务（H5 一键登录，M35）
 * <p>对接 dypnsapi OpenAPI 两个接口（与前端 numberAuth-web-sdk.js 配对）：
 * <ul>
 *     <li>{@link #getAuthToken()} → GetAuthToken：下发 accessToken/jwtToken 供前端 SDK checkLoginAvailable 鉴权；</li>
 *     <li>{@link #getPhoneWithToken(String)} → GetPhoneWithToken：前端授权页取得 spToken 后换取明文手机号。</li>
 * </ul>
 * <p>★证明链：spToken 由运营商数据网关签发（仅本机 SIM + 用户手输中间4位双重确认），
 * 取号成功即证明"当前设备持有该号码"，证明力强于短信验证码。
 * <p>★spToken 一次性：移动 2 分钟 / 电信 10 分钟 / 联通 30 分钟内仅可使用一次，取号须立即调用。
 *
 * @author ruoyi
 * @date 2026-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NumberAuthService {

    /** 号码认证服务端点 */
    private static final String ENDPOINT = "dypnsapi.aliyuncs.com";

    /** 业务类型：1=一键登录（与前端 checkLoginAvailable/getLoginToken 配对，不可与本机号码校验混用） */
    private static final int BIZ_TYPE_ONE_CLICK_LOGIN = 1;

    /** CheckSmsVerifyCode 核验通过标识（★接口 Code=OK 仅代表调用成功，核验结果只认 Model.VerifyResult） */
    private static final String SMS_VERIFY_RESULT_PASS = "PASS";

    /** 默认国家编码 */
    private static final String DEFAULT_COUNTRY_CODE = "86";

    /** 默认验证码位数 / 有效期（秒），配置缺省时兜底 */
    private static final long DEFAULT_CODE_LENGTH = 6L;
    private static final long DEFAULT_VALID_TIME_SECONDS = 300L;

    /** 验证码生成类型：1=纯数字（与前端 6 位数字校验一致；TemplateParam 传 ##code## 占位符时必填） */
    private static final long CODE_TYPE_DIGITS = 1L;

    private final NumberAuthProperties properties;

    /** Client 懒加载（凭据变更需重启生效，单实例够用） */
    private volatile Client client;

    /**
     * 一键登录是否启用（未启用时前端不展示一键登录按钮，走短信验证码兜底）
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /**
     * PNVS 短信认证兜底通道是否启用（未启用时前端验证码降级开发模式 devCode）
     */
    public boolean isSmsEnabled() {
        return Boolean.TRUE.equals(properties.getSmsEnabled());
    }

    /**
     * 获取 H5 鉴权 Token（GetAuthToken）
     *
     * @return accessToken（10分钟）+ jwtToken（1小时）
     */
    public NumberAuthToken getAuthToken() {
        checkConfig();
        try {
            GetAuthTokenRequest request = new GetAuthTokenRequest()
                .setUrl(properties.getPageUrl())
                .setOrigin(properties.getOrigin())
                .setSceneCode(properties.getSceneCode())
                .setBizType(BIZ_TYPE_ONE_CLICK_LOGIN);
            GetAuthTokenResponse response = client().getAuthToken(request);
            if (response.getBody() == null || !"OK".equals(response.getBody().getCode())
                || response.getBody().getTokenInfo() == null) {
                String code = response.getBody() == null ? null : response.getBody().getCode();
                String msg = response.getBody() == null ? null : response.getBody().getMessage();
                log.error("[NumberAuth] GetAuthToken 失败 code={} msg={}", code, msg);
                throw new ServiceException("一键登录鉴权失败：" + StringUtils.blankToDefault(msg, code));
            }
            NumberAuthToken token = new NumberAuthToken();
            token.setAccessToken(response.getBody().getTokenInfo().getAccessToken());
            token.setJwtToken(response.getBody().getTokenInfo().getJwtToken());
            return token;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[NumberAuth] GetAuthToken 异常", e);
            throw new ServiceException("一键登录鉴权服务暂不可用，请稍后重试");
        }
    }

    /**
     * 一键登录取号（GetPhoneWithToken）
     *
     * @param spToken 前端 SDK 授权页成功后返回的一次性 Token
     * @return 明文手机号
     */
    public String getPhoneWithToken(String spToken) {
        checkConfig();
        if (StringUtils.isBlank(spToken)) {
            throw new ServiceException("spToken 不能为空");
        }
        try {
            GetPhoneWithTokenRequest request = new GetPhoneWithTokenRequest().setSpToken(spToken);
            GetPhoneWithTokenResponse response = client().getPhoneWithToken(request);
            if (response.getBody() == null || !"OK".equals(response.getBody().getCode())
                || response.getBody().getData() == null
                || StringUtils.isBlank(response.getBody().getData().getMobile())) {
                String code = response.getBody() == null ? null : response.getBody().getCode();
                String msg = response.getBody() == null ? null : response.getBody().getMessage();
                log.error("[NumberAuth] GetPhoneWithToken 失败 code={} msg={}", code, msg);
                throw new ServiceException("一键登录取号失败，请改用短信验证码登录");
            }
            String mobile = response.getBody().getData().getMobile();
            log.info("[NumberAuth] 一键登录取号成功 phone={}", mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            return mobile;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[NumberAuth] GetPhoneWithToken 异常", e);
            throw new ServiceException("一键登录取号服务暂不可用，请改用短信验证码登录");
        }
    }

    /**
     * 发送短信验证码（SendSmsVerifyCode，PNVS 赠送签名+模板）
     * <p>★验证码由阿里云平台自动生成并托管生命周期（有效期/重发间隔由参数控制），
     * 本项目不再自行生成/存储验证码，核验时调 {@link #checkSmsVerifyCode(String, String)}。
     *
     * @param phone 接收验证码的手机号
     */
    public void sendSmsVerifyCode(String phone) {
        checkSmsConfig();
        if (StringUtils.isBlank(phone)) {
            throw new ServiceException("手机号不能为空");
        }
        try {
            long validSeconds = properties.getSmsValidTime() == null ? DEFAULT_VALID_TIME_SECONDS : properties.getSmsValidTime();
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName(properties.getSmsSignName())
                .setTemplateCode(properties.getSmsTemplateCode())
                // ★TemplateParam 为必填参数（缺失报 400 MissingTemplateParam）：验证码位置传 ##code## 占位符由平台按 CodeType 生成并托管核验；
                //   赠送模板 100001 的有效期分钟变量 min 按 ValidTime 秒换算回填
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"" + Math.max(1, validSeconds / 60) + "\"}")
                .setCodeType(CODE_TYPE_DIGITS)
                .setCountryCode(StringUtils.blankToDefault(properties.getSmsCountryCode(), DEFAULT_COUNTRY_CODE))
                .setCodeLength(properties.getSmsCodeLength() == null ? DEFAULT_CODE_LENGTH : properties.getSmsCodeLength())
                .setValidTime(validSeconds)
                .setDuplicatePolicy(properties.getSmsDuplicatePolicy())
                .setInterval(properties.getSmsInterval());
            SendSmsVerifyCodeResponse response = client().sendSmsVerifyCode(request);
            // 发送成功以 Code=OK 为准
            if (response.getBody() == null || !"OK".equals(response.getBody().getCode())) {
                String code = response.getBody() == null ? null : response.getBody().getCode();
                String msg = response.getBody() == null ? null : response.getBody().getMessage();
                log.error("[NumberAuth] SendSmsVerifyCode 失败 code={} msg={}", code, msg);
                throw new ServiceException("验证码短信发送失败：" + StringUtils.blankToDefault(msg, code));
            }
            log.info("[NumberAuth] 验证码短信发送成功 phone={}", maskPhone(phone));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[NumberAuth] SendSmsVerifyCode 异常 phone={}", maskPhone(phone), e);
            throw new ServiceException("验证码短信发送服务暂不可用，请稍后重试");
        }
    }

    /**
     * 核验短信验证码（CheckSmsVerifyCode）
     * <p>★阿里云明确：接口 Code=OK 不代表核验成功，核验结果仅以 {@code Model.VerifyResult==PASS} 为准。
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=核验通过
     */
    public boolean checkSmsVerifyCode(String phone, String code) {
        checkSmsConfig();
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
            return false;
        }
        try {
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setVerifyCode(code)
                .setCountryCode(StringUtils.blankToDefault(properties.getSmsCountryCode(), DEFAULT_COUNTRY_CODE));
            CheckSmsVerifyCodeResponse response = client().checkSmsVerifyCode(request);
            if (response.getBody() == null || response.getBody().getModel() == null) {
                String c = response.getBody() == null ? null : response.getBody().getCode();
                log.warn("[NumberAuth] CheckSmsVerifyCode 无核验结果 phone={} code={}", maskPhone(phone), c);
                return false;
            }
            String verifyResult = response.getBody().getModel().getVerifyResult();
            boolean pass = SMS_VERIFY_RESULT_PASS.equalsIgnoreCase(verifyResult);
            if (!pass) {
                log.info("[NumberAuth] 验证码核验未通过 phone={} verifyResult={}", maskPhone(phone), verifyResult);
            }
            return pass;
        } catch (Exception e) {
            log.error("[NumberAuth] CheckSmsVerifyCode 异常 phone={}", maskPhone(phone), e);
            throw new ServiceException("验证码核验服务暂不可用，请稍后重试");
        }
    }

    /**
     * 配置完整性校验（一键登录：fail-fast，避免带着空凭据调云接口）
     */
    private void checkConfig() {
        if (!isEnabled()) {
            throw new ServiceException("一键登录未开启");
        }
        checkCredentials();
        if (StringUtils.isBlank(properties.getSceneCode())) {
            throw new ServiceException("一键登录未配置方案 Code（number-auth.scene-code）");
        }
    }

    /**
     * 短信认证配置校验（需启用 + 凭据 + 赠送签名/模板）
     */
    private void checkSmsConfig() {
        if (!isSmsEnabled()) {
            throw new ServiceException("短信认证未开启");
        }
        checkCredentials();
        if (StringUtils.isBlank(properties.getSmsSignName()) || StringUtils.isBlank(properties.getSmsTemplateCode())) {
            throw new ServiceException("短信认证未配置完整（number-auth.sms-sign-name/sms-template-code）");
        }
    }

    /**
     * 阿里云凭据校验（一键登录与短信认证共用）
     */
    private void checkCredentials() {
        if (StringUtils.isBlank(properties.getAccessKeyId()) || StringUtils.isBlank(properties.getAccessKeySecret())) {
            throw new ServiceException("号码认证未配置 AccessKey（number-auth.access-key-id/access-key-secret）");
        }
    }

    /**
     * 手机号脱敏（日志用）
     */
    private String maskPhone(String phone) {
        return StringUtils.isBlank(phone) ? "" : phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 懒加载 dypnsapi Client（双重检查锁）
     */
    private Client client() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config config = new Config()
                        .setAccessKeyId(properties.getAccessKeyId())
                        .setAccessKeySecret(properties.getAccessKeySecret())
                        .setEndpoint(ENDPOINT);
                    client = new Client(config);
                }
            }
        }
        return client;
    }

}
