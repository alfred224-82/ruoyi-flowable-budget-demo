package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.SeatAssignBo;
import com.ruoyi.system.domain.bo.SeatCanvasPersonBo;
import com.ruoyi.system.domain.bo.SeatMoveBo;
import com.ruoyi.system.domain.bo.SeatMoveMineBo;
import com.ruoyi.system.domain.bo.SeatPickBo;
import com.ruoyi.system.domain.bo.SeatSwapBo;
import com.ruoyi.system.domain.vo.SeatAssignmentVo;
import com.ruoyi.system.domain.vo.SeatCandidateVo;
import com.ruoyi.system.domain.vo.SeatMapVo;
import com.ruoyi.system.domain.vo.SeatOperationLogVo;
import com.ruoyi.system.service.ISeatAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动座位占用 Controller（T26.7 / T26.8，小微版 M26）
 * <p>顾客小程序按位置选座 / 取消选座；活动座位图查询。
 * <p>★联动 M28：活动锁定后顾客端选座接口拒绝（Service 层抛 ServiceException）。
 * <p>★座位图查询接口登录即可访问（不限制角色）；选座 / 取消需 {@code micro:booking:pickSeat} 权限（T26.7 菜单 7290）。
 * <p>★Boss 编排接口（assign / swap / move）见 M28 T28.2，本 Controller 不承载。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/seat")
public class MicroSeatAssignmentController extends BaseController {

    private final ISeatAssignmentService seatAssignmentService;

    /**
     * 查询活动座位图（T26.7）
     * <p>返回布局背景图 + 座位列表（空闲 / 已占 / 预置三态）+ 统计信息。
     * <p>★登录即可访问（顾客 / Boss / 员工均可查看座位图）。
     * <p>★若房间未配置座位布局（T26.1 未落地或 has_seat_layout=0），返回 layoutId=null + seats=简化列表。
     */
    @GetMapping("/map/{activityId}")
    public R<SeatMapVo> seatMap(@PathVariable Long activityId) {
        return R.ok(seatAssignmentService.getSeatMap(activityId));
    }

    /**
     * 顾客选座（T26.7）
     * <p>★服务端校验：活动未锁定 / 座位空闲 / 当前用户未重复占座。
     * <p>★并发选座由 UNIQUE(activity_id, seat_id) 兜底，冲突时返回友好提示。
     */
    @SaCheckPermission("micro:booking:pickSeat")
    @Log(title = "顾客选座", businessType = BusinessType.INSERT)
    @PostMapping("/pick")
    public R<Long> pick(@Validated(AddGroup.class) @RequestBody SeatPickBo bo) {
        return R.ok(seatAssignmentService.pickSeat(bo));
    }

    /**
     * 顾客取消自己的座位（T26.7）
     * <p>★服务端校验：assignment.user_id == currentUserId；活动未锁定。
     */
    @SaCheckPermission("micro:booking:pickSeat")
    @Log(title = "顾客取消座位", businessType = BusinessType.DELETE)
    @DeleteMapping("/release/{assignmentId}")
    public R<Void> release(@PathVariable Long assignmentId) {
        seatAssignmentService.releaseSeat(assignmentId);
        return R.ok();
    }

    /**
     * 查询活动座位占用列表（Boss 端 / 内部使用）
     * <p>★登录即可访问（Boss / 员工 / 顾客均可查看本活动占座名单）。
     */
    @GetMapping("/assignments/{activityId}")
    public R<List<SeatAssignmentVo>> assignments(@PathVariable Long activityId) {
        return R.ok(seatAssignmentService.listAssignments(activityId));
    }

    // ============================================================
    // M28 T28.2 Boss 编排接口（加人 / 移座 / 互换）——perms micro:seat:assign
    // ============================================================

    /**
     * ★Boss 加人到座位（T28.2）：将指定人员指派到活动的某个空闲座位（assignment_type=BOSS_ASSIGN）。
     * <p>★服务端校验：活动未锁定（is_locked=0）/ 目标座位空闲 / 该人员未在本活动重复占座。
     * <p>★操作日志 ASSIGN 由 {@code @SeatOperation} 切面自动记录（含前后座位快照）。
     */
    @SaCheckPermission("micro:seat:assign")
    @Log(title = "Boss编排座位", businessType = BusinessType.INSERT)
    @PostMapping("/assign")
    public R<Long> assign(@Validated(AddGroup.class) @RequestBody SeatAssignBo bo) {
        return R.ok(seatAssignmentService.bossAssign(bo));
    }

    /**
     * ★Boss 移动座位（T28.2，含预置人员）：将某条占座记录（含导游/司机等预置人员）移动到另一个空闲座位。
     * <p>★服务端校验：活动未锁定 / 占座记录属于本活动 / 目标座位空闲（预留记录会被清理）。
     * <p>★预置人员可被 Boss 移动，assignment_type 保持不变；操作日志 MOVE 由切面自动记录。
     */
    @SaCheckPermission("micro:seat:assign")
    @Log(title = "Boss编排座位", businessType = BusinessType.UPDATE)
    @PutMapping("/move")
    public R<Void> move(@Validated(AddGroup.class) @RequestBody SeatMoveBo bo) {
        seatAssignmentService.moveSeat(bo);
        return R.ok();
    }

    /**
     * ★Boss 两人互换座位（T28.2）：将两条已占座记录的座位互换（含预置人员）。
     * <p>★服务端校验：活动未锁定 / 两条占座记录均属于本活动 / 非同一条记录。
     * <p>★实现上交换两条记录的 user_id/assignment_type/remark（seat_id 固定，规避 UNIQUE 冲突）；操作日志 SWAP 由切面自动记录。
     */
    @SaCheckPermission("micro:seat:assign")
    @Log(title = "Boss编排座位", businessType = BusinessType.UPDATE)
    @PostMapping("/swap")
    public R<Void> swap(@Validated(AddGroup.class) @RequestBody SeatSwapBo bo) {
        seatAssignmentService.swapSeat(bo);
        return R.ok();
    }

    // ============================================================
    // M28 T28.3 顾客移座接口（仅自己的）——perms micro:booking:pickSeat
    // ============================================================

    /**
     * ★顾客移动自己的座位（T28.3）：顾客仅可将【自己】所占的座位移动到另一个空闲座位。
     * <p>★服务端校验：活动未锁定 / from 座位占座人 == 当前登录用户 / 目标座位空闲。
     * <p>★操作日志 MOVE 由切面自动记录；锁定后拒绝（抛 ServiceException）。
     */
    @SaCheckPermission("micro:booking:pickSeat")
    @Log(title = "顾客移座", businessType = BusinessType.UPDATE)
    @PutMapping("/moveMine")
    public R<Void> moveMine(@Validated(AddGroup.class) @RequestBody SeatMoveMineBo bo) {
        seatAssignmentService.moveMine(bo);
        return R.ok();
    }

    // ============================================================
    // M28 T28.4 座位编排操作日志查询（Boss 端回放）
    // ============================================================

    /**
     * ★查询活动的座位编排操作日志（T28.4）：按操作时间倒序返回，含 before/after 座位快照。
     * <p>★需 {@code micro:seat:assign} 权限（Boss 编排回放专用）。
     */
    @SaCheckPermission("micro:seat:assign")
    @GetMapping("/operationLogs/{activityId}")
    public R<List<SeatOperationLogVo>> operationLogs(@PathVariable Long activityId) {
        return R.ok(seatAssignmentService.listOperationLogs(activityId));
    }

    // ============================================================
    // T26.10 Boss 背景画布加人（发布/编辑活动弹窗）——perms micro:seat:assign
    // ============================================================

    /**
     * ★画布加人候选人（T26.10）：按角色分组返回租户下全部员工（导游/司机）
     * 与注册游客（含头像），供发布活动弹窗背景画布点座“＋”号选人。
     * <p>★需 {@code micro:seat:assign} 权限（Boss 编排专用）。
     */
    @SaCheckPermission("micro:seat:assign")
    @GetMapping("/candidates")
    public R<List<SeatCandidateVo>> candidates() {
        return R.ok(seatAssignmentService.listSeatCandidates());
    }

    /**
     * ★背景画布加人批量保存（T26.10）：将画布上选好的真实人员（员工/游客）批量指派到座位。
     * <p>★全量替换：仅替换本活动 PRESET/BOSS_ASSIGN 记录，顾客自选（CUSTOMER_PICK）不受影响。
     * <p>★服务端校验：活动未锁定 / 人员存在 / 座位与人员不重复 / 目标座位未被顾客自选；
     *   操作日志 PRESET 由 {@code @SeatOperation} 切面自动记录。
     */
    @SaCheckPermission("micro:seat:assign")
    @Log(title = "Boss背景画布加人", businessType = BusinessType.UPDATE)
    @PostMapping("/saveCanvasPersons")
    public R<Void> saveCanvasPersons(@Validated(AddGroup.class) @RequestBody SeatCanvasPersonBo bo) {
        seatAssignmentService.saveCanvasPersons(bo);
        return R.ok();
    }
}
