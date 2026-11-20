package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.SeatAssignBo;
import com.ruoyi.system.domain.bo.SeatMoveBo;
import com.ruoyi.system.domain.bo.SeatMoveMineBo;
import com.ruoyi.system.domain.bo.SeatPickBo;
import com.ruoyi.system.domain.bo.SeatCanvasPersonBo;
import com.ruoyi.system.domain.bo.SeatSwapBo;
import com.ruoyi.system.domain.vo.SeatAssignmentVo;
import com.ruoyi.system.domain.vo.SeatCandidateVo;
import com.ruoyi.system.domain.vo.SeatMapVo;
import com.ruoyi.system.domain.vo.SeatOperationLogVo;

import java.util.List;

/**
 * 活动座位占用服务接口（T26.7 / T26.8，小微版 M26）
 * <p>顾客小程序按位置选座 / 取消选座；活动座位图查询。
 * <p>★联动 M28：活动锁定后顾客端选座接口拒绝；Boss 编排接口见 M28 T28.2。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface ISeatAssignmentService {

    /**
     * 查询活动座位图（T26.7）
     * <p>返回布局背景图 + 座位列表（空闲 / 已占 / 预置三态）+ 统计信息。
     * <p>★若房间未配置座位布局（has_seat_layout=0 或 room_seat_layout 表不存在），
     *   返回 layoutId=null + seats=空列表，前端不展示选座入口。
     *
     * @param activityId 活动ID
     * @return 座位图 VO
     */
    SeatMapVo getSeatMap(Long activityId);

    /**
     * 顾客选座（T26.7）
     * <p>★服务端校验：
     * <ul>
     *   <li>活动存在且未锁定（locked=0）</li>
     *   <li>座位空闲（无 activity_seat_assignment 记录或 user_id=NULL）</li>
     *   <li>当前用户未在该活动重复占座</li>
     * </ul>
     * <p>★写入 activity_seat_assignment（assignment_type=CUSTOMER_PICK）+ 操作日志（ASSIGN）。
     *
     * @param bo 选座参数（activityId + seatId + seatNo）
     * @return 占座记录ID（assignmentId）
     */
    Long pickSeat(SeatPickBo bo);

    /**
     * 顾客取消自己的座位（T26.7）
     * <p>★服务端校验：assignment.user_id == currentUserId；活动未锁定。
     * <p>★删除 activity_seat_assignment 记录 + 操作日志（RELEASE）。
     *
     * @param assignmentId 占座记录ID
     */
    void releaseSeat(Long assignmentId);

    /**
     * 查询活动的座位占用列表（Boss 端 / 内部使用）
     *
     * @param activityId 活动ID
     * @return 占座记录列表（含用户昵称 / 头像回填）
     */
    List<SeatAssignmentVo> listAssignments(Long activityId);

    // ============================================================
    // M28 T28.2 Boss 编排接口（加人 / 移座 / 互换）
    // ============================================================

    /**
     * Boss 加人到座位（T28.2）
     * <p>★服务端校验：活动未锁定（is_locked=0）/ 目标座位空闲 / 该人员未在本活动重复占座。
     * <p>★写入 activity_seat_assignment（assignment_type=BOSS_ASSIGN）；操作日志 ASSIGN 由切面记录。
     *
     * @param bo 加人参数（activityId + seatId + userId）
     * @return 占座记录ID（assignmentId）
     */
    Long bossAssign(SeatAssignBo bo);

    /**
     * Boss 移动座位（T28.2，含预置人员）
     * <p>★服务端校验：活动未锁定 / 占座记录属于本活动 / 目标座位空闲。
     * <p>★预置人员（导游/司机）可被 Boss 移动，assignment_type 保持不变；操作日志 MOVE 由切面记录。
     *
     * @param bo 移座参数（activityId + assignmentId + toSeatId）
     */
    void moveSeat(SeatMoveBo bo);

    /**
     * Boss 两人互换座位（T28.2）
     * <p>★服务端校验：活动未锁定 / 两条占座记录均属于本活动 / 非同一条记录。
     * <p>★交换两条记录的 seat_id + seat_no；操作日志 SWAP 由切面记录。
     *
     * @param bo 互换参数（activityId + assignmentIdA + assignmentIdB）
     */
    void swapSeat(SeatSwapBo bo);

    /**
     * 顾客移动自己的座位（T28.3）
     * <p>★服务端校验：活动未锁定 / from 座位占座人 == 当前登录用户 / 目标座位空闲。
     * <p>★操作日志 MOVE 由切面记录。
     *
     * @param bo 移座参数（activityId + fromSeatId + toSeatId）
     */
    void moveMine(SeatMoveMineBo bo);

    /**
     * 查询活动的座位编排操作日志（Boss 端回放，T28.4）
     *
     * @param activityId 活动ID
     * @return 操作日志列表（按操作时间倒序，含 before/after 快照）
     */
    List<SeatOperationLogVo> listOperationLogs(Long activityId);

    /**
     * Boss 新增活动「背景画布加人」候选人员（T26.10）
     * <p>选人弹窗数据源：按角色分组返回租户下全部员工（导游/司机=salesman）
     * 与注册游客（customer），含头像；组名取租户角色显示名。
     *
     * @return 候选人分组列表（员工组 + 顾客组）
     */
    List<SeatCandidateVo> listSeatCandidates();

    /**
     * Boss 新增活动「背景画布加人」批量保存（T26.10）
     * <p>将画布上选好的【真实人员】（员工=导游/司机 或 游客）批量指派到座位：
     * 员工落 PRESET（橙色预置，不占顾客名额），游客落 BOSS_ASSIGN（已占）。
     * <p>★全量替换语义：仅替换本活动 PRESET/BOSS_ASSIGN 记录，顾客自选（CUSTOMER_PICK）不受影响；
     *   目标座位已被顾客自选时拒绝（ServiceException）。
     * <p>★服务端校验：活动未锁定 / 人员存在 / 同座位不重复 / 同人员不重复占多座。
     * <p>★操作日志 PRESET 由 {@code @SeatOperation} 切面自动记录（含前后座位快照）。
     *
     * @param bo 画布加人明细（activityId + items[]）
     */
    void saveCanvasPersons(SeatCanvasPersonBo bo);
}
