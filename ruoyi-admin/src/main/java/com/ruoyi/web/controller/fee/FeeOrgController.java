package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeOrgBo;
import com.ruoyi.system.domain.bo.FeeOrgUserBo;
import com.ruoyi.system.domain.vo.FeeOrgUserVo;
import com.ruoyi.system.domain.vo.FeeOrgVo;
import com.ruoyi.system.service.IFeeOrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织管理Controller（T7.1 / FEAT_MULTI_ORG）
 * 组织树 + 用户挂组织 + 数据范围（本组织+下级组织）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/org")
@RequireFeature("FEAT_MULTI_ORG")
public class FeeOrgController extends BaseController {

    private final IFeeOrgService orgService;

    /**
     * 组织树（含成员数统计）
     */
    @SaCheckPermission("fee:org:list")
    @GetMapping("/tree")
    public R<List<FeeOrgVo>> tree() {
        return R.ok(orgService.selectOrgTree());
    }

    /**
     * 新增组织
     */
    @SaCheckPermission("fee:org:edit")
    @Log(title = "组织管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeOrgBo bo) {
        return R.ok(orgService.insertOrg(bo));
    }

    /**
     * 修改组织
     */
    @SaCheckPermission("fee:org:edit")
    @Log(title = "组织管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FeeOrgBo bo) {
        return toAjax(orgService.updateOrg(bo));
    }

    /**
     * 删除组织（存在子组织或成员时禁止）
     */
    @SaCheckPermission("fee:org:edit")
    @Log(title = "组织管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(orgService.deleteOrg(id));
    }

    /**
     * 组织成员列表
     */
    @SaCheckPermission("fee:org:list")
    @GetMapping("/users/{orgId}")
    public R<List<FeeOrgUserVo>> users(@PathVariable Long orgId) {
        return R.ok(orgService.listOrgUsers(orgId));
    }

    /**
     * 全量保存组织成员（用户挂组织）
     */
    @SaCheckPermission("fee:org:edit")
    @Log(title = "组织成员", businessType = BusinessType.UPDATE)
    @PutMapping("/users")
    public R<Void> saveUsers(@Validated @RequestBody FeeOrgUserBo bo) {
        return toAjax(orgService.saveOrgUsers(bo));
    }
}
