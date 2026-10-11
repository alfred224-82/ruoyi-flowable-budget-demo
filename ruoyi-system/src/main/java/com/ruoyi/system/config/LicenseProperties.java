package com.ruoyi.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * License授权配置属性
 * 通过 license.enforce-mode 控制校验强度：
 * - soft: 开发/测试环境，仅读取数据库配置，不校验签名和指纹（方便调试）
 * - hard: 生产环境，强制校验RSA数字签名 + 机器指纹绑定 + 防篡改
 *
 * @author ruoyi
 * @date 2026-07-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "license")
public class LicenseProperties {

    /**
     * 执行模式：soft=软限制(开发测试) / hard=加固模式(生产)
     */
    private String enforceMode = "soft";

    /**
     * RSA公钥（Base64编码）
     * hard模式下用于验证License签名
     * 由 LicenseGenerateTool 生成密钥对时产出
     */
    private String publicKey = "";

    /**
     * RSA私钥（Base64编码，可选）
     * <p>仅 Admin 侧签发 License 时使用（{@code POST /system/license/generate}）。
     * hard 模式下必须配置，否则签发接口报错，提示改用 LicenseGenerateTool CLI；
     * soft 模式下可留空，签发的 License 使用 "LOCAL-GENERATED" 占位签名。
     * <p><b>安全提示</b>：生产环境建议将私钥放在独立配置中心或环境变量，禁止提交到代码仓库。
     */
    private String privateKey = "";

    /**
     * 是否校验机器指纹（hard模式下强制为true）
     * soft模式下可设为false方便开发环境切换
     */
    private Boolean verifyFingerprint = false;

    /**
     * 是否允许通过API明文切换版本（仅soft模式有效）
     * hard模式下此选项无效，始终禁止明文切换
     */
    private Boolean allowPlainSwitch = true;

    /**
     * 判断是否为加固模式
     */
    public boolean isHardMode() {
        return "hard".equalsIgnoreCase(enforceMode);
    }

    /**
     * 判断是否为软限制模式
     */
    public boolean isSoftMode() {
        return !isHardMode();
    }
}
