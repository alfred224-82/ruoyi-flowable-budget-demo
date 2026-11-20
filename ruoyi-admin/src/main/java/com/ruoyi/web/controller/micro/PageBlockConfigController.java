package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PageBlockConfigBo;
import com.ruoyi.system.domain.bo.PageDecorateBo;
import com.ruoyi.system.domain.vo.PageBlockConfigVo;
import com.ruoyi.system.service.IPageBlockConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 页面装修Controller（T15.17，老板 / 平台管理员 PC 后台）
 * <p>① 选择页面(home/mine/booking/report) + 角色(boss/salesman/customer)；② 从组件库勾选区块、拖拽排序、设参数(JSON)；
 * ③ 批量保存（整页全量覆盖，小程序实时生效）；④ 恢复默认（从 tenant_id=0 系统模板克隆）。
 * <p>★老板只做「个性化排版」，全程不涉及菜单/perms/区块权限配置；权限是安全底线，配置永不突破权限（见 /page/config 二次过滤）。
 * <p>★perms 说明：M15_06 未预置「页面装修」种子菜单，本控制器采用 micro:page:list/add/edit/remove 约定权限——
 * 超管（*:*:*）可直接管理 tenant_id=0 系统模板；老板角色需由 role_template / 后续菜单补种相应 micro:page:* 后方可访问。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/page")
public class PageBlockConfigController extends BaseController {

    private final IPageBlockConfigService pageBlockConfigService;

    /**
     * 装修面板：查某角色某页面的原始区块配置（不过滤权限，按 sortOrder）
     *
     * @param tenantId 目标租户（仅超管可指定，默认 0=系统默认模板；非超管忽略，强制本租户）
     */
    @SaCheckPermission("micro:page:list")
    @GetMapping("/list")
    public R<List<PageBlockConfigVo>> list(@RequestParam String roleCode,
                                           @RequestParam String pageKey,
                                           @RequestParam(required = false) Long tenantId) {
        return R.ok(pageBlockConfigService.listByRolePage(roleCode, pageKey, tenantId));
    }

    /**
     * 区块详情
     */
    @SaCheckPermission("micro:page:list")
    @GetMapping("/{id}")
    public R<PageBlockConfigVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(pageBlockConfigService.queryById(id));
    }

    /**
     * 批量保存装修（勾选/拖拽排序/设参数 → 整页全量覆盖，先删后插）
     */
    @SaCheckPermission("micro:page:edit")
    @Log(title = "页面装修", businessType = BusinessType.UPDATE)
    @PostMapping("/decorate")
    public R<Void> decorate(@Validated(AddGroup.class) @RequestBody PageDecorateBo bo) {
        pageBlockConfigService.saveConfig(bo);
        return R.ok();
    }

    /**
     * 恢复默认（从 tenant_id=0 系统模板克隆到目标租户）
     */
    @SaCheckPermission("micro:page:edit")
    @Log(title = "页面装修-恢复默认", businessType = BusinessType.UPDATE)
    @PostMapping("/resetDefault")
    public R<Void> resetDefault(@RequestParam String roleCode,
                                @RequestParam String pageKey,
                                @RequestParam(required = false) Long tenantId) {
        pageBlockConfigService.resetDefault(roleCode, pageKey, tenantId);
        return R.ok();
    }

    /**
     * 新增单区块（超管默认写入系统模板 tenant_id=0；老板写入本租户）
     */
    @SaCheckPermission("micro:page:add")
    @Log(title = "页面区块", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody PageBlockConfigBo bo) {
        return R.ok(pageBlockConfigService.insert(bo));
    }

    /**
     * 编辑单区块（排版：排序/显隐/参数）
     */
    @SaCheckPermission("micro:page:edit")
    @Log(title = "页面区块", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PageBlockConfigBo bo) {
        return toAjax(pageBlockConfigService.update(bo));
    }

    /**
     * 删除区块（page_block_config 无 del_flag，物理删除，可批量）
     */
    @SaCheckPermission("micro:page:remove")
    @Log(title = "页面区块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(pageBlockConfigService.deleteByIds(Arrays.asList(ids)));
    }
}
