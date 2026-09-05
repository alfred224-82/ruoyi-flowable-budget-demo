package com.ruoyi.web.controller.billing;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.SysBillingPlanBo;
import com.ruoyi.system.domain.vo.SysBillingPlanVo;
import com.ruoyi.system.service.IBillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计费套餐Controller（T14.1，平台级；菜单 perms billing:plan:*）
 * <p>四档套餐（试用/首月特惠/月付/包年）系统级共享，平台超管统一维护。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/billing/plan")
public class BillingPlanController extends BaseController {

    private final IBillingPlanService billingPlanService;

    /**
     * 套餐分页列表
     */
    @SaCheckPermission("billing:plan:list")
    @GetMapping("/list")
    public TableDataInfo<SysBillingPlanVo> list(@RequestParam(required = false) String planCode,
                                                @RequestParam(required = false) String status,
                                                PageQuery pageQuery) {
        return billingPlanService.queryPageList(planCode, status, pageQuery);
    }

    /**
     * 上架套餐列表（供下单选择）
     */
    @SaCheckPermission("billing:plan:list")
    @GetMapping("/enabled")
    public R<List<SysBillingPlanVo>> enabled() {
        return R.ok(billingPlanService.listEnabled());
    }

    /**
     * 套餐详情
     */
    @SaCheckPermission("billing:plan:query")
    @GetMapping("/{planCode}")
    public R<SysBillingPlanVo> getInfo(@PathVariable String planCode) {
        return R.ok(billingPlanService.queryByCode(planCode));
    }

    /**
     * 新增套餐
     */
    @SaCheckPermission("billing:plan:add")
    @Log(title = "计费套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysBillingPlanBo bo) {
        return toAjax(billingPlanService.insert(bo));
    }

    /**
     * 编辑套餐
     */
    @SaCheckPermission("billing:plan:edit")
    @Log(title = "计费套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysBillingPlanBo bo) {
        return toAjax(billingPlanService.update(bo));
    }
}
