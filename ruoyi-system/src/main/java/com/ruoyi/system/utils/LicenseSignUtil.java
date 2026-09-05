package com.ruoyi.system.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * License RSA-SHA256 签名/验签工具
 * 
 * 签名规则：
 * 1. 将License核心字段按固定顺序拼接为待签名字符串
 * 2. 使用RSA私钥(SHA256withRSA)签名 → Base64编码存入signature字段
 * 3. 验证时用公钥验签，任何字段被篡改都会导致验签失败
 *
 * @author ruoyi
 * @date 2026-07-25
 */
@Slf4j
public class LicenseSignUtil {

    private static final String ALGORITHM = "RSA";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成RSA密钥对（用于初始化部署）
     * @return [0]=publicKey(Base64), [1]=privateKey(Base64)
     */
    public static String[] generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return new String[]{publicKey, privateKey};
        } catch (Exception e) {
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 构建待签名内容（字段顺序固定，确保签名一致性）
     * 格式: licenseId|edition|licenseType|customer|features|industries|expiresAt|machineFingerprint|licenseVersion
     */
    public static String buildSignContent(String licenseId, String edition, String licenseType,
                                          String customer, String features, String industries,
                                          String expiresAt, String machineFingerprint, Integer licenseVersion) {
        return nullToEmpty(licenseId) + "|"
            + nullToEmpty(edition) + "|"
            + nullToEmpty(licenseType) + "|"
            + nullToEmpty(customer) + "|"
            + nullToEmpty(features) + "|"
            + nullToEmpty(industries) + "|"
            + nullToEmpty(expiresAt) + "|"
            + nullToEmpty(machineFingerprint) + "|"
            + (licenseVersion != null ? licenseVersion : "");
    }

    /**
     * 使用私钥签名
     * @param content 待签名内容
     * @param privateKeyBase64 私钥(Base64编码)
     * @return 签名(Base64编码)
     */
    public static String sign(String content, String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException("License签名失败", e);
        }
    }

    /**
     * 使用公钥验签
     * @param content 待验证内容
     * @param signatureBase64 签名(Base64编码)
     * @param publicKeyBase64 公钥(Base64编码)
     * @return true=验签通过 false=验签失败(被篡改)
     */
    public static boolean verify(String content, String signatureBase64, String publicKeyBase64) {
        try {
            if (content == null || signatureBase64 == null || publicKeyBase64 == null) {
                return false;
            }
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signBytes);
        } catch (Exception e) {
            log.warn("[LicenseSign] 验签异常: {}", e.getMessage());
            return false;
        }
    }

    private static String nullToEmpty(String val) {
        return val != null ? val : "";
    }
}
