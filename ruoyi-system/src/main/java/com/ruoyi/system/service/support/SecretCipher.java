package com.ruoyi.system.service.support;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.utils.EncryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 敏感字段加解密与脱敏工具（T13.5 / T13.6）
 * <p>open_api_credential.app_secret、tenant_payment_config.api_key 等须【加密存储】、接口返回【脱敏】。
 * 密钥来自配置 biz.secret-key（生产须覆盖为强密钥并纳入密钥管理），默认值仅供开发。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class SecretCipher {

    /** AES 密钥（规整为 16 位） */
    private final String key;

    public SecretCipher(@Value("${biz.secret-key:RyFlowPlusM13Key}") String secretKey) {
        this.key = normalizeKey(secretKey);
    }

    /**
     * 加密（明文空则返回 null）
     */
    public String encrypt(String plain) {
        if (StrUtil.isBlank(plain)) {
            return null;
        }
        return EncryptUtils.encryptByAes(plain, key);
    }

    /**
     * 解密（密文空则返回 null；解密失败视为历史明文原样返回，不阻断）
     */
    public String decrypt(String cipher) {
        if (StrUtil.isBlank(cipher)) {
            return null;
        }
        try {
            return EncryptUtils.decryptByAes(cipher, key);
        } catch (Exception e) {
            log.warn("[SecretCipher] 解密失败，按明文处理（可能为历史未加密数据）");
            return cipher;
        }
    }

    /**
     * 脱敏展示：保留前 3 位与后 2 位，中间以 * 代替；过短则全掩码
     */
    public String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int len = value.length();
        if (len <= 5) {
            return StrUtil.repeat('*', len);
        }
        return value.substring(0, 3) + StrUtil.repeat('*', len - 5) + value.substring(len - 2);
    }

    /**
     * 规整密钥为 16 位（不足右补 '0'，超出截断），满足 AES 密钥长度要求
     */
    private String normalizeKey(String secretKey) {
        String base = StrUtil.isBlank(secretKey) ? "RyFlowPlusM13Key" : secretKey;
        if (base.length() >= 16) {
            return base.substring(0, 16);
        }
        return base + StrUtil.repeat('0', 16 - base.length());
    }
}
