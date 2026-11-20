package com.ruoyi.sms.numberauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云号码认证服务配置属性（H5 一键登录，M35）
 * <p>对应 yml 节点 {@code number-auth}；需在阿里云控制台开通号码认证服务并创建
 * <b>H5 一键登录认证方案</b>（获取 sceneCode，登记页面地址/Origin）后填值。
 *
 * @author ruoyi
 * @date 2026-10-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "number-auth")
public class NumberAuthProperties {

    /**
     * 一键登录总开关：false 时前端不展示一键登录按钮、后端拒绝取号（降级短信验证码）
     */
    private Boolean enabled;

    /**
     * 阿里云 AccessKeyId（RAM 子账号需具备 dypns 号码认证权限）
     */
    private String accessKeyId;

    /**
     * 阿里云 AccessKeySecret
     */
    private String accessKeySecret;

    /**
     * 认证方案 Code（控制台创建 H5 一键登录方案后获取，如 FC10000010643xxx）
     */
    private String sceneCode;

    /**
     * 请求页面地址（必须与控制台方案登记一致，格式：协议 + // + 域名 + /，如 https://xiaozhangben.com.cn/）
     */
    private String pageUrl;

    /**
     * 请求源地址（必须与控制台方案登记一致，格式：协议 + // + 域名，末尾无 /）
     */
    private String origin;

    // ==================== PNVS 短信认证（赠送签名+模板，SendSmsVerifyCode/CheckSmsVerifyCode） ====================
    // ★与一键登录共用同一把 accessKeyId/accessKeySecret 与同一个 dypnsapi Client；
    // 赠送签名必须搭配赠送模板使用，不能走 dysmsapi SendSms（那是标准短信服务的另一套）。

    /**
     * 短信认证兜底通道开关：true 时游客验证码走 PNVS SendSmsVerifyCode 真实下发 + CheckSmsVerifyCode 核验；
     * false 时降级开发模式（devCode，仅 dev 环境）。
     */
    private Boolean smsEnabled;

    /**
     * PNVS 赠送短信签名（控制台「赠送签名配置」页原样抄写，如 恒创联众）
     */
    private String smsSignName;

    /**
     * PNVS 赠送短信模板 Code（控制台「模板配置」页，登录/注册验证码模板，如 100001）
     */
    private String smsTemplateCode;

    /**
     * 号码国家编码（默认 86，目前仅支持国内号码）
     */
    private String smsCountryCode;

    /**
     * 验证码位数（与前端 6 位校验一致）
     */
    private Long smsCodeLength;

    /**
     * 验证码有效期（单位：秒，如 300 = 5 分钟；会回填模板「有效期」变量）
     */
    private Long smsValidTime;

    /**
     * 重复发送策略（1：仅当验证码未失效时不重发，直接复用；具体以阿里云文档为准）
     */
    private Long smsDuplicatePolicy;

    /**
     * 两次发送最小间隔（单位：秒，如 60）
     */
    private Long smsInterval;

}
