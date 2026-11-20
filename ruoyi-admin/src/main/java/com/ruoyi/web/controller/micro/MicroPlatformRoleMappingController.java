package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.TenantRoleMappingBo;
import com.ruoyi.system.domain.vo.TenantRoleMappingVo;
import com.ruoyi.system.service.ITenantRoleMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * platform_admin 角色映射Controller（T15.33，租户管理员三件套之二）
 * <p>platform_admin 查看/编辑本租户角色映射（显示名/启停），复用 TenantRoleMappingService。
 * <p>perms：micro:platform:roleMapping（M15_17 种子菜单）。
 * <p>★与 /micro/tenantRole 共用底层 Service，仅 URL 路径不同（platform_admin 菜单独立入口）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/roleMapping")
public class MicroPlatformRoleMappingController extends BaseController {

    private final ITenantRoleMappingService tenantRoleMappingService;

    /**
     * 本租户角色映射列表
     */
    @SaCheckPermission("micro:platform:roleMapping")
    @GetMapping("/list")
    public TableDataInfo<TenantRoleMappingVo> list(TenantRoleMappingBo bo, PageQuery pageQuery) {
        return tenantRoleMappingService.queryPageList(bo, pageQuery);
    }

    /**
     * 标准角色集挑选列表（合并本租户映射状态）
     */
    @SaCheckPermission("micro:platform:roleMapping")
    @GetMapping("/standardRoles")
    public R<List<TenantRoleMappingVo>> standardRoles() {
        return R.ok(tenantRoleMappingService.listStandardRoles());
    }

    /**
     * 改名（display_name）
     */
    @SaCheckPermission("micro:platform:roleMapping")
    @Log(title = "平台角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/rename")
    public R<Void> rename(@RequestBody TenantRoleMappingBo bo) {
        return toAjax(tenantRoleMappingService.rename(bo));
    }

    /**
     * 启用/禁用（status：0启用 1禁用）
     */
    @SaCheckPermission("micro:platform:roleMapping")
    @Log(title = "平台角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(tenantRoleMappingService.toggle(id, status));
    }
}
