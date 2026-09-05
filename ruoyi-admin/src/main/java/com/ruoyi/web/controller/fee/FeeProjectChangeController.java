package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeProjectChangeBo;
import com.ruoyi.system.domain.vo.FeeProjectChangeVo;
import com.ruoyi.system.service.IFeeProjectChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 合同变更Controller（T5.5）
 * 变更单→老板审批→增量叠加→待收款确认→财务闭环
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/change")
public class FeeProjectChangeController extends BaseController {

    private final IFeeProjectChangeService changeService;

    /**
     * 变更单列表
     */
    @SaCheckPermission("fee:change:list")
    @GetMapping("/list")
    public TableDataInfo<FeeProjectChangeVo> list(FeeProjectChangeBo bo, PageQuery pageQuery) {
        return changeService.queryPageList(bo, pageQuery);
    }

    /**
     * 变更单详情
     */
    @SaCheckPermission("fee:change:query")
    @GetMapping("/{id}")
    public R<FeeProjectChangeVo> getInfo(@PathVariable Long id) {
        return R.ok(changeService.queryById(id));
    }

    /**
     * 发起合同变更
     */
    @SaCheckPermission("fee:change:add")
    @Log(title = "合同变更", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeProjectChangeBo bo) {
        return R.ok(changeService.insertChange(bo));
    }

    /**
     * 老板审批通过（合同额/预算增量叠加）
     */
    @SaCheckPermission("fee:change:approve")
    @Log(title = "合同变更审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        return toAjax(changeService.approveChange(id));
    }

    /**
     * 老板驳回
     */
    @SaCheckPermission("fee:change:approve")
    @Log(title = "合同变更审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(changeService.rejectChange(id, reason));
    }

    /**
     * 财务收款确认（闭环）
     */
    @SaCheckPermission("fee:change:confirm")
    @Log(title = "收款确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{id}")
    public R<Void> confirm(@PathVariable Long id) {
        return toAjax(changeService.confirmChange(id));
    }
}
