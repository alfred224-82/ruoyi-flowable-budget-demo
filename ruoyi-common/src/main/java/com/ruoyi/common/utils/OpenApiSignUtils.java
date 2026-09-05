package com.ruoyi.common.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;

/**
 * 开放平台 API 签名工具（T7.8 / FEAT_OPEN_API）
 * <p>签名算法：HMAC-SHA256(appSecret, appKey + timestamp + nonce)，Hex 小写输出；
 * 防重放：时间戳窗口校验（|now - timestamp| &lt;= 窗口）；同时提供 AppKey/AppSecret/Nonce 生成。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public class OpenApiSignUtils {

    /** 请求头：AppKey */
    public static final String HEADER_APP_KEY = "X-App-Key";
    /** 请求头：时间戳(毫秒) */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    /** 请求头：随机串(防重放) */
    public static final String HEADER_NONCE = "X-Nonce";
    /** 请求头：签名 */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /** 默认防重放时间窗口：5 分钟 */
    public static final long DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L;

    private OpenApiSignUtils() {
    }

    /**
     * 生成 AppKey（对外唯一标识）
     */
    public static String generateAppKey() {
        return "ak_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 生成 AppSecret 明文（64 位，加密存储，仅创建/重置时一次性返回）
     */
    public static String generateAppSecret() {
        return IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
    }

    /**
     * 生成随机串（防重放）
     */
    public static String generateNonce() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 计算签名：HMAC-SHA256(appSecret, appKey + timestamp + nonce)
     *
     * @param appSecret 明文密钥
     * @param appKey    AppKey
     * @param timestamp 时间戳(毫秒)
     * @param nonce     随机串
     * @return Hex 小写签名
     */
    public static String sign(String appSecret, String appKey, String timestamp, String nonce) {
        String payload = StrUtil.nullToEmpty(appKey) + StrUtil.nullToEmpty(timestamp) + StrUtil.nullToEmpty(nonce);
        return SecureUtil.hmacSha256(appSecret.getBytes(StandardCharsets.UTF_8)).digestHex(payload);
    }

    /**
     * 校验签名（大小写不敏感）；任一要素为空即失败
     */
    public static boolean verify(String appSecret, String appKey, String timestamp, String nonce, String signature) {
        if (StrUtil.hasBlank(appSecret, signature, timestamp, nonce)) {
            return false;
        }
        return sign(appSecret, appKey, timestamp, nonce).equalsIgnoreCase(signature.trim());
    }

    /**
     * 时间戳防重放校验：|now - timestamp| &lt;= timeoutMs
     */
    public static boolean isTimestampValid(String timestamp, long timeoutMs) {
        if (StrUtil.isBlank(timestamp)) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp.trim());
            return Math.abs(System.currentTimeMillis() - ts) <= timeoutMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
