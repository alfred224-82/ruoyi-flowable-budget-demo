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
import com.ruoyi.system.domain.bo.ServiceItemBo;
import com.ruoyi.system.domain.vo.ServiceCategoryVo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.service.IIndustryTemplateService;
import com.ruoyi.system.service.IServiceCategoryService;
import com.ruoyi.system.service.IServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * platform_admin 项目管理Controller（T15.38，V3.7 原子资源管理·项目）
 * <p>租户管理员维护本租户「项目」原子资源（service_item，排除空间分类项），复用 ServiceItemService。
 * <p>★数据源：首次进入本租户项目为空时，按 sys_tenant.industry 从 admin 行业模板预置数据
 * （industry_template.preset_config.projects）自愈拉取落库（幂等仅空表补录，V3.5 §16.4 脱钩原则）。
 * <p>perms：micro:platform:project（M15_23 种子菜单 7511~7513）。
 * <p>★与 /micro/service（老板·服务与定价）共用底层 Service，仅 URL 路径不同（platform_admin 菜单独立入口）。
 *
 * @author ruoyi
 * @date 2026-09-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/project")
public class MicroPlatformProjectController extends BaseController {

    private final IServiceItemService serviceItemService;
    private final IServiceCategoryService serviceCategoryService;
    private final IIndustryTemplateService industryTemplateService;

    /**
     * 项目列表（排除房间/空间项；★进入前按行业模板自愈初始化）
     */
    @SaCheckPermission("micro:platform:project")
    @GetMapping("/list")
    public TableDataInfo<ServiceItemVo> list(ServiceItemBo bo, PageQuery pageQuery) {
        industryTemplateService.selfHealCurrentTenantResources();
        return serviceItemService.queryPageListByCategoryType(bo, pageQuery, false);
    }

    /**
     * 服务分类下拉（category_type=service，项目表单选择用）
     */
    @SaCheckPermission("micro:platform:project")
    @GetMapping("/categories")
    public R<List<ServiceCategoryVo>> categories() {
        return R.ok(serviceCategoryService.listEnabledByType("service"));
    }

    /**
     * 项目详情
     */
    @SaCheckPermission("micro:platform:project")
    @GetMapping("/{id}")
    public R<ServiceItemVo> getInfo(@PathVariable Long id) {
        return R.ok(serviceItemService.queryById(id));
    }

    /**
     * 新建项目
     */
    @SaCheckPermission("micro:platform:project")
    @Log(title = "平台项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ServiceItemBo bo) {
        return R.ok(serviceItemService.insert(bo));
    }

    /**
     * 编辑项目（★价格变更自动写 price_change_log，见 ServiceItemServiceImpl）
     */
    @SaCheckPermission("micro:platform:project")
    @Log(title = "平台项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ServiceItemBo bo) {
        return toAjax(serviceItemService.update(bo));
    }

    /**
     * 上架/下架（status：0上架 1下架）
     */
    @SaCheckPermission("micro:platform:project")
    @Log(title = "平台项目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(serviceItemService.toggle(id, status));
    }

    /**
     * 删除项目
     */
    @SaCheckPermission("micro:platform:project")
    @Log(title = "平台项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(serviceItemService.deleteByIds(Arrays.asList(ids)));
    }
}
