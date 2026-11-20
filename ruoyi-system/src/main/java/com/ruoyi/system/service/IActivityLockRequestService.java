package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ActivityLockRequestVo;

import java.util.List;

/**
 * 活动锁定申请审批服务接口（M33 §九 老板/员工权限分工）
 * <p>确认下放给员工、锁定审批由老板把关：员工「申请锁定」提交 PENDING，
 * 老板「审批通过」触发活动真正锁定（{@code IPromotionActivityService#lockActivity}）或「驳回」。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IActivityLockRequestService {

    /**
     * 员工申请锁定活动（§九：申请锁定=员工提交）。
     * <p>校验：活动存在、未锁定、无进行中的待审批申请（防重复提交）。
     *
     * @param activityId 活动ID
     * @param reason     申请说明（可选）
     * @return 新建申请记录ID
     */
    Long apply(Long activityId, String reason);

    /**
     * 老板端锁定申请列表（§九：审批锁定申请=老板把关）。
     *
     * @param status 状态过滤：空=默认 PENDING（待审批）；可传 APPROVED/REJECTED 查历史
     * @return 申请列表（按申请时间倒序）
     */
    List<ActivityLockRequestVo> listRequests(String status);

    /**
     * 老板审批通过锁定申请：触发活动真正锁定 + 申请状态置 APPROVED。
     *
     * @param id 申请记录ID
     */
    void approve(Long id);

    /**
     * 老板驳回锁定申请：申请状态置 REJECTED，活动保持未锁定。
     *
     * @param id           申请记录ID
     * @param rejectReason 驳回原因（可选）
     */
    void reject(Long id, String rejectReason);

    /**
     * 判断活动是否存在待审批（PENDING）的锁定申请。
     * <p>供 {@code getSignupDetail} 回显员工端「锁定申请待审批」状态。
     *
     * @param activityId 活动ID
     * @return true=存在待审批申请
     */
    boolean hasPending(Long activityId);
}
