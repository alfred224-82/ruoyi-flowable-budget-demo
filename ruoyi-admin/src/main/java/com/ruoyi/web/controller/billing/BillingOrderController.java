package com.ruoyi.web.controller.billing;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.BillingOrderCreateBo;
import com.ruoyi.system.domain.vo.SysBillingOrderVo;
import com.ruoyi.system.service.IBillingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 计费订单Controller（T14.1，平台级；菜单 perms billing:order:*）
 * <p>平台代下单/离线收款开通/续费；【严禁自动扣款】——付费套餐须显式确认收款（markPaid）方激活。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/billing/order")
public class BillingOrderController extends BaseController {

    private final IBillingOrderService billingOrderService;

    /**
     * 订单分页列表
     */
    @SaCheckPermission("billing:order:list")
    @GetMapping("/list")
    public TableDataInfo<SysBillingOrderVo> list(@RequestParam(required = false) String orderNo,
                                                 @RequestParam(required = false) String payStatus,
                                                 @RequestParam(required = false) Long tenantId,
                                                 PageQuery pageQuery) {
        return billingOrderService.queryPageList(orderNo, payStatus, tenantId, pageQuery);
    }

    /**
     * 订单详情
     */
    @SaCheckPermission("billing:order:query")
    @GetMapping("/{id}")
    public R<SysBillingOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(billingOrderService.queryById(id));
    }

    /**
     * 下单/手动开通（试用或离线/手动收款即时开通，在线渠道置待支付）
     */
    @SaCheckPermission("billing:order:create")
    @Log(title = "计费订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<SysBillingOrderVo> create(@Validated(AddGroup.class) @RequestBody BillingOrderCreateBo bo) {
        return R.ok(billingOrderService.createOrder(bo));
    }

    /**
     * 确认收款（离线/第三方回调）：待支付 → 已支付，激活租户并分账
     */
    @SaCheckPermission("billing:order:create")
    @Log(title = "计费订单", businessType = BusinessType.UPDATE)
    @PutMapping("/paid/{id}")
    public R<Void> markPaid(@PathVariable Long id, @RequestParam(required = false) String transactionId) {
        return toAjax(billingOrderService.markPaid(id, transactionId));
    }

    /**
     * 取消未支付订单
     */
    @SaCheckPermission("billing:order:create")
    @Log(title = "计费订单", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(billingOrderService.cancel(id));
    }
}
