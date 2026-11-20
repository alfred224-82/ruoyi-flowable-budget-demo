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
 * platform_admin 房间/空间管理Controller（T15.38，V3.7 原子资源管理·房间空间）
 * <p>租户管理员维护本租户「房间/空间」原子资源：房间 = 空间分类（service_category.category_type='space'）
 * 下的 service_item（元/小时按时长计价，capacity 存 description），与预约/结算（booking_order.service_item_id）天然兼容。
 * <p>★数据源：首次进入本租户项目为空时，按 sys_tenant.industry 从 admin 行业模板预置数据
 * （industry_template.preset_config.rooms，T15.33 每行业 ≥2 间）自愈拉取落库（幂等仅空表补录，V3.5 §16.4 脱钩原则）。
 * <p>perms：micro:platform:room（M15_23 种子菜单 7514~7516）。
 *
 * @author ruoyi
 * @date 2026-09-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/room")
public class MicroPlatformRoomController extends BaseController {

    private final IServiceItemService serviceItemService;
    private final IServiceCategoryService serviceCategoryService;
    private final IIndustryTemplateService industryTemplateService;

    /**
     * 房间/空间列表（仅空间分类项；★进入前按行业模板自愈初始化）
     */
    @SaCheckPermission("micro:platform:room")
    @GetMapping("/list")
    public TableDataInfo<ServiceItemVo> list(ServiceItemBo bo, PageQuery pageQuery) {
        industryTemplateService.selfHealCurrentTenantResources();
        return serviceItemService.queryPageListByCategoryType(bo, pageQuery, true);
    }

    /**
     * 空间分类下拉（category_type=space，房间表单选择用）
     */
    @SaCheckPermission("micro:platform:room")
    @GetMapping("/categories")
    public R<List<ServiceCategoryVo>> categories() {
        return R.ok(serviceCategoryService.listEnabledByType("space"));
    }

    /**
     * 房间详情
     */
    @SaCheckPermission("micro:platform:room")
    @GetMapping("/{id}")
    public R<ServiceItemVo> getInfo(@PathVariable Long id) {
        return R.ok(serviceItemService.queryById(id));
    }

    /**
     * 新建房间/空间（未指定分类时自动挂到本租户空间分类，无则创建默认「房间/空间」分类）
     */
    @SaCheckPermission("micro:platform:room")
    @Log(title = "平台房间空间管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ServiceItemBo bo) {
        if (bo.getCategoryId() == null) {
            bo.setCategoryId(serviceCategoryService.ensureSpaceCategory());
        }
        return R.ok(serviceItemService.insert(bo));
    }

    /**
     * 编辑房间/空间（★价格变更自动写 price_change_log，见 ServiceItemServiceImpl）
     */
    @SaCheckPermission("micro:platform:room")
    @Log(title = "平台房间空间管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ServiceItemBo bo) {
        return toAjax(serviceItemService.update(bo));
    }

    /**
     * 上架/下架（status：0启用 1停用）
     */
    @SaCheckPermission("micro:platform:room")
    @Log(title = "平台房间空间管理", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(serviceItemService.toggle(id, status));
    }

    /**
     * 删除房间/空间
     */
    @SaCheckPermission("micro:platform:room")
    @Log(title = "平台房间空间管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(serviceItemService.deleteByIds(Arrays.asList(ids)));
    }
}
