package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.OpenApiCredentialBo;
import com.ruoyi.system.domain.bo.OpenApiWebhookBo;
import com.ruoyi.system.domain.vo.OpenApiCallLogVo;
import com.ruoyi.system.domain.vo.OpenApiCredentialVo;
import com.ruoyi.system.domain.vo.OpenApiWebhookVo;
import com.ruoyi.system.service.IOpenApiCredentialService;
import com.ruoyi.system.service.IOpenApiWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 开放平台API凭证Controller（T7.8 / T13.5，专业版 FEAT_OPEN_API）
 * <p>AppKey+HMAC-SHA256 签名鉴权 / 配额限流 / IP 白名单 / 全量调用审计；appSecret 加密存储、返回脱敏，仅创建/重置时一次性明文返回。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/openapi")
@RequireFeature("FEAT_OPEN_API")
public class OpenApiController extends BaseController {

    private final IOpenApiCredentialService openApiService;
    private final IOpenApiWebhookService webhookService;

    /**
     * 凭证列表（appSecret 脱敏）
     */
    @SaCheckPermission("fee:openApi:list")
    @GetMapping("/list")
    public TableDataInfo<OpenApiCredentialVo> list(@RequestParam(required = false) String appName,
                                                   @RequestParam(required = false) String status,
                                                   PageQuery pageQuery) {
        return openApiService.queryPageList(appName, status, pageQuery);
    }

    /**
     * 凭证详情（appSecret 脱敏）
     */
    @SaCheckPermission("fee:openApi:query")
    @GetMapping("/{id}")
    public R<OpenApiCredentialVo> getInfo(@PathVariable Long id) {
        return R.ok(openApiService.queryById(id));
    }

    /**
     * 新建凭证（返回 appKey + appSecret 明文，仅此一次）
     */
    @SaCheckPermission("fee:openApi:add")
    @Log(title = "开放平台凭证", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Map<String, String>> add(@Validated(AddGroup.class) @RequestBody OpenApiCredentialBo bo) {
        return R.ok(openApiService.create(bo));
    }

    /**
     * 编辑凭证（不含 appSecret）
     */
    @SaCheckPermission("fee:openApi:edit")
    @Log(title = "开放平台凭证", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpenApiCredentialBo bo) {
        return toAjax(openApiService.update(bo));
    }

    /**
     * 重置 appSecret（返回新明文，仅此一次）
     */
    @SaCheckPermission("fee:openApi:resetSecret")
    @Log(title = "开放平台凭证", businessType = BusinessType.UPDATE)
    @PutMapping("/resetSecret/{id}")
    public R<String> resetSecret(@PathVariable Long id) {
        return R.ok(openApiService.resetSecret(id));
    }

    /**
     * 启停凭证
     */
    @SaCheckPermission("fee:openApi:edit")
    @Log(title = "开放平台凭证", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        return toAjax(openApiService.changeStatus(id, status));
    }

    /**
     * 删除凭证
     */
    @SaCheckPermission("fee:openApi:remove")
    @Log(title = "开放平台凭证", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(openApiService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 调用审计列表
     */
    @SaCheckPermission("fee:openApi:callLog")
    @GetMapping("/callLog")
    public TableDataInfo<OpenApiCallLogVo> callLog(@RequestParam(required = false) String appKey, PageQuery pageQuery) {
        return openApiService.queryCallLogPageList(appKey, pageQuery);
    }

    // ==================== Webhook 回调注册（T7.8） ====================

    /**
     * Webhook 列表（secret 脱敏，填充关联凭证 AppKey/应用名）
     */
    @SaCheckPermission("fee:openApi:webhook")
    @GetMapping("/webhook/list")
    public TableDataInfo<OpenApiWebhookVo> webhookList(@RequestParam(required = false) Long credentialId,
                                                       @RequestParam(required = false) String eventType,
                                                       PageQuery pageQuery) {
        return webhookService.queryPageList(credentialId, eventType, pageQuery);
    }

    /**
     * Webhook 详情（secret 脱敏）
     */
    @SaCheckPermission("fee:openApi:webhook")
    @GetMapping("/webhook/{id}")
    public R<OpenApiWebhookVo> webhookInfo(@PathVariable Long id) {
        return R.ok(webhookService.queryById(id));
    }

    /**
     * 新建 Webhook（返回 secret 明文，仅此一次）
     */
    @SaCheckPermission("fee:openApi:webhook")
    @Log(title = "开放平台Webhook", businessType = BusinessType.INSERT)
    @PostMapping("/webhook")
    public R<Map<String, String>> webhookAdd(@Validated(AddGroup.class) @RequestBody OpenApiWebhookBo bo) {
        return R.ok(webhookService.create(bo));
    }

    /**
     * 编辑 Webhook（不含 secret）
     */
    @SaCheckPermission("fee:openApi:webhook")
    @Log(title = "开放平台Webhook", businessType = BusinessType.UPDATE)
    @PutMapping("/webhook")
    public R<Void> webhookEdit(@Validated(EditGroup.class) @RequestBody OpenApiWebhookBo bo) {
        return toAjax(webhookService.update(bo));
    }

    /**
     * 重置 Webhook secret（返回新明文，仅此一次）
     */
    @SaCheckPermission("fee:openApi:webhook")
    @Log(title = "开放平台Webhook", businessType = BusinessType.UPDATE)
    @PutMapping("/webhook/resetSecret/{id}")
    public R<String> webhookResetSecret(@PathVariable Long id) {
        return R.ok(webhookService.resetSecret(id));
    }

    /**
     * 删除 Webhook
     */
    @SaCheckPermission("fee:openApi:webhook")
    @Log(title = "开放平台Webhook", businessType = BusinessType.DELETE)
    @DeleteMapping("/webhook/{ids}")
    public R<Void> webhookRemove(@PathVariable Long[] ids) {
        return toAjax(webhookService.deleteByIds(Arrays.asList(ids)));
    }
}
