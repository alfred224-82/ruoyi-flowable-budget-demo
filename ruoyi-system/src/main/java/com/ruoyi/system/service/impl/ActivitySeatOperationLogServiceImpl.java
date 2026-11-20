package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ActivitySeatAssignment;
import com.ruoyi.system.domain.ActivitySeatOperationLog;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.vo.SeatOperationLogVo;
import com.ruoyi.system.mapper.ActivitySeatAssignmentMapper;
import com.ruoyi.system.mapper.ActivitySeatOperationLogMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IActivitySeatOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动座位操作日志服务实现（T28.4，小微版 M28）
 * <p>供 {@code SeatOperationLogAspect} 切面调用，自动构建操作前后座位快照并落库；
 * 全部写日志动作 fail-soft——日志失败不阻断主编排流程（仅 error 记录）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySeatOperationLogServiceImpl implements IActivitySeatOperationLogService {

    private final ActivitySeatOperationLogMapper operationLogMapper;
    private final ActivitySeatAssignmentMapper assignmentMapper;
    private final PromotionActivityMapper activityMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String snapshotSeats(Long activityId) {
        if (activityId == null) {
            return null;
        }
        try {
            Map<String, Object> snap = new LinkedHashMap<>(4);
            // 活动锁定状态（回放时可见锁定前后 is_locked 变化）
            PromotionActivity activity = activityMapper.selectById(activityId);
            snap.put("activityId", activityId);
            snap.put("activityLocked", activity != null && Boolean.TRUE.equals(activity.getIsLocked()));
            // 当前全部座位占用（按座位号升序，保证前后快照可对比）
            List<ActivitySeatAssignment> assignments = assignmentMapper.selectList(
                Wrappers.<ActivitySeatAssignment>lambdaQuery()
                    .eq(ActivitySeatAssignment::getActivityId, activityId)
                    .orderByAsc(ActivitySeatAssignment::getSeatNo));
            List<Map<String, Object>> seatList = new ArrayList<>(assignments.size());
            for (ActivitySeatAssignment a : assignments) {
                Map<String, Object> cell = new LinkedHashMap<>(6);
                cell.put("assignmentId", a.getId());
                cell.put("seatId", a.getSeatId());
                cell.put("seatNo", a.getSeatNo());
                cell.put("userId", a.getUserId());
                cell.put("assignmentType", a.getAssignmentType());
                cell.put("locked", a.getLocked());
                seatList.add(cell);
            }
            snap.put("seats", seatList);
            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            log.warn("[SeatOpLog] 构建座位快照失败 activityId={}", activityId, e);
            return null;
        }
    }

    @Override
    public void record(Long activityId, String operationType, String beforeSnapshot, String afterSnapshot, String remark) {
        if (activityId == null || operationType == null) {
            log.warn("[SeatOpLog] 记录参数不完整，跳过：activityId={} operationType={}", activityId, operationType);
            return;
        }
        try {
            ActivitySeatOperationLog entity = new ActivitySeatOperationLog();
            entity.setActivityId(activityId);
            entity.setOperationType(operationType);
            entity.setOperatorUserId(LoginHelper.getUserId());
            entity.setOperatorRole(resolveRoleKey());
            entity.setBeforeSnapshot(beforeSnapshot);
            entity.setAfterSnapshot(afterSnapshot);
            entity.setOperateTime(new Date());
            entity.setRemark(remark);
            operationLogMapper.insert(entity);
            log.info("[SeatOpLog] activityId={} type={} operator={} role={} logId={}",
                activityId, operationType, entity.getOperatorUserId(), entity.getOperatorRole(), entity.getId());
        } catch (Exception e) {
            // 操作日志写入失败不影响主流程（降级记录）
            log.error("[SeatOpLog] 写入操作日志失败 activityId={} type={}", activityId, operationType, e);
        }
    }

    @Override
    public List<SeatOperationLogVo> listByActivity(Long activityId) {
        if (activityId == null) {
            return Collections.emptyList();
        }
        List<ActivitySeatOperationLog> list = operationLogMapper.selectList(
            Wrappers.<ActivitySeatOperationLog>lambdaQuery()
                .eq(ActivitySeatOperationLog::getActivityId, activityId)
                .orderByDesc(ActivitySeatOperationLog::getOperateTime));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 回填操作人昵称
        Set<Long> userIds = list.stream()
            .map(ActivitySeatOperationLog::getOperatorUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
            if (users != null && !users.isEmpty()) {
                userMap = users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
            }
        }
        // 回填活动标题
        PromotionActivity activity = activityMapper.selectById(activityId);
        String activityTitle = activity != null ? activity.getTitle() : null;
        final Map<Long, SysUser> finalUserMap = userMap;
        return list.stream().map(entity -> {
            SeatOperationLogVo vo = new SeatOperationLogVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setActivityTitle(activityTitle);
            vo.setOperationTypeStr(translateOperationType(entity.getOperationType()));
            SysUser u = finalUserMap.get(entity.getOperatorUserId());
            if (u != null) {
                vo.setOperatorNickName(u.getNickName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 从当前登录用户 roles 列表取第一个 roleKey（boss/salesman/customer）
     */
    private String resolveRoleKey() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null && loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
                RoleDTO first = loginUser.getRoles().get(0);
                return first.getRoleKey();
            }
        } catch (Exception e) {
            log.warn("[SeatOpLog] 解析操作人角色失败", e);
        }
        return null;
    }

    /**
     * 操作类型编码 → 中文
     */
    private String translateOperationType(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case ActivitySeatOperationLog.OP_ASSIGN: return "加人到座位";
            case ActivitySeatOperationLog.OP_MOVE: return "移动座位";
            case ActivitySeatOperationLog.OP_SWAP: return "互换座位";
            case ActivitySeatOperationLog.OP_LOCK: return "锁定活动";
            case ActivitySeatOperationLog.OP_UNLOCK: return "解锁活动";
            case ActivitySeatOperationLog.OP_COPY_LAYOUT: return "复制布局";
            case ActivitySeatOperationLog.OP_PRESET: return "预置人员";
            case ActivitySeatOperationLog.OP_RELEASE: return "释放座位";
            default: return type;
        }
    }
}
