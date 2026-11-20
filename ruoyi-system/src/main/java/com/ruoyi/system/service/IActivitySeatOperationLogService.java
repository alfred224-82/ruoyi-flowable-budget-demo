package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.SeatOperationLogVo;

import java.util.List;

/**
 * 活动座位操作日志服务接口（T28.4，小微版 M28）
 * <p>供 {@code SeatOperationLogAspect} 切面调用，自动记录编排动作的操作前后快照；
 * 亦可被 Boss 端「操作日志」查询接口调用做回放。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public interface IActivitySeatOperationLogService {

    /**
     * 构建活动当前座位占用快照 JSON（含 activity_seat_assignment 全量 + 活动 is_locked 状态）。
     * <p>★查询失败 fail-soft 返回 {@code null}，不阻断主编排流程。
     *
     * @param activityId 活动ID
     * @return 快照 JSON 字符串
     */
    String snapshotSeats(Long activityId);

    /**
     * 记录一条座位编排操作日志。
     * <p>★operator_user_id / operator_role 由当前登录上下文自动解析；写入失败 fail-soft（不影响主流程）。
     *
     * @param activityId    活动ID
     * @param operationType 操作类型编码（ASSIGN/MOVE/SWAP/LOCK/UNLOCK/COPY_LAYOUT/PRESET/RELEASE）
     * @param beforeSnapshot 操作前快照 JSON
     * @param afterSnapshot  操作后快照 JSON
     * @param remark         备注
     */
    void record(Long activityId, String operationType, String beforeSnapshot, String afterSnapshot, String remark);

    /**
     * 查询活动的座位编排操作日志（按操作时间倒序，供 Boss 端回放）。
     *
     * @param activityId 活动ID
     * @return 操作日志列表（含操作人昵称 / 操作类型中文回填）
     */
    List<SeatOperationLogVo> listByActivity(Long activityId);
}
