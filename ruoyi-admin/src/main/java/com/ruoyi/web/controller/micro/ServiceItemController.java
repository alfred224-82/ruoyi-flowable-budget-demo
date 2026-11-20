package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ServiceItemBo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.service.IServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 服务项目管理Controller（T15.4/T15.11，小微版）
 * <p>① 服务项目 CRUD（名称+价格+单位+计价方式）；② 停用/启用（上架/下架）；③ 改价确认（自动写 price_change_log）。
 * <p>perms 对齐 M15_06 种子菜单：micro:service:list / add / edit / toggle / remove；
 * 老板小程序「改价确认」独立权限 micro:service:editConfirm（改价必留痕，见 ServiceItemServiceImpl）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/service")
public class ServiceItemController extends BaseController {

    private final IServiceItemService serviceItemService;

    /**
     * 服务项目列表
     */
    @SaCheckPermission("micro:service:list")
    @GetMapping("/list")
    public TableDataInfo<ServiceItemVo> list(ServiceItemBo bo, PageQuery pageQuery) {
        return serviceItemService.queryPageList(bo, pageQuery);
    }

    /**
     * 上架服务项目（顾客店铺页展示，status=0；categoryId 可空=全部）
     * ★SaMode.OR：预约/结算页面下拉数据源用 micro:booking:list 放行，
     *   避免将 micro:service:list（含完整服务管理菜单）授予 boss 导致侧边栏污染。
     */
    @SaCheckPermission(value = {"micro:service:list", "micro:booking:list"}, mode = SaMode.OR)
    @GetMapping("/onSale")
    public R<List<ServiceItemVo>> onSale(@RequestParam(required = false) Long categoryId) {
        return R.ok(serviceItemService.listOnSale(categoryId));
    }

    /**
     * 服务项目详情
     */
    @SaCheckPermission("micro:service:list")
    @GetMapping("/{id}")
    public R<ServiceItemVo> getInfo(@PathVariable Long id) {
        return R.ok(serviceItemService.queryById(id));
    }

    /**
     * ① 新建服务项目
     */
    @SaCheckPermission("micro:service:add")
    @Log(title = "服务项目", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ServiceItemBo bo) {
        return R.ok(serviceItemService.insert(bo));
    }

    /**
     * ① 编辑服务项目（★价格变更自动写 price_change_log）
     */
    @SaCheckPermission("micro:service:edit")
    @Log(title = "服务项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ServiceItemBo bo) {
        return toAjax(serviceItemService.update(bo));
    }

    /**
     * ③ 改价确认（老板小程序）：更新单价并写 price_change_log
     */
    @SaCheckPermission("micro:service:editConfirm")
    @Log(title = "服务项目改价", businessType = BusinessType.UPDATE)
    @PutMapping("/changePrice")
    public R<Void> changePrice(@RequestParam Long id,
                               @RequestParam BigDecimal newPrice,
                               @RequestParam(required = false) String remark) {
        return toAjax(serviceItemService.changePrice(id, newPrice, remark));
    }

    /**
     * ② 上架/下架（status：0上架 1下架）
     */
    @SaCheckPermission("micro:service:toggle")
    @Log(title = "服务项目", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(serviceItemService.toggle(id, status));
    }

    /**
     * 删除服务项目
     */
    @SaCheckPermission("micro:service:remove")
    @Log(title = "服务项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(serviceItemService.deleteByIds(Arrays.asList(ids)));
    }
}
