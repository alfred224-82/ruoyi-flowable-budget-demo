package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ImageComplianceConfigBo;
import com.ruoyi.system.domain.vo.ImageComplianceConfigVo;
import com.ruoyi.system.service.ISensitiveWordAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 图片违规类型配置 Controller（M18 T18.4，platform_admin 专属）
 * <p>类级 {@code @SaCheckRole("platform_admin")} + {@code @RequireFeature("FEAT_IMAGE_COMPLIANCE")}。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/admin/imageConfig")
@SaCheckRole("platform_admin")
@RequireFeature("FEAT_IMAGE_COMPLIANCE")
public class AdminImageComplianceConfigController extends BaseController {

    private final ISensitiveWordAdminService adminService;

    /** 图片违规类型配置列表（porn/violence/politics 三行） */
    @SaCheckPermission("compliance:imageConfig:list")
    @GetMapping("/list")
    public R<List<ImageComplianceConfigVo>> list() {
        return R.ok(adminService.queryImageConfigs());
    }

    /** 保存图片违规类型配置（按 violationType upsert） */
    @SaCheckPermission("compliance:imageConfig:save")
    @Log(title = "图片违规类型配置", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@Validated @RequestBody ImageComplianceConfigBo bo) {
        return toAjax(adminService.saveImageConfig(bo));
    }

    /** 图片合规总开关（按违规类型启用/禁用） */
    @SaCheckPermission("compliance:imageConfig:toggle")
    @Log(title = "图片合规开关", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle")
    public R<Void> toggle(@RequestParam String violationType, @RequestParam Integer enabled) {
        return toAjax(adminService.toggleImageConfig(violationType, enabled));
    }
}
