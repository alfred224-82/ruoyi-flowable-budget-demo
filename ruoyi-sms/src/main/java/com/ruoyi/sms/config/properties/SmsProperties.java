package com.ruoyi.sms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMS短信 配置属性
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private Boolean enabled;

    /**
     * 配置节点
     * 阿里云 dysmsapi.aliyuncs.com
     * 腾讯云 sms.tencentcloudapi.com
     */
    private String endpoint;

    /**
     * key
     */
    private String accessKeyId;

    /**
     * 密匙
     */
    private String accessKeySecret;

    /*
     * 短信签名
     */
    private String signName;

    /**
     * 短信应用ID (腾讯专属)
     */
    private String sdkAppId;

    /**
     * 开发模式验证码开关：短信云通道未开启时，是否将验证码经 devCode 返回供联调。
     * <p>★安全：仅限开发/测试环境置 true；生产必须为 false（未配云短信时直接报错，禁止假校验）。
     */
    private Boolean devCodeEnabled;

    /**
     * 同一手机号每日短信发送上限（防刷），默认 10 次。
     * <p>对应 yml 节点 {@code sms.sms-day-limit}；未配置时兜底 10。
     */
    private Integer smsDayLimit;

    /**
     * 短信核验服务不可用时的保底开关：true 时若 PNVS CheckSmsVerifyCode 抛异常（云接口抖动/超时），
     * 只要用户输入的是 6 位数字即视为通过并放行登录；false 时严格拒绝。
     * <p>★安全：仅限短信通道不稳定时临时置 true 做降级保底；通道恢复后应置回 false。
     */
    private Boolean smsVerifyFallbackEnabled;

}
