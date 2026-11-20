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
import com.ruoyi.system.domain.bo.BookingOrderBo;
import com.ruoyi.system.domain.vo.BookingOrderVo;
import com.ruoyi.system.service.IBookingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 预约订单管理Controller（T15.12，老板 PC）
 * <p>预约列表/新建（帮顾客约）/编辑/取消/完成服务/标记已收款/详情。
 * <p>★V1 零支付合规：标记「已收款」仅改状态 + 自动写 cash_flow 收入流水，不调任何支付接口。
 * <p>perms 对齐 M15_06：micro:booking:list/add/edit/cancel/complete；「标记已收款」无独立种子权限，归 micro:booking:edit。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/booking")
public class BookingOrderController extends BaseController {

    private final IBookingOrderService bookingOrderService;

    /**
     * 预约订单列表
     */
    @SaCheckPermission("micro:booking:list")
    @GetMapping("/list")
    public TableDataInfo<BookingOrderVo> list(BookingOrderBo bo, PageQuery pageQuery) {
        return bookingOrderService.queryPageList(bo, pageQuery);
    }

    /**
     * 预约订单详情
     */
    @SaCheckPermission("micro:booking:list")
    @GetMapping("/{id}")
    public R<BookingOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(bookingOrderService.queryById(id));
    }

    /**
     * 新建预约（帮顾客约）
     */
    @SaCheckPermission("micro:booking:add")
    @Log(title = "预约订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody BookingOrderBo bo) {
        return R.ok(bookingOrderService.insert(bo));
    }

    /**
     * 编辑预约
     */
    @SaCheckPermission("micro:booking:edit")
    @Log(title = "预约订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody BookingOrderBo bo) {
        return toAjax(bookingOrderService.update(bo));
    }

    /**
     * 取消预约
     */
    @SaCheckPermission("micro:booking:cancel")
    @Log(title = "预约订单", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(bookingOrderService.cancel(id));
    }

    /**
     * 完成服务
     */
    @SaCheckPermission("micro:booking:complete")
    @Log(title = "预约订单", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{id}")
    public R<Void> complete(@PathVariable Long id) {
        return toAjax(bookingOrderService.complete(id));
    }

    /**
     * ★标记已收款（线下收款后点此，自动写 cash_flow 收入流水，不调支付接口）
     */
    @SaCheckPermission("micro:booking:edit")
    @Log(title = "预约订单收款", businessType = BusinessType.UPDATE)
    @PutMapping("/receive/{id}")
    public R<Void> markReceived(@PathVariable Long id) {
        return toAjax(bookingOrderService.markReceived(id));
    }
}
