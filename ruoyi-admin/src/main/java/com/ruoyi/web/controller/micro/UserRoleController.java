package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.UserTenantRoleBo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色分配Controller（T15.9 ④ 分配用户，写 user_tenant_role）
 * <p>支持一人多角色、主角色设定；分配前校验角色已在本租户挑选且启用。
 * <p>perms 对齐 M15_06 种子菜单：micro:userRole:list / micro:userRole:assign / micro:userRole:remove。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/userRole")
public class UserRoleController extends BaseController {

    private final IUserTenantRoleService userTenantRoleService;

    /**
     * 本租户用户角色分配列表（回填用户账号/昵称/手机号 + 角色显示名）
     */
    @SaCheckPermission("micro:userRole:list")
    @GetMapping("/list")
    public TableDataInfo<UserTenantRoleVo> list(UserTenantRoleBo bo, PageQuery pageQuery) {
        return userTenantRoleService.queryPageList(bo, pageQuery);
    }

    /**
     * ★员工下拉数据源（预约/结算页面用）：按 roleCode 过滤本租户已分配且启用的用户
     * <p>SaMode.OR：用 micro:booking:list 或 micro:settle:list 放行，
     *   避免将 micro:userRole:list（含完整角色分配管理菜单）授予 boss 导致侧边栏污染。
     */
    @SaCheckPermission(value = {"micro:booking:list", "micro:settle:list"}, mode = SaMode.OR)
    @GetMapping("/staff")
    public TableDataInfo<UserTenantRoleVo> staff(UserTenantRoleBo bo, PageQuery pageQuery) {
        return userTenantRoleService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询指定用户在本租户下的全部角色
     */
    @SaCheckPermission("micro:userRole:list")
    @GetMapping("/user/{userId}")
    public R<List<UserTenantRoleVo>> listByUser(@PathVariable Long userId) {
        return R.ok(userTenantRoleService.listByUser(userId));
    }

    /**
     * ④ 分配用户角色（单角色 roleCode 或批量 roleCodes，幂等 upsert）
     */
    @SaCheckPermission("micro:userRole:assign")
    @Log(title = "用户角色分配", businessType = BusinessType.INSERT)
    @PostMapping("/assign")
    public R<Void> assign(@Validated(AddGroup.class) @RequestBody UserTenantRoleBo bo) {
        return toAjax(userTenantRoleService.assign(bo));
    }

    /**
     * 设置主角色（同一用户同一租户下主角色唯一）
     */
    @SaCheckPermission("micro:userRole:assign")
    @Log(title = "用户角色分配", businessType = BusinessType.UPDATE)
    @PutMapping("/primary/{id}")
    public R<Void> setPrimary(@PathVariable Long id) {
        return toAjax(userTenantRoleService.setPrimary(id));
    }

    /**
     * 取消分配（按关联ID逻辑删除）
     */
    @SaCheckPermission("micro:userRole:remove")
    @Log(title = "用户角色分配", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(userTenantRoleService.removeById(id));
    }
}
