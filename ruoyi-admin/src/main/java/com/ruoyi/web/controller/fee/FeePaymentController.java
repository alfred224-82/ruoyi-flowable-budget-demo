package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeePaymentBo;
import com.ruoyi.system.domain.vo.FeePaymentVo;
import com.ruoyi.system.service.IFeePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 付款单管理Controller（T5.2）
 * 必选项目、财务登记、老板审批、通过扣项目预算 + 记总支出
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/payment")
public class FeePaymentController extends BaseController {

    private final IFeePaymentService paymentService;

    /**
     * 付款单列表
     */
    @SaCheckPermission("fee:payment:list")
    @GetMapping("/list")
    public TableDataInfo<FeePaymentVo> list(FeePaymentBo bo, PageQuery pageQuery) {
        return paymentService.queryPageList(bo, pageQuery);
    }

    /**
     * 付款单详情
     */
    @SaCheckPermission("fee:payment:query")
    @GetMapping("/{id}")
    public R<FeePaymentVo> getInfo(@PathVariable Long id) {
        return R.ok(paymentService.queryById(id));
    }

    /**
     * 财务登记付款单
     */
    @SaCheckPermission("fee:payment:add")
    @Log(title = "付款单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeePaymentBo bo) {
        return R.ok(paymentService.insertPayment(bo));
    }

    /**
     * 老板审批通过（扣项目预算 + 记总支出）
     */
    @SaCheckPermission("fee:payment:approve")
    @Log(title = "付款单审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        return toAjax(paymentService.approvePayment(id));
    }

    /**
     * 老板驳回
     */
    @SaCheckPermission("fee:payment:reject")
    @Log(title = "付款单审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(paymentService.rejectPayment(id, reason));
    }
}
