package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.ILicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * License授权管理Controller
 * 提供版本信息查询、版本切换、机器指纹导出等端点
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/license")
public class LicenseController extends BaseController {

    private final ILicenseService licenseService;

    /**
     * 查看当前授权信息（前端根据此接口动态渲染菜单/按钮）
     */
    @GetMapping("/info")
    public R<ILicenseService.LicenseInfoVo> info() {
        return R.ok(licenseService.getLicenseInfo());
    }

    /**
     * 导出机器指纹（用于申请License）
     * 注意：必须使用 ok(msg, data) 双参版本，避免 R.ok(String) 重载将内容放入msg而非data
     */
    @SaCheckPermission("system:license:manage")
    @GetMapping("/fingerprint")
    public R<String> fingerprint() {
        return R.ok("操作成功", licenseService.exportFingerprint());
    }

    /**
     * 替换License / 切换版本
     * 传入JSON格式：{"edition":"ADVANCED"} 即可切换版本
     * 或传入完整License JSON
     */
    @SaCheckPermission("system:license:manage")
    @PostMapping("/replace")
    public R<Void> replace(@RequestBody String licenseJson) {
        licenseService.replaceLicense(licenseJson);
        return R.ok();
    }

    /**
     * 试用剩余时长
     */
    @GetMapping("/trial-status")
    public R<Long> trialStatus() {
        return R.ok(licenseService.getTrialRemainingHours());
    }

    /**
     * 获取当前版本等级
     * 注意：必须使用 ok(msg, data) 双参版本，避免 R.ok(String) 重载将内容放入msg而非data
     */
    @GetMapping("/edition")
    public R<String> edition() {
        return R.ok("操作成功", licenseService.getCurrentEdition());
    }
}
