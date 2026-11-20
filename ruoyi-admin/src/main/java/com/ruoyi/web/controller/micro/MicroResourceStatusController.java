package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ResourceStatusLogVo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.service.IMicroSelfBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Boss PC 资源状态管理 Controller（T21.5/T21.6，小微版）
 * <p>Boss 可查看助教/房间当前资源状态（空闲🟢/预约中🟡/使用中🔴），
 * 支持手动将「预约中」改为「空闲」（临时放开资源，需填写原因）。
 * <p>类级 {@code @RequireFeature("FEAT_SELF_BOOKING")}，与自助预约共用同一功能开关。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Validated
@RequireFeature("FEAT_SELF_BOOKING")
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/resourceStatus")
public class MicroResourceStatusController extends BaseController {

    private final IMicroSelfBookingService selfBookingService;

    /**
     * 房间资源状态列表（space 分类 service_item，含 resource_status）
     */
    @SaCheckPermission("micro:resourceStatus:room:view")
    @GetMapping("/room/list")
    public R<List<ServiceItemVo>> listRoomStatus() {
        return R.ok(selfBookingService.listRoomResourceStatus());
    }

    /**
     * 助教资源状态列表（salesman 角色用户 + 最新 resource_status_log 推导状态）
     */
    @SaCheckPermission("micro:resourceStatus:staff:view")
    @GetMapping("/staff/list")
    public R<List<Map<String, Object>>> listStaffStatus() {
        return R.ok(selfBookingService.listStaffResourceStatus());
    }

    /**
     * 手动释放资源（reserved → idle + 写 resource_status_log）
     *
     * @param resourceType staff=助教 / room=房间
     * @param resourceId   助教=sys_user.user_id / 房间=service_item.id
     * @param reason       释放原因（必填）
     */
    @SaCheckPermission("micro:resourceStatus:release")
    @Log(title = "手动释放资源", businessType = BusinessType.UPDATE)
    @PutMapping("/release")
    public R<Void> manualRelease(@RequestParam String resourceType,
                                 @RequestParam Long resourceId,
                                 @RequestParam String reason) {
        selfBookingService.manualReleaseResource(resourceType, resourceId, reason);
        return R.ok();
    }

    /**
     * 手动设置资源状态（写 resource_status_log；房间同步更新 service_item.resource_status）
     * <p>助教可选：idle=空闲 / working=工作 / resting=休息；房间可选：idle=空闲 / in_use=使用。
     * <p>仅 idle 状态的资源可被顾客预约。
     *
     * @param resourceType staff=助教 / room=房间
     * @param resourceId   助教=sys_user.user_id / 房间=service_item.id
     * @param targetStatus 目标状态
     * @param reason       变更原因（可空）
     */
    @SaCheckPermission("micro:resourceStatus:release")
    @Log(title = "设置资源状态", businessType = BusinessType.UPDATE)
    @PutMapping("/set")
    public R<Void> setStatus(@RequestParam String resourceType,
                             @RequestParam Long resourceId,
                             @RequestParam String targetStatus,
                             @RequestParam(required = false) String reason) {
        selfBookingService.setResourceStatus(resourceType, resourceId, targetStatus, reason);
        return R.ok();
    }

    /**
     * 资源状态变更日志（最近 50 条）
     *
     * @param resourceType staff/room（可空=全部）
     * @param resourceId   资源ID（可空=全部）
     */
    @SaCheckPermission("micro:resourceStatus:log:list")
    @GetMapping("/log/list")
    public R<List<ResourceStatusLogVo>> listLogs(
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) Long resourceId) {
        return R.ok(selfBookingService.listResourceStatusLogs(resourceType, resourceId));
    }
}
