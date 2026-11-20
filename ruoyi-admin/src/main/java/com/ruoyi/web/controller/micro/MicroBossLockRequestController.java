package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ActivityLockRequestVo;
import com.ruoyi.system.service.IActivityLockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 老板端·活动锁定申请审批Controller（M33 §九 老板/员工权限分工）
 * <p>§九 权限分工：「申请锁定」由员工提交，「审批锁定申请」由老板把关。
 * 员工端 {@code POST /micro/staff/lockApply} 提交申请 → 本控制器读取待审批列表并一键审批：
 * <ul>
 *   <li>审批通过：委派 {@code ActivityLockRequestServiceImpl#approve} 触发活动真正锁定；</li>
 *   <li>驳回：置 REJECTED，活动保持未锁定，可附驳回原因。</li>
 * </ul>
 * <p>★权限复用既有 {@code micro:activity:lock}（菜单 7287，[128]M28_01 交付，boss 已授权）：
 *   只有具备锁定权限的老板才能查看/审批锁定申请；FEAT_ACTIVITY_LOCK 版本门槛与锁定/解锁一致。
 * <p>★前端接口路径见 ruoyi-app/src/config/index.js：microBossLockRequestList/Approve/Reject。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/boss/lockRequest")
public class MicroBossLockRequestController extends BaseController {

    private final IActivityLockRequestService activityLockRequestService;

    /**
     * 待处理锁定申请列表（§九：审批锁定申请=老板把关）。
     *
     * @param status 状态过滤：空=默认 PENDING（待审批）；可传 APPROVED/REJECTED 查历史
     */
    @SaCheckPermission("micro:activity:lock")
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @GetMapping("/list")
    public R<List<ActivityLockRequestVo>> list(@RequestParam(required = false) String status) {
        return R.ok(activityLockRequestService.listRequests(status));
    }

    /**
     * 审批通过锁定申请：触发活动真正锁定（is_locked=1 + 座位只读）+ 申请置 APPROVED。
     *
     * @param id 申请记录ID
     */
    @SaCheckPermission("micro:activity:lock")
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @Log(title = "审批通过锁定申请", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        activityLockRequestService.approve(id);
        return R.ok();
    }

    /**
     * 驳回锁定申请：申请置 REJECTED，活动保持未锁定。
     *
     * @param id   申请记录ID
     * @param body 可选请求体：reason（驳回原因）
     */
    @SaCheckPermission("micro:activity:lock")
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @Log(title = "驳回锁定申请", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Object reasonObj = body == null ? null : body.get("reason");
        activityLockRequestService.reject(id, reasonObj == null ? null : String.valueOf(reasonObj));
        return R.ok();
    }
}
