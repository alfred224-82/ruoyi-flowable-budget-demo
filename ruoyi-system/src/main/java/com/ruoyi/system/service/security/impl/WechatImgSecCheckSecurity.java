package com.ruoyi.system.service.security.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.service.IImageComplianceService;
import com.ruoyi.system.service.security.ContentCheckResult;
import com.ruoyi.system.service.security.IContentSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 微信图片内容安全审核实现（T12.4 真实接入，覆盖 {@link ContentSecurityBlacklistChecker} 桩）。
 * <p>扩展点落地：标注 {@code @Primary} 后，{@link com.ruoyi.system.service.IContentSecurityService}
 * 注入的 {@link IContentSecurity} 即为本类，业务侧（ThemeDecorationServiceImpl）零改动；
 * 审核结果统一由 {@code ContentSecurityServiceImpl} 写入 {@code biz_content_security_log}
 * （provider=wechat_imgSecCheck），无需另建 img_sec_check_log 表。
 * <p>接口：{@code POST https://api.weixin.qq.com/wxa/img_sec_check?access_token=xxx}（multipart media=图片二进制）；
 * 返回 {@code errcode=0} 通过、{@code errcode=87014} 含违规内容拒绝。
 * <p>access_token 取自 {@code wx.miniapp.appid/secret}，Redis 缓存 110 分钟（微信官方 7200s）。
 * <p>★降级策略：未配置 appid/secret、开关关闭、或微信侧基础设施异常（网络/token 失效/图片超限等
 * 非 87014 错误码）时一律「降级放行」并留 warn 日志，避免微信故障阻断活动发布主流程；
 * 仅当微信明确判定内容违规（87014）时 fail-closed 拒绝。可通过
 * {@code content.security.img-check-fail-open=false} 切换为基础设施异常也拒绝（严格合规模式）。
 * <p>文本审核仍委派既有黑名单实现（本类只负责图片真实 imgSecCheck）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Primary
@Component("wechatImgSecCheckSecurity")
public class WechatImgSecCheckSecurity implements IContentSecurity {

    /** 审核提供方标识（写入 biz_content_security_log.provider） */
    private static final String PROVIDER = "wechat_imgSecCheck";

    /** 微信获取接口调用凭据 access_token */
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    /** 微信图片内容安全识别接口（小程序） */
    private static final String IMG_SEC_CHECK_URL = "https://api.weixin.qq.com/wxa/img_sec_check";

    /** access_token Redis 缓存键（小程序） */
    private static final String REDIS_TOKEN_KEY = "wx:miniapp:access_token";

    /** 请求超时（毫秒） */
    private static final int TIMEOUT_MS = 10_000;

    /** 微信内容安全错误码：87014 = 含违规内容（图片鉴黄鉴政命中） */
    private static final int ERRCODE_RISKY = 87014;

    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

    /** 图片审核开关：默认开启；关闭时直接降级放行（不调用微信） */
    @Value("${content.security.img-check-enabled:true}")
    private boolean imgCheckEnabled;

    /** 基础设施异常时是否降级放行：true=放行（默认，保业务）；false=拒绝（严格合规） */
    @Value("${content.security.img-check-fail-open:true}")
    private boolean failOpen;

    /** 文本审核委派既有黑名单实现（@Qualifier 精确指向桩 Bean，避免 @Primary 自注入） */
    private final IContentSecurity fallbackTextChecker;

    /** M18 图片合规服务（@Lazy 避免循环依赖）：微信 imgSecCheck 前先走 M18 引擎，两路串行 */
    private final IImageComplianceService imageComplianceService;

    public WechatImgSecCheckSecurity(@Qualifier("contentSecurityChecker") IContentSecurity fallbackTextChecker,
                                     @Lazy IImageComplianceService imageComplianceService) {
        this.fallbackTextChecker = fallbackTextChecker;
        this.imageComplianceService = imageComplianceService;
    }

    @Override
    public ContentCheckResult checkImage(String scene, String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return ContentCheckResult.error(PROVIDER, "imageUrl is blank");
        }
        // ★M18 T18.6：微信 imgSecCheck 前先委派 M18 图片合规引擎（OCR+视觉两路），任一 FAIL 即拒绝
        try {
            Long tenantId = TenantHelper.getTenantId();
            ImageComplianceResult m18 = imageComplianceService.checkImageByUrl(
                imageUrl, tenantId != null ? tenantId : 0L, scene, null);
            if (m18 != null && !m18.isPass()) {
                log.warn("[ImgSecCheck] scene={} M18 图片合规命中 → REJECT reason={}", scene, m18.getFailReason());
                return ContentCheckResult.reject("image:" + StrUtil.nullToEmpty(m18.getFailReason()), PROVIDER,
                    StrUtil.maxLength(m18.getFailReason(), 500));
            }
        } catch (Exception e) {
            // M18 引擎异常不阻断微信二次校验，记 ERROR 后继续
            log.error("{\"event\":\"img_sec_check_m18_delegate_fail\",\"scene\":\"{}\",\"error\":\"{}\"}", scene, e.getMessage());
        }
        // 开关关闭或未配置小程序凭据：降级放行（不阻断业务）
        if (!imgCheckEnabled || StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            log.warn("[ImgSecCheck] 未启用或未配置 wx.miniapp.appid/secret，scene={} 降级放行", scene);
            return ContentCheckResult.pass(PROVIDER);
        }
        String accessToken = getAccessToken();
        if (StrUtil.isBlank(accessToken)) {
            return degrade(scene, "access_token 获取失败");
        }
        byte[] data;
        try {
            // 下载待检图片二进制（img_sec_check 要求 jpg/png、≤1MB、≤750x1334，业务侧上传已收紧至 ≤200KB）
            data = HttpUtil.downloadBytes(imageUrl);
        } catch (Exception e) {
            log.error("[ImgSecCheck] 下载图片失败 scene={} url={}", scene, imageUrl, e);
            return degrade(scene, "图片下载失败");
        }
        if (data == null || data.length == 0) {
            return degrade(scene, "图片内容为空");
        }
        String resp;
        try {
            resp = HttpRequest.post(IMG_SEC_CHECK_URL + "?access_token=" + accessToken)
                .form("media", data, "media.png")
                .timeout(TIMEOUT_MS)
                .execute().body();
        } catch (Exception e) {
            log.error("[ImgSecCheck] 调用微信 img_sec_check 异常 scene={}", scene, e);
            return degrade(scene, "微信接口调用异常");
        }
        JSONObject json;
        try {
            json = JSONUtil.parseObj(resp);
        } catch (Exception e) {
            log.error("[ImgSecCheck] 微信响应解析失败 scene={} resp={}", scene, StrUtil.maxLength(resp, 500));
            return degrade(scene, "微信响应解析失败");
        }
        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode == 0) {
            log.info("[ImgSecCheck] scene={} 审核通过 url={}", scene, imageUrl);
            return ContentCheckResult.pass(PROVIDER);
        }
        if (errcode != null && errcode == ERRCODE_RISKY) {
            String label = StrUtil.nullToDefault(json.getStr("errmsg"), "risky content");
            log.warn("[ImgSecCheck] scene={} 命中违规内容(87014) url={} label={}", scene, imageUrl, label);
            return ContentCheckResult.reject(label, PROVIDER, StrUtil.maxLength(resp, 1000));
        }
        // 其它错误码（40001 token 失效 / 87009 / 图片超限 / 系统繁忙等）：按 failOpen 策略处理
        log.warn("[ImgSecCheck] scene={} 微信返回非预期错误码 errcode={} errmsg={}",
            scene, errcode, json.getStr("errmsg"));
        return degrade(scene, "微信错误码 " + errcode);
    }

    @Override
    public ContentCheckResult checkText(String scene, String text) {
        // 文本审核不在本类职责内，委派既有黑名单实现（如需真实 msgSecCheck 另建 @Primary 文本 Provider）
        return fallbackTextChecker.checkText(scene, text);
    }

    /**
     * 基础设施异常降级：failOpen=true 放行（保业务），false 返回 ERROR（拦截落库，严格合规）
     */
    private ContentCheckResult degrade(String scene, String reason) {
        if (failOpen) {
            log.warn("[ImgSecCheck] scene={} 降级放行（fail-open）：{}", scene, reason);
            return ContentCheckResult.pass(PROVIDER);
        }
        log.warn("[ImgSecCheck] scene={} 降级拒绝（fail-closed）：{}", scene, reason);
        return ContentCheckResult.error(PROVIDER, reason);
    }

    /**
     * 获取小程序 access_token（Redis 缓存 110 分钟，与消息 Provider 同范式）
     */
    private String getAccessToken() {
        String cached = RedisUtils.getCacheObject(REDIS_TOKEN_KEY);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String url = TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        try {
            String resp = HttpUtil.get(url, TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            String token = json.getStr("access_token");
            if (StrUtil.isNotBlank(token)) {
                RedisUtils.setCacheObject(REDIS_TOKEN_KEY, token, Duration.ofMinutes(110));
                return token;
            }
            log.error("[ImgSecCheck] 获取 access_token 失败 resp={}", StrUtil.maxLength(resp, 500));
        } catch (Exception e) {
            log.error("[ImgSecCheck] 获取 access_token 异常", e);
        }
        return null;
    }
}
