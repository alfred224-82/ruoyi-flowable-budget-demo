package com.ruoyi.web.controller.biz;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.OpenApiSignUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.OpenApiCredential;
import com.ruoyi.system.domain.vo.OpenApiVerifyResult;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.IOpenApiCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 开放平台外部调用网关Controller（T7.8 / FEAT_OPEN_API）
 * <p>匿名入口（application.yml security.excludes 放行 /openapi/gateway/**）：
 * 从请求头取 AppKey/时间戳/随机串/签名，调 verifyAndLog 完成 HMAC-SHA256 验签 + 时间戳防重放 + 日月配额 + 全量审计；
 * 通过后再按凭证所属租户校验 FEAT_OPEN_API 授权（专业版以下返回 403 版本不支持）。
 * <p>不依赖登录态：多租户插件在无登录上下文时不注入 tenant_id，凭证按全局唯一 AppKey 反查；
 * 业务数据须显式按凭证租户隔离，故当前仅暴露 ping echo（租户安全的连通性/鉴权自检），
 * 后续业务接口在此基础上按 result.getCredential().getTenantId() 做数据隔离即可。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/openapi/gateway")
public class OpenApiGatewayController {

    private final IOpenApiCredentialService openApiService;
    private final ILicenseService licenseService;

    /**
     * 连通性 + 鉴权自检：验签通过返回凭证所属应用与租户信息（不泄露任何业务数据）
     * <p>HTTP 状态语义：200 通过 / 401 签名或AppKey或时间戳 / 403 停用或过期或IP或版本不支持 / 429 超配额。
     */
    @GetMapping("/ping")
    public ResponseEntity<R<Map<String, Object>>> ping(
        @RequestHeader(value = OpenApiSignUtils.HEADER_APP_KEY, required = false) String appKey,
        @RequestHeader(value = OpenApiSignUtils.HEADER_TIMESTAMP, required = false) String timestamp,
        @RequestHeader(value = OpenApiSignUtils.HEADER_NONCE, required = false) String nonce,
        @RequestHeader(value = OpenApiSignUtils.HEADER_SIGNATURE, required = false) String signature,
        HttpServletRequest request) {
        String clientIp = ServletUtils.getClientIP();
        OpenApiVerifyResult result = openApiService.verifyAndLog(appKey, timestamp, nonce, signature,
            request.getRequestURI(), request.getMethod(), request.getQueryString(), clientIp);
        if (!result.isPass()) {
            return ResponseEntity.status(toHttpStatus(result.getCode()))
                .body(R.<Map<String, Object>>fail(result.getCode(), result.getMessage()));
        }
        OpenApiCredential credential = result.getCredential();
        // 专业版以下租户：功能未授权（凭证有效但所属租户未开通开放平台能力）
        if (credential == null || !licenseService.tenantHasFeature(credential.getTenantId(), "FEAT_OPEN_API")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.<Map<String, Object>>fail(403, "当前版本未开通开放平台API，请联系厂商升级"));
        }
        Map<String, Object> data = new HashMap<>(8);
        data.put("message", "pong");
        data.put("appKey", credential.getAppKey());
        data.put("appName", credential.getAppName());
        data.put("tenantId", credential.getTenantId());
        data.put("serverTime", new Date());
        return ResponseEntity.ok(R.ok(data));
    }

    /**
     * 鉴权结果码 → HTTP 状态码
     */
    private HttpStatus toHttpStatus(int code) {
        switch (code) {
            case 401:
                return HttpStatus.UNAUTHORIZED;
            case 403:
                return HttpStatus.FORBIDDEN;
            case 429:
                return HttpStatus.TOO_MANY_REQUESTS;
            default:
                return HttpStatus.OK;
        }
    }
}
