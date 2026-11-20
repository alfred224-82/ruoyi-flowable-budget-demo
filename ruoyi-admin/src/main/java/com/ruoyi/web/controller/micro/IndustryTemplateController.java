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
import com.ruoyi.system.domain.bo.IndustryRoleMapBo;
import com.ruoyi.system.domain.bo.IndustryTemplateBo;
import com.ruoyi.system.domain.vo.IndustryRoleMapVo;
import com.ruoyi.system.domain.vo.IndustryTemplateVo;
import com.ruoyi.system.service.IIndustryTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 行业模板管理Controller（T15.10，admin PC 界面，小微版专属）
 * <p>① 行业模板 CRUD；② 配置角色映射（业务角色名→标准角色 code）；④ 租户创建按行业初始化角色映射。
 * ★admin 维护系统级共享模板；新增行业只需配模板，小程序零改动。
 * <p>perms 对齐 M15_06 种子菜单：system:industry:list / add / edit / roleMap（未种子化的删除归 edit）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/industry")
public class IndustryTemplateController extends BaseController {

    private final IIndustryTemplateService industryTemplateService;

    /**
     * 行业模板列表
     */
    @SaCheckPermission("system:industry:list")
    @GetMapping("/list")
    public TableDataInfo<IndustryTemplateVo> list(IndustryTemplateBo bo, PageQuery pageQuery) {
        return industryTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 全部启用行业（新建小微版租户时的行业选择下拉）
     */
    @SaCheckPermission("system:industry:list")
    @GetMapping("/all")
    public R<List<IndustryTemplateVo>> all() {
        return R.ok(industryTemplateService.listEnabled());
    }

    /**
     * 行业模板详情
     */
    @SaCheckPermission("system:industry:list")
    @GetMapping("/{id}")
    public R<IndustryTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(industryTemplateService.queryById(id));
    }

    /**
     * ① 新建行业模板
     */
    @SaCheckPermission("system:industry:add")
    @Log(title = "行业模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody IndustryTemplateBo bo) {
        return R.ok(industryTemplateService.insert(bo));
    }

    /**
     * ①③ 编辑行业模板（含 preset_config 预置数据）
     */
    @SaCheckPermission("system:industry:edit")
    @Log(title = "行业模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody IndustryTemplateBo bo) {
        return toAjax(industryTemplateService.update(bo));
    }

    /**
     * 删除行业模板
     */
    @SaCheckPermission("system:industry:edit")
    @Log(title = "行业模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(industryTemplateService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * ② 查询指定行业的角色映射列表
     */
    @SaCheckPermission("system:industry:roleMap")
    @GetMapping("/roleMap/{industryCode}")
    public R<List<IndustryRoleMapVo>> roleMaps(@PathVariable String industryCode) {
        return R.ok(industryTemplateService.listRoleMaps(industryCode));
    }

    /**
     * ② 保存指定行业的角色映射（整表替换）
     */
    @SaCheckPermission("system:industry:roleMap")
    @Log(title = "行业角色映射", businessType = BusinessType.UPDATE)
    @PutMapping("/roleMap/{industryCode}")
    public R<Void> saveRoleMaps(@PathVariable String industryCode,
                                @RequestBody List<IndustryRoleMapBo> roleMaps) {
        return toAjax(industryTemplateService.saveRoleMaps(industryCode, roleMaps));
    }

    /**
     * ④ 按行业模板初始化目标租户角色映射（新建小微版租户时调用，幂等）
     */
    @SaCheckPermission("system:industry:edit")
    @Log(title = "行业模板初始化租户", businessType = BusinessType.INSERT)
    @PostMapping("/initTenant")
    public R<Integer> initTenant(@RequestParam Long tenantId, @RequestParam String industryCode) {
        return R.ok(industryTemplateService.initTenantByIndustry(tenantId, industryCode));
    }
}
