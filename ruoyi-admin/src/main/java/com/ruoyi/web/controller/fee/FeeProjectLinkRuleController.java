package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo;
import com.ruoyi.system.service.IFeeProjectLinkRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目关联规则Controller（T5.7）
 * 三层内置：角色级硬规则 + 大类级软提示 + 财务兜底
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/linkRule")
public class FeeProjectLinkRuleController extends BaseController {

    private final IFeeProjectLinkRuleService linkRuleService;

    /**
     * 规则列表
     */
    @RequireFeature("FEAT_LINK_RULE")
    @SaCheckPermission("fee:linkRule:list")
    @GetMapping("/list")
    public R<List<FeeProjectLinkRuleVo>> list() {
        return R.ok(linkRuleService.queryList());
    }

    /**
     * 新增/修改规则（财务维护）
     */
    @RequireFeature("FEAT_LINK_RULE")
    @SaCheckPermission("fee:linkRule:edit")
    @Log(title = "项目关联规则", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Long> save(@RequestBody FeeProjectLinkRuleVo vo) {
        return R.ok(linkRuleService.saveRule(vo));
    }

    /**
     * 删除规则
     */
    @RequireFeature("FEAT_LINK_RULE")
    @SaCheckPermission("fee:linkRule:edit")
    @Log(title = "项目关联规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(linkRuleService.deleteRule(id));
    }

    /**
     * 匹配当前用户的关联规则（报销单表单调用，登录即可）
     * 无匹配返回 null，前端兜底公司级（project_id=0）
     */
    @GetMapping("/match")
    public R<FeeProjectLinkRuleVo> match(@RequestParam(required = false) Long categoryId) {
        return R.ok(linkRuleService.matchRule(LoginHelper.getLoginUser().getRolePermission(), categoryId));
    }
}
