package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ActivityLockRequest;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.vo.ActivityLockRequestVo;
import com.ruoyi.system.mapper.ActivityLockRequestMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.service.IActivityLockRequestService;
import com.ruoyi.system.service.IPromotionActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 活动锁定申请审批服务实现（M33 §九 老板/员工权限分工）
 * <p>员工「申请锁定」写入 PENDING → 老板「审批通过」委派 {@link IPromotionActivityService#lockActivity}
 * 触发真正锁定（is_locked=1 + 座位只读），或「驳回」置 REJECTED 保持未锁定。
 * <p>租户隔离由 MyBatis-Plus 多租户插件自动注入；申请人/审批人姓名取当前登录态快照。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLockRequestServiceImpl implements IActivityLockRequestService {

    private final ActivityLockRequestMapper lockRequestMapper;
    private final PromotionActivityMapper activityMapper;
    /** ★审批通过时委派既有锁定逻辑（含座位全量刷 locked + 操作日志切面） */
    private final IPromotionActivityService promotionActivityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long activityId, String reason) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (Boolean.TRUE.equals(activity.getIsLocked())) {
            throw new ServiceException("活动已锁定，无需重复申请");
        }
        if (hasPending(activityId)) {
            throw new ServiceException("该活动已有待审批的锁定申请，请勿重复提交");
        }
        ActivityLockRequest req = new ActivityLockRequest();
        req.setActivityId(activityId);
        req.setActivityTitle(activity.getTitle());
        req.setApplyStaffId(LoginHelper.getUserId());
        req.setApplyStaffName(LoginHelper.getNickName());
        req.setReason(reason);
        req.setStatus(ActivityLockRequest.STATUS_PENDING);
        lockRequestMapper.insert(req);
        log.info("[lockRequest.apply] activityId={} applyStaffId={} requestId={}",
            activityId, req.getApplyStaffId(), req.getId());
        return req.getId();
    }

    @Override
    public List<ActivityLockRequestVo> listRequests(String status) {
        String filter = StrUtil.isNotBlank(status) ? status : ActivityLockRequest.STATUS_PENDING;
        return lockRequestMapper.selectVoList(
            Wrappers.<ActivityLockRequest>lambdaQuery()
                .eq(ActivityLockRequest::getStatus, filter)
                .orderByDesc(ActivityLockRequest::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        ActivityLockRequest req = loadPending(id);
        // ① 触发活动真正锁定（is_locked=1 + 座位全量刷 locked，操作日志由 @SeatOperation 切面记录）
        promotionActivityService.lockActivity(req.getActivityId());
        // ② 申请置 APPROVED + 审批人快照
        ActivityLockRequest update = new ActivityLockRequest();
        update.setId(req.getId());
        update.setStatus(ActivityLockRequest.STATUS_APPROVED);
        update.setApproveBy(LoginHelper.getUserId());
        update.setApproveByName(LoginHelper.getNickName());
        update.setApproveTime(new Date());
        lockRequestMapper.updateById(update);
        log.info("[lockRequest.approve] requestId={} activityId={} approveBy={}",
            id, req.getActivityId(), update.getApproveBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String rejectReason) {
        ActivityLockRequest req = loadPending(id);
        ActivityLockRequest update = new ActivityLockRequest();
        update.setId(req.getId());
        update.setStatus(ActivityLockRequest.STATUS_REJECTED);
        update.setApproveBy(LoginHelper.getUserId());
        update.setApproveByName(LoginHelper.getNickName());
        update.setApproveTime(new Date());
        update.setRejectReason(rejectReason);
        lockRequestMapper.updateById(update);
        log.info("[lockRequest.reject] requestId={} activityId={} approveBy={} reason={}",
            id, req.getActivityId(), update.getApproveBy(), rejectReason);
    }

    @Override
    public boolean hasPending(Long activityId) {
        if (activityId == null) {
            return false;
        }
        return lockRequestMapper.selectCount(
            Wrappers.<ActivityLockRequest>lambdaQuery()
                .eq(ActivityLockRequest::getActivityId, activityId)
                .eq(ActivityLockRequest::getStatus, ActivityLockRequest.STATUS_PENDING)) > 0;
    }

    /**
     * 加载待审批申请，非 PENDING（已处理/不存在）直接 fail-fast。
     */
    private ActivityLockRequest loadPending(Long id) {
        if (id == null) {
            throw new ServiceException("申请ID不能为空");
        }
        ActivityLockRequest req = lockRequestMapper.selectById(id);
        if (req == null) {
            throw new ServiceException("锁定申请不存在");
        }
        if (!ActivityLockRequest.STATUS_PENDING.equals(req.getStatus())) {
            throw new ServiceException("该锁定申请已处理，请勿重复操作");
        }
        return req;
    }
}
