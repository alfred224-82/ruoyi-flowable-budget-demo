package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ComplaintRecordVo;
import com.ruoyi.system.service.IComplianceAgreementService;
import com.ruoyi.system.service.IMicroSupervisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台监管 Controller（T25.4，小微版 M25）
 * <p>★platform_admin 专属：全部端点走 system:supervision:* 权限。
 * <p>不使用 @RequireFeature（平台监管为系统级能力，不受功能开关限制）。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/supervision")
public class MicroPlatformSupervisionController extends BaseController {

    private final IMicroSupervisionService supervisionService;
    private final IComplianceAgreementService agreementService;

    /**
     * 全部投诉列表（平台 Admin 跨租户查看）
     */
    @SaCheckPermission("system:supervision:view")
    @GetMapping("/complaints")
    public R<List<ComplaintRecordVo>> complaints(
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long tenantId) {
        return R.ok(supervisionService.listAllComplaints(channel, status, tenantId));
    }

    /**
     * 停止活动
     */
    @SaCheckPermission("system:supervision:stopActivity")
    @Log(title = "平台监管-停止活动", businessType = BusinessType.UPDATE)
    @PutMapping("/stopActivity/{id}")
    public R<Void> stopActivity(@PathVariable Long id) {
        supervisionService.stopActivity(id);
        return R.ok();
    }

    /**
     * 停止租户
     */
    @SaCheckPermission("system:supervision:stopTenant")
    @Log(title = "平台监管-停止租户", businessType = BusinessType.UPDATE)
    @PutMapping("/stopTenant/{id}")
    public R<Void> stopTenant(@PathVariable Long id) {
        supervisionService.stopTenant(id);
        return R.ok();
    }

    /**
     * 恢复租户
     */
    @SaCheckPermission("system:supervision:stopTenant")
    @Log(title = "平台监管-恢复租户", businessType = BusinessType.UPDATE)
    @PutMapping("/resumeTenant/{id}")
    public R<Void> resumeTenant(@PathVariable Long id) {
        supervisionService.resumeTenant(id);
        return R.ok();
    }

    /**
     * 发放 license
     */
    @SaCheckPermission("system:supervision:issueLicense")
    @Log(title = "平台监管-发放License", businessType = BusinessType.INSERT)
    @PostMapping("/issueLicense")
    public R<String> issueLicense(@RequestBody Map<String, Object> params) {
        return R.ok(supervisionService.issueLicense(params));
    }

    /**
     * 活动留痕查询（租户不可删除，仅平台 Admin 可管理）
     */
    @SaCheckPermission("system:supervision:activityTrace")
    @GetMapping("/activityTrace/{tenantId}")
    public R<List<Map<String, Object>>> activityTrace(
        @PathVariable(required = false) Long tenantId,
        @RequestParam(required = false) Integer limit) {
        return R.ok(supervisionService.activityTrace(tenantId, limit));
    }

    // ===== 合规协议管理（T25.5 OSS 方案） =====

    /**
     * 获取当前协议配置（平台 Admin 查看版本号 + OSS URL）
     */
    @SaCheckPermission("system:supervision:agreement")
    @GetMapping("/agreement/config")
    public R<Map<String, String>> getAgreementConfig() {
        return R.ok(agreementService.getAgreementConfig());
    }

    /**
     * 更新协议配置（平台 Admin 上传 OSS 后设置新版本号 + URL）
     * data: { version: "v1.1", contentUrl: "https://oss.../agreement_v1.1.html" }
     */
    @SaCheckPermission("system:supervision:agreement")
    @Log(title = "平台监管-更新协议配置", businessType = BusinessType.UPDATE)
    @PutMapping("/agreement/config")
    public R<Void> updateAgreementConfig(@RequestBody Map<String, String> data) {
        agreementService.updateAgreementConfig(data.get("version"), data.get("contentUrl"));
        return R.ok();
    }
}
