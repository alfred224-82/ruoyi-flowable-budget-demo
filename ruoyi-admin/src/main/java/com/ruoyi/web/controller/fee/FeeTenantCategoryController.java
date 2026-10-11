package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeeTenantCategoryVo;
import com.ruoyi.system.service.IFeeTenantCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户大类启用管理Controller（T5.13 三层角色分工）
 * platform_admin 从 admin 定义的大类中勾选租户需要启用的大类
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/tenantCategory")
@RequireFeature("FEAT_SUBJECT_3LAYER")
public class FeeTenantCategoryController extends BaseController {

    private final IFeeTenantCategoryService tenantCategoryService;

    /**
     * 查询当前租户大类启用配置（含未启用，platform_admin 勾选管理用）
     */
    @SaCheckPermission("fee:tenantCategory:list")
    @GetMapping("/list")
    public R<List<FeeTenantCategoryVo>> list() {
        return R.ok(tenantCategoryService.queryTenantCategoryList());
    }

    /**
     * 查询当前租户已启用的大类（仅启用状态，业务下拉用）
     */
    @GetMapping("/enabledList")
    public R<List<FeeTenantCategoryVo>> enabledList() {
        return R.ok(tenantCategoryService.queryEnabledList());
    }

    /**
     * 批量保存租户大类启用配置（整体替换，platform_admin 勾选提交）
     */
    @SaCheckPermission("fee:tenantCategory:edit")
    @Log(title = "租户大类启用配置", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public R<Void> save(@RequestBody List<String> categoryCodes) {
        tenantCategoryService.saveTenantCategories(categoryCodes);
        return R.ok();
    }
}
