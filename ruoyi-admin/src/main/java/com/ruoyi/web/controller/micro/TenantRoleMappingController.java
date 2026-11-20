package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.TenantRoleMappingBo;
import com.ruoyi.system.domain.vo.TenantRoleMappingVo;
import com.ruoyi.system.service.ITenantRoleMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务角色管理Controller（T15.9 平台管理员角色映射管理）
 * <p>平台管理员从标准角色集勾选本租户角色 → 改名 → 启停 → 排序；★不能新建角色 code、不能改角色权限（菜单/区块）。
 * <p>perms 对齐 M15_06 种子菜单：列表 micro:tenantRole:list；改名/挑选/排序归 micro:tenantRole:rename；启停 micro:tenantRole:toggle。
 * 企业版高级版与小微版共用本套映射接口，改映射后无需重启即生效。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/tenantRole")
public class TenantRoleMappingController extends BaseController {

    private final ITenantRoleMappingService tenantRoleMappingService;

    /**
     * 本租户已挑选角色映射列表
     */
    @SaCheckPermission("micro:tenantRole:list")
    @GetMapping("/list")
    public TableDataInfo<TenantRoleMappingVo> list(TenantRoleMappingBo bo, PageQuery pageQuery) {
        return tenantRoleMappingService.queryPageList(bo, pageQuery);
    }

    /**
     * ★本租户角色显示名映射（role_code → display_name）：供各端页面按角色映射显示行业术语
     * <p>登录可读、不校验权限（同 /micro/platform/theme/current 模式）；boss/顾客端页面据此显示
     * 助教/导游/教练等行业名，租户未配置映射时由前端回退通用名（员工/顾客）。
     */
    @GetMapping("/displayNames")
    public R<Map<String, String>> displayNames() {
        return R.ok(tenantRoleMappingService.roleDisplayNameMap());
    }

    /**
     * 标准角色集挑选列表（合并本租户映射状态，未挑选者 id=null）
     */
    @SaCheckPermission("micro:tenantRole:list")
    @GetMapping("/standardRoles")
    public R<List<TenantRoleMappingVo>> standardRoles() {
        return R.ok(tenantRoleMappingService.listStandardRoles());
    }

    /**
     * ★T15.25「可选角色列表」（角色可见性硬隔离）：数据源层按本租户 tenant_type 过滤 sys_role.tenant_type_scope，
     * 仅返回 scope IN (tenant_type,'both') 的启用标准角色。小微租户看不到 manager/employee/finance，企业租户看不到 salesman/customer。
     */
    @SaCheckPermission("micro:tenantRole:list")
    @GetMapping("/optional-list")
    public R<List<TenantRoleMappingVo>> optionalList() {
        return R.ok(tenantRoleMappingService.listOptionalRoles());
    }

    /**
     * ① 挑选标准角色（批量写入本租户映射，幂等跳过已存在）
     */
    @SaCheckPermission("micro:tenantRole:rename")
    @Log(title = "业务角色映射", businessType = BusinessType.INSERT)
    @PostMapping("/select")
    public R<Void> select(@RequestBody List<String> roleCodes) {
        return toAjax(tenantRoleMappingService.selectRoles(roleCodes));
    }

    /**
     * ② 改名（display_name，如 salesman→"助教"）
     */
    @SaCheckPermission("micro:tenantRole:rename")
    @Log(title = "业务角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/rename")
    public R<Void> rename(@Validated(AddGroup.class) @RequestBody TenantRoleMappingBo bo) {
        return toAjax(tenantRoleMappingService.rename(bo));
    }

    /**
     * ③ 启用/禁用（status：0启用 1禁用）
     */
    @SaCheckPermission("micro:tenantRole:toggle")
    @Log(title = "业务角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(tenantRoleMappingService.toggle(id, status));
    }

    /**
     * ⑤ 调整排序（sort_order）
     */
    @SaCheckPermission("micro:tenantRole:rename")
    @Log(title = "业务角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public R<Void> sort(@RequestBody TenantRoleMappingBo bo) {
        return toAjax(tenantRoleMappingService.updateSort(bo));
    }
}
