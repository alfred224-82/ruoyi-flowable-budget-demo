package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.BookingPolicyBo;
import com.ruoyi.system.domain.vo.BookingPolicyVo;
import com.ruoyi.system.service.IBookingPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 预约策略配置Controller（T18.10，小微版 M18）
 * <p>「策略开关」菜单（micro:bookingPolicy:*）：
 * <ul>
 *   <li>是否允许顾客自助预约</li>
 *   <li>提前预约天数</li>
 *   <li>取消政策（免费/收费/禁止）</li>
 *   <li>自动确认开关</li>
 *   <li>每日最大预约数</li>
 * </ul>
 * <p>★租户管理员（platform_admin）专属。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/bookingPolicy")
public class MicroBookingPolicyController extends BaseController {

    private final IBookingPolicyService bookingPolicyService;

    /**
     * 查询当前预约策略
     */
    @SaCheckPermission("micro:bookingPolicy:list")
    @GetMapping
    public R<BookingPolicyVo> getPolicy() {
        return R.ok(bookingPolicyService.queryPolicy());
    }

    /**
     * ★顾客端预约策略视图（登录可读、不校验权限，同 /micro/tenantRole/displayNames 模式）
     * <p>顾客小程序首页/自助预约页据此隐藏或展示自助预约入口（前端第一层防线），
     *   后端提交接口仍复核策略（最终防线）；仅返回顾客相关开关项。
     */
    @GetMapping("/customerView")
    public R<Map<String, Object>> customerView() {
        BookingPolicyVo vo = bookingPolicyService.queryPolicy();
        Map<String, Object> view = new LinkedHashMap<>(16);
        view.put("customerSelfBooking", vo != null ? vo.getCustomerSelfBooking() : null);
        // REQ-01：三类资源控制开关（enabled=是否对客户开放, required=是否必填）+ 行业编码（锁死行业前端提示）
        view.put("projectBookingEnabled", vo != null ? vo.getProjectBookingEnabled() : 1);
        view.put("projectBookingRequired", vo != null ? vo.getProjectBookingRequired() : 0);
        view.put("roomBookingEnabled", vo != null ? vo.getRoomBookingEnabled() : 1);
        view.put("roomBookingRequired", vo != null ? vo.getRoomBookingRequired() : 0);
        view.put("staffBookingEnabled", vo != null ? vo.getStaffBookingEnabled() : 1);
        view.put("staffBookingRequired", vo != null ? vo.getStaffBookingRequired() : 0);
        view.put("industryCode", vo != null ? vo.getIndustryCode() : null);
        return R.ok(view);
    }

    /**
     * 保存/更新预约策略
     */
    @SaCheckPermission("micro:bookingPolicy:edit")
    @Log(title = "预约策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Long> savePolicy(@RequestBody BookingPolicyBo bo) {
        return R.ok(bookingPolicyService.savePolicy(bo));
    }
}
