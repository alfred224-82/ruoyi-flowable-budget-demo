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
import com.ruoyi.system.domain.bo.ServiceCategoryBo;
import com.ruoyi.system.domain.vo.ServiceCategoryVo;
import com.ruoyi.system.service.IServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 服务分类管理Controller（T15.4/T15.11，小微版）
 * <p>① 服务分类 CRUD；② 停用/启用；③ 启用分类下拉（供服务项目表单选择）。
 * <p>perms 对齐 M15_06 种子菜单：micro:category:list / add / edit / remove；停用/启用归 edit。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/category")
public class ServiceCategoryController extends BaseController {

    private final IServiceCategoryService serviceCategoryService;

    /**
     * 服务分类列表
     */
    @SaCheckPermission("micro:category:list")
    @GetMapping("/list")
    public TableDataInfo<ServiceCategoryVo> list(ServiceCategoryBo bo, PageQuery pageQuery) {
        return serviceCategoryService.queryPageList(bo, pageQuery);
    }

    /**
     * 启用分类下拉（服务项目表单选择用）
     */
    @SaCheckPermission("micro:category:list")
    @GetMapping("/enabled")
    public R<List<ServiceCategoryVo>> enabled() {
        return R.ok(serviceCategoryService.listEnabled());
    }

    /**
     * 服务分类详情
     */
    @SaCheckPermission("micro:category:list")
    @GetMapping("/{id}")
    public R<ServiceCategoryVo> getInfo(@PathVariable Long id) {
        return R.ok(serviceCategoryService.queryById(id));
    }

    /**
     * ① 新建服务分类
     */
    @SaCheckPermission("micro:category:add")
    @Log(title = "服务分类", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ServiceCategoryBo bo) {
        return R.ok(serviceCategoryService.insert(bo));
    }

    /**
     * ① 编辑服务分类
     */
    @SaCheckPermission("micro:category:edit")
    @Log(title = "服务分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ServiceCategoryBo bo) {
        return toAjax(serviceCategoryService.update(bo));
    }

    /**
     * ② 停用/启用（status：0正常 1停用）
     */
    @SaCheckPermission("micro:category:edit")
    @Log(title = "服务分类", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(serviceCategoryService.toggle(id, status));
    }

    /**
     * 删除服务分类
     */
    @SaCheckPermission("micro:category:remove")
    @Log(title = "服务分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(serviceCategoryService.deleteByIds(Arrays.asList(ids)));
    }
}
