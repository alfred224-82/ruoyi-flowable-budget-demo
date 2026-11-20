package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.SelfBookingSubmitBo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.service.IMicroSelfBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 顾客小程序「自助预约」Controller（T21.1~T21.4，小微版）
 * <p>三类预约入口：预约项目 / 预约房间 / 预约助教，支持可选关联资源。
 * <p>★复用 booking_order 表写入，V1 零支付（pay_status=PENDING_PAY）。
 * <p>类级 {@code @RequireFeature("FEAT_SELF_BOOKING")} 版本开关控制。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Validated
@RequireFeature("FEAT_SELF_BOOKING")
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/selfBooking")
public class MicroSelfBookingController extends BaseController {

    private final IMicroSelfBookingService selfBookingService;

    /**
     * 查询可选项目列表（排除 space 分类，仅上架状态）
     */
    @SaCheckPermission("micro:selfBooking:view")
    @GetMapping("/items")
    public R<List<ServiceItemVo>> listAvailableItems() {
        return R.ok(selfBookingService.listAvailableItems());
    }

    /**
     * 查询空闲房间列表（排除该时段已有预约 + 排除 in_use 房间）
     *
     * @param bookingDate 预约日期（yyyy-MM-dd）
     * @param timeSlot    预约时段（如 09:00-11:00，可空）
     */
    @SaCheckPermission("micro:selfBooking:view")
    @GetMapping("/rooms")
    public R<List<ServiceItemVo>> listAvailableRooms(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
        @RequestParam(required = false) String timeSlot) {
        // 校验预约日期（≤ 明天）
        selfBookingService.validateBookingTime(bookingDate);
        return R.ok(selfBookingService.listAvailableRooms(bookingDate, timeSlot));
    }

    /**
     * 查询当天有排班的助教列表（返回 avatar/nickName/description/hourlyRate）
     *
     * @param bookingDate 预约日期（yyyy-MM-dd）
     */
    @SaCheckPermission("micro:selfBooking:view")
    @GetMapping("/staff")
    public R<List<Map<String, Object>>> listAvailableStaff(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate) {
        // 校验预约日期（≤ 明天）
        selfBookingService.validateBookingTime(bookingDate);
        return R.ok(selfBookingService.listAvailableStaff(bookingDate));
    }

    /**
     * 提交自助预约（写 booking_order + 更新资源状态为 reserved + 写 resource_status_log）
     */
    @SaCheckPermission("micro:selfBooking:submit")
    @Log(title = "自助预约", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public R<Long> submitSelfBooking(@Validated(AddGroup.class) @RequestBody SelfBookingSubmitBo bo) {
        return R.ok(selfBookingService.submitSelfBooking(bo));
    }
}
