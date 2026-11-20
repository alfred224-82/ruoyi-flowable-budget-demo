package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.RoleTemplateBo;
import com.ruoyi.system.domain.vo.RoleTemplateVo;
import com.ruoyi.system.service.IRoleTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 角色模板管理Controller（T15.7，admin 维护系统级共享配方）
 * <p>按 version_type × role_code 绑定菜单(menu_keys)/区块(block_keys)配方；运行时供菜单加载按角色 + 版本取配方。
 * ★系统级共享表，仅 admin 可维护，平台管理员无权修改（无 system:roleTemplate:* 权限）。
 * <p>perms 对齐 M15_06 种子菜单：查询 system:roleTemplate:list；新建/编辑/删除归 system:roleTemplate:add；绑定菜单 system:roleTemplate:bindMenu。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/roleTemplate")
public class RoleTemplateController extends BaseController {

    private final IRoleTemplateService roleTemplateService;

    /**
     * 角色模板列表
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<RoleTemplateVo> list(RoleTemplateBo bo, PageQuery pageQuery) {
        return roleTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 按版本类型列出配方（如 micro 版 boss/salesman/customer）
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/version/{versionType}")
    public R<List<RoleTemplateVo>> listByVersion(@PathVariable String versionType) {
        return R.ok(roleTemplateService.listByVersion(versionType));
    }

    /**
     * 运行时按「版本类型 + 标准角色」取唯一配方（菜单加载数据源）
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/recipe")
    public R<RoleTemplateVo> recipe(@RequestParam String versionType, @RequestParam String roleCode) {
        return R.ok(roleTemplateService.getByVersionAndRole(versionType, roleCode));
    }

    /**
     * 模板详情
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/{id}")
    public R<RoleTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(roleTemplateService.queryById(id));
    }

    /**
     * 新建模板（version_type + role_code 唯一）
     */
    @SaCheckPermission("system:roleTemplate:add")
    @Log(title = "角色模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody RoleTemplateBo bo) {
        return R.ok(roleTemplateService.insert(bo));
    }

    /**
     * 编辑模板（身份字段 version_type/role_code 不可改）
     */
    @SaCheckPermission("system:roleTemplate:add")
    @Log(title = "角色模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RoleTemplateBo bo) {
        return toAjax(roleTemplateService.update(bo));
    }

    /**
     * 绑定菜单/区块（仅更新 menu_keys / block_keys）
     */
    @SaCheckPermission("system:roleTemplate:bindMenu")
    @Log(title = "角色模板", businessType = BusinessType.UPDATE)
    @PutMapping("/bindMenu")
    public R<Void> bindMenu(@RequestBody RoleTemplateBo bo) {
        return toAjax(roleTemplateService.bindMenu(bo));
    }

    /**
     * 删除模板（物理删除，系统级表无 del_flag）
     */
    @SaCheckPermission("system:roleTemplate:add")
    @Log(title = "角色模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(roleTemplateService.deleteByIds(Arrays.asList(ids)));
    }
}
