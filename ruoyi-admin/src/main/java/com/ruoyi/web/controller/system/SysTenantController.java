package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.bo.SysTenantConfigBo;
import com.ruoyi.system.domain.vo.SysTenantConfigVo;
import com.ruoyi.system.domain.vo.SysTenantVersionLogVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户管理Controller
 * 设计文档 Phase 16 Task 16.6
 * 注：所有接口仅限 admin 超级管理员角色访问
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/tenant")
public class SysTenantController extends BaseController {

    private final ISysTenantService tenantService;
    private final ILicenseService licenseService;

    /**
     * 租户列表
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/list")
    public TableDataInfo<SysTenantVo> list(SysTenantBo bo, PageQuery pageQuery) {
        return tenantService.queryPageList(bo, pageQuery);
    }

    /**
     * 租户详情
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}")
    public R<SysTenantVo> getInfo(@PathVariable Long id) {
        return R.ok(tenantService.queryById(id));
    }

    /**
     * 创建租户
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:add")
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysTenantBo bo) {
        return toAjax(tenantService.insertTenant(bo));
    }

    /**
     * 编辑租户
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysTenantBo bo) {
        return toAjax(tenantService.updateTenant(bo));
    }

    /**
     * 启用/停用租户
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return toAjax(tenantService.updateStatus(id, status));
    }

    /**
     * 分配版本
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/assignVersion/{id}")
    public R<Void> assignVersion(@PathVariable Long id, @RequestParam String edition) {
        return toAjax(tenantService.assignVersion(id, edition));
    }

    /**
     * ★T15.26 分配/变更版本档位（lite→pro 升级零迁移：UPDATE version_type + 刷新菜单缓存）
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/assignVersionType/{id}")
    public R<Void> assignVersionType(@PathVariable Long id, @RequestParam String versionType) {
        return toAjax(tenantService.assignVersionType(id, versionType));
    }

    /**
     * 版本变更日志
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/versionLog/{id}")
    public R<List<SysTenantVersionLogVo>> versionLog(@PathVariable Long id) {
        return R.ok(tenantService.getVersionLogs(id));
    }

    /**
     * 获取功能配置
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/config/{id}")
    public R<SysTenantConfigVo> getConfig(@PathVariable Long id, @RequestParam String configKey) {
        return R.ok(tenantService.getConfig(id, configKey));
    }

    /**
     * 更新功能配置
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/config/{id}")
    public R<Void> saveConfig(@PathVariable Long id, @Validated @RequestBody SysTenantConfigBo bo) {
        return toAjax(tenantService.saveConfig(id, bo));
    }

    /**
     * 用量统计
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/usage/{id}")
    public R<Map<String, Object>> usage(@PathVariable Long id) {
        return R.ok(tenantService.getUsageStats(id));
    }

    /**
     * 导入 License（T3.11）
     * platform_admin 为租户导入 License，激活租户功能
     * 同步更新租户 expire_date 和 edition
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "License导入", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/importLicense")
    public R<Void> importLicense(@PathVariable Long id, @RequestBody String licenseJson) {
        licenseService.importLicenseForTenant(id, licenseJson);
        return R.ok();
    }

    /**
     * 查看租户 License 信息（T3.14）
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}/license")
    public R<ILicenseService.LicenseInfoVo> tenantLicense(@PathVariable Long id) {
        return R.ok(licenseService.getTenantLicenseInfo(id));
    }

    /**
     * 租户选择器（T3.17）
     * 返回按版本分组的租户列表，支持名称模糊搜索
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/tenantSelector")
    public R<Map<String, java.util.List<Map<String, Object>>>> tenantSelector(@RequestParam(required = false) String name) {
        return R.ok(licenseService.getTenantSelector(name));
    }
}
