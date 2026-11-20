package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.annotation.SeatOperation;
import com.ruoyi.system.domain.ActivitySeatAssignment;
import com.ruoyi.system.domain.ActivitySeatOperationLog;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.TenantRoleMapping;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.SeatAssignBo;
import com.ruoyi.system.domain.bo.SeatCanvasPersonBo;
import com.ruoyi.system.domain.bo.SeatMoveBo;
import com.ruoyi.system.domain.bo.SeatMoveMineBo;
import com.ruoyi.system.domain.bo.SeatPickBo;
import com.ruoyi.system.domain.bo.SeatSwapBo;
import com.ruoyi.system.domain.enums.SeatOperationType;
import com.ruoyi.system.domain.vo.SeatAssignmentVo;
import com.ruoyi.system.domain.vo.SeatCandidateVo;
import com.ruoyi.system.domain.vo.SeatMapVo;
import com.ruoyi.system.domain.vo.SeatOperationLogVo;
import com.ruoyi.system.mapper.ActivitySeatAssignmentMapper;
import com.ruoyi.system.mapper.ActivitySeatOperationLogMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.TenantRoleMappingMapper;
import com.ruoyi.system.mapper.UserTenantRoleMapper;
import com.ruoyi.system.service.IActivitySeatOperationLogService;
import com.ruoyi.system.service.ISeatAssignmentService;
import com.ruoyi.system.service.ITenantRoleMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动座位占用服务实现（T26.7 / T26.8，小微版 M26）
 * <p>★顾客小程序按位置选座 / 取消选座；活动座位图查询。
 * <p>★联动 M28：活动锁定后顾客端选座接口拒绝（抛 ServiceException）。
 * <p>★防御式设计：T26.1 room_seat_layout / room_seat 表可能尚未落地，
 *   使用 INFORMATION_SCHEMA 检测表存在性，缺失时返回简化座位图（仅含 activity_seat_assignment 数据）。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAssignmentServiceImpl implements ISeatAssignmentService {

    private final ActivitySeatAssignmentMapper assignmentMapper;
    private final ActivitySeatOperationLogMapper operationLogMapper;
    private final PromotionActivityMapper activityMapper;
    private final SysUserMapper userMapper;
    private final UserTenantRoleMapper userTenantRoleMapper;
    private final TenantRoleMappingMapper tenantRoleMappingMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    /** M28 T28.4 操作日志服务（供 listOperationLogs 回放 + 切面快照复用） */
    private final IActivitySeatOperationLogService operationLogService;
    /** T26.10 租户角色显示名映射（候选人分组命名） */
    private final ITenantRoleMappingService tenantRoleMappingService;

    /** 座位状态：空闲 */
    private static final String STATUS_FREE = "FREE";
    /** 座位状态：已占 */
    private static final String STATUS_OCCUPIED = "OCCUPIED";
    /** 座位状态：预置人员 */
    private static final String STATUS_PRESET = "PRESET";

    /** 小微标准角色编码（T26.10 画布加人区分员工/游客） */
    private static final String ROLE_SALESMAN = "salesman";
    private static final String ROLE_CUSTOMER = "customer";

    // ============================================================
    // 查询活动座位图（T26.7）
    // ============================================================

    @Override
    public SeatMapVo getSeatMap(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        // ① 查询活动基础信息
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }

        SeatMapVo vo = new SeatMapVo();
        vo.setActivityId(activityId);
        vo.setActivityTitle(StrUtil.isNotBlank(activity.getAutoTitle()) ? activity.getAutoTitle() : activity.getTitle());
        // ★活动锁定状态（M28 T28.1 联动；isLocked 字段 M22 已补）
        vo.setActivityLocked(activity.getIsLocked() != null && activity.getIsLocked());

        // ② 解析活动关联的房间ID（selectedRoomIds JSON 数组，取第一个作为主房间）
        Long roomId = resolvePrimaryRoomId(activity);
        vo.setRoomId(roomId);

        // ③ 查询 activity_seat_assignment（当前活动的所有占座记录）
        List<ActivitySeatAssignment> assignments = assignmentMapper.selectList(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .orderByAsc(ActivitySeatAssignment::getSeatNo));

        // ④ 尝试查询 room_seat_layout + room_seat（T26.1 建表；若表不存在则降级）
        List<SeatMapVo.SeatVo> seats = new ArrayList<>();
        boolean layoutAvailable = isTableExists("room_seat_layout") && isTableExists("room_seat");
        Long layoutId = null;
        String bgImageUrl = null;
        String bgImageSource = null;
        Integer layoutWidth = null;
        Integer layoutHeight = null;

        if (layoutAvailable && roomId != null) {
            // 查询布局主表
            Map<String, Object> layoutRow = queryLayoutByRoomId(roomId);
            if (layoutRow != null) {
                layoutId = toLong(layoutRow.get("id"));
                bgImageUrl = toStr(layoutRow.get("bg_image_url"));
                bgImageSource = toStr(layoutRow.get("bg_image_source"));
                layoutWidth = toInt(layoutRow.get("layout_width"));
                layoutHeight = toInt(layoutRow.get("layout_height"));
                // 查询该布局下的所有座位点
                List<Map<String, Object>> seatRows = querySeatsByLayoutId(layoutId);
                // 构建 seatId → assignment 映射
                Map<Long, ActivitySeatAssignment> seatAssignMap = assignments.stream()
                    .filter(a -> a.getSeatId() != null)
                    .collect(Collectors.toMap(ActivitySeatAssignment::getSeatId, a -> a, (a, b) -> a));
                // 合并座位点 + 占座信息
                for (Map<String, Object> row : seatRows) {
                    SeatMapVo.SeatVo seat = new SeatMapVo.SeatVo();
                    Long seatId = toLong(row.get("id"));
                    seat.setSeatId(seatId);
                    seat.setSeatNo(toStr(row.get("seat_no")));
                    seat.setPosXPercent(toBigDecimal(row.get("pos_x_percent")));
                    seat.setPosYPercent(toBigDecimal(row.get("pos_y_percent")));
                    Integer isPreset = toInt(row.get("is_preset"));
                    seat.setIsPreset(isPreset);
                    seat.setPresetRole(toStr(row.get("preset_role")));
                    seat.setPresetUserId(toLong(row.get("preset_user_id")));
                    seat.setRemark(toStr(row.get("remark")));
                    // 状态判定：预置（房间级静态 is_preset=1 OR 活动级 Boss 加员工 type=PRESET）> 已占 > 空闲
                    ActivitySeatAssignment assign = seatAssignMap.get(seatId);
                    boolean activityPreset = assign != null
                        && ActivitySeatAssignment.TYPE_PRESET.equals(assign.getAssignmentType());
                    if ((isPreset != null && isPreset == 1) || activityPreset) {
                        seat.setStatus(STATUS_PRESET);
                        seat.setIsPreset(1);
                        if (assign != null) {
                            seat.setAssignmentId(assign.getId());
                            seat.setUserId(assign.getUserId());
                            seat.setAssignmentType(assign.getAssignmentType());
                        }
                    } else if (assign != null && assign.getUserId() != null) {
                        seat.setStatus(STATUS_OCCUPIED);
                        seat.setAssignmentId(assign.getId());
                        seat.setUserId(assign.getUserId());
                        seat.setAssignmentType(assign.getAssignmentType());
                    } else {
                        seat.setStatus(STATUS_FREE);
                    }
                    seats.add(seat);
                }
            }
        }

        // ⑤ 若布局不可用，降级：仅基于 activity_seat_assignment 构建简化座位列表
        if (!layoutAvailable || layoutId == null) {
            log.debug("[SeatMap] 房间布局表未就绪或房间未配置布局，降级返回简化座位图：activityId={} roomId={}", activityId, roomId);
            seats = assignments.stream().map(a -> {
                SeatMapVo.SeatVo seat = new SeatMapVo.SeatVo();
                seat.setSeatId(a.getSeatId());
                seat.setSeatNo(a.getSeatNo());
                // ★T26.10 降级场景同样按 assignment_type 识别预置（Boss 加员工）
                boolean activityPreset = ActivitySeatAssignment.TYPE_PRESET.equals(a.getAssignmentType());
                seat.setStatus(activityPreset ? STATUS_PRESET : (a.getUserId() != null ? STATUS_OCCUPIED : STATUS_FREE));
                seat.setIsPreset(activityPreset ? 1 : 0);
                seat.setAssignmentId(a.getId());
                seat.setUserId(a.getUserId());
                seat.setAssignmentType(a.getAssignmentType());
                seat.setRemark(a.getRemark());
                return seat;
            }).collect(Collectors.toList());
        }

        // ⑥ 批量回填用户昵称 / 头像
        fillUserInfo(seats);

        // ⑦ 标记当前登录用户的座位
        Long currentUserId = LoginHelper.getUserId();
        if (currentUserId != null) {
            seats.stream()
                .filter(s -> currentUserId.equals(s.getUserId()))
                .findFirst()
                .ifPresent(s -> vo.setMySeatId(s.getSeatId()));
        }

        // ⑧ 统计
        vo.setLayoutId(layoutId);
        vo.setBgImageUrl(bgImageUrl);
        vo.setBgImageSource(bgImageSource);
        vo.setLayoutWidth(layoutWidth != null ? layoutWidth : 1920);
        vo.setLayoutHeight(layoutHeight != null ? layoutHeight : 1080);
        vo.setSeats(seats);
        vo.setTotalSeats(seats.size());
        vo.setOccupiedSeats((int) seats.stream().filter(s -> STATUS_OCCUPIED.equals(s.getStatus())).count());
        vo.setPresetSeats((int) seats.stream().filter(s -> STATUS_PRESET.equals(s.getStatus())).count());
        vo.setFreeSeats((int) seats.stream().filter(s -> STATUS_FREE.equals(s.getStatus())).count());

        log.info("[SeatMap] activityId={} total={} free={} occupied={} preset={} layoutAvailable={}",
            activityId, vo.getTotalSeats(), vo.getFreeSeats(), vo.getOccupiedSeats(), vo.getPresetSeats(), layoutAvailable);
        return vo;
    }

    // ============================================================
    // 顾客选座（T26.7）
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long pickSeat(SeatPickBo bo) {
        // ① 参数校验
        Objects.requireNonNull(bo, "选座参数不能为空");
        Objects.requireNonNull(bo.getActivityId(), "活动ID不能为空");
        Objects.requireNonNull(bo.getSeatId(), "座位ID不能为空");

        Long activityId = bo.getActivityId();
        Long seatId = bo.getSeatId();
        Long currentUserId = LoginHelper.getUserId();
        if (currentUserId == null) {
            throw new ServiceException("用户未登录");
        }

        // ② 校验活动存在且未锁定
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (activity.getIsLocked() != null && activity.getIsLocked()) {
            throw new ServiceException("活动已锁定，无法调整座位");
        }

        // ③ 校验当前用户未在该活动重复占座
        Long existCount = assignmentMapper.selectCount(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .eq(ActivitySeatAssignment::getUserId, currentUserId));
        if (existCount != null && existCount > 0) {
            throw new ServiceException("您已在本活动中选座，请先取消原座位");
        }

        // ④ 校验目标座位空闲（UNIQUE(activity_id, seat_id) 兜底）
        ActivitySeatAssignment existSeat = assignmentMapper.selectOne(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .eq(ActivitySeatAssignment::getSeatId, seatId));
        if (existSeat != null && existSeat.getUserId() != null) {
            throw new ServiceException("该座位已被占用，请选择其他座位");
        }

        // ⑤ 写入占座记录
        ActivitySeatAssignment assignment = new ActivitySeatAssignment();
        assignment.setActivityId(activityId);
        assignment.setSeatId(seatId);
        assignment.setSeatNo(bo.getSeatNo());
        assignment.setUserId(currentUserId);
        assignment.setAssignmentType(ActivitySeatAssignment.TYPE_CUSTOMER_PICK);
        assignment.setAssignTime(new Date());
        assignment.setLocked(0);
        assignment.setRemark(bo.getRemark());
        try {
            assignmentMapper.insert(assignment);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // UNIQUE(activity_id, seat_id) 冲突：并发选座
            log.warn("[SeatPick] 并发选座冲突 activityId={} seatId={} userId={}", activityId, seatId, currentUserId);
            throw new ServiceException("该座位刚刚被他人选中，请重新选择");
        }

        // ⑥ 写入操作日志（ASSIGN）
        recordOperationLog(activityId, ActivitySeatOperationLog.OP_ASSIGN,
            currentUserId, "customer", null, buildAssignmentSnapshot(assignment), "顾客自选座位");

        log.info("[SeatPick] 选座成功 activityId={} seatId={} seatNo={} userId={} assignmentId={}",
            activityId, seatId, bo.getSeatNo(), currentUserId, assignment.getId());
        return assignment.getId();
    }

    // ============================================================
    // 顾客取消自己的座位（T26.7）
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeat(Long assignmentId) {
        if (assignmentId == null) {
            throw new ServiceException("占座记录ID不能为空");
        }
        Long currentUserId = LoginHelper.getUserId();
        if (currentUserId == null) {
            throw new ServiceException("用户未登录");
        }

        // ① 查询占座记录
        ActivitySeatAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new ServiceException("占座记录不存在");
        }
        // ② 归属校验：仅可取消自己的座位
        if (!currentUserId.equals(assignment.getUserId())) {
            throw new ServiceException("只能取消本人的座位");
        }
        // ③ 锁定校验
        if (assignment.getLocked() != null && assignment.getLocked() == 1) {
            throw new ServiceException("活动已锁定，无法调整座位");
        }
        PromotionActivity activity = activityMapper.selectById(assignment.getActivityId());
        if (activity != null && activity.getIsLocked() != null && activity.getIsLocked()) {
            throw new ServiceException("活动已锁定，无法调整座位");
        }

        // ④ 删除占座记录
        String beforeSnapshot = buildAssignmentSnapshot(assignment);
        assignmentMapper.deleteById(assignmentId);

        // ⑤ 写入操作日志（RELEASE）
        recordOperationLog(assignment.getActivityId(), ActivitySeatOperationLog.OP_RELEASE,
            currentUserId, "customer", beforeSnapshot, null, "顾客取消座位");

        log.info("[SeatRelease] 取消座位成功 assignmentId={} activityId={} seatId={} userId={}",
            assignmentId, assignment.getActivityId(), assignment.getSeatId(), currentUserId);
    }

    // ============================================================
    // 查询活动座位占用列表（Boss 端 / 内部使用）
    // ============================================================

    @Override
    public List<SeatAssignmentVo> listAssignments(Long activityId) {
        if (activityId == null) {
            return Collections.emptyList();
        }
        List<ActivitySeatAssignment> list = assignmentMapper.selectList(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .orderByAsc(ActivitySeatAssignment::getSeatNo));
        return toVoList(list);
    }

    // ============================================================
    // M28 T28.2 Boss 编排接口（加人 / 移座 / 互换）
    //   ★操作日志由 @SeatOperation 切面（SeatOperationLogAspect）自动记录 before/after 快照
    //   ★所有接口先校验 activity.is_locked=0，锁定则抛 ServiceException("活动已锁定，请先解锁")
    // ============================================================

    @SeatOperation(SeatOperationType.ASSIGN)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bossAssign(SeatAssignBo bo) {
        // ① fail-fast 入参校验
        Objects.requireNonNull(bo, "加人参数不能为空");
        Long activityId = bo.getActivityId();
        Long seatId = bo.getSeatId();
        Long userId = bo.getUserId();
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        if (seatId == null) {
            throw new ServiceException("座位ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("被指派人员不能为空");
        }

        // ② 校验活动未锁定
        assertActivityEditable(activityId);

        // ③ 校验该人员未在本活动重复占座
        Long existCount = assignmentMapper.selectCount(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .eq(ActivitySeatAssignment::getUserId, userId));
        if (existCount != null && existCount > 0) {
            throw new ServiceException("该人员已在本活动中占座");
        }

        // ④ 目标座位处理：已占（有 user_id）拒绝；预留未指派（user_id=NULL）则填充；无记录则新增
        ActivitySeatAssignment existSeat = findAssignmentBySeat(activityId, seatId);
        String seatNo = resolveSeatNo(seatId, bo.getSeatNo());
        if (existSeat != null && existSeat.getUserId() != null) {
            throw new ServiceException("该座位已被占用，请选择其他座位");
        }
        if (existSeat != null) {
            // 预留未指派记录 → 填充占座人（保留 assignmentId，避免 UNIQUE 冲突）
            existSeat.setUserId(userId);
            existSeat.setAssignmentType(ActivitySeatAssignment.TYPE_BOSS_ASSIGN);
            existSeat.setAssignTime(new Date());
            existSeat.setLocked(0);
            existSeat.setSeatNo(seatNo);
            if (StrUtil.isNotBlank(bo.getRemark())) {
                existSeat.setRemark(bo.getRemark());
            }
            assignmentMapper.updateById(existSeat);
            log.info("[SeatAssign] Boss 加人(填充预留)成功 activityId={} seatId={} userId={} assignmentId={}",
                activityId, seatId, userId, existSeat.getId());
            return existSeat.getId();
        }

        // ⑤ 新增占座记录（UNIQUE(activity_id, seat_id) 兜底并发）
        ActivitySeatAssignment assignment = new ActivitySeatAssignment();
        assignment.setActivityId(activityId);
        assignment.setSeatId(seatId);
        assignment.setSeatNo(seatNo);
        assignment.setUserId(userId);
        assignment.setAssignmentType(ActivitySeatAssignment.TYPE_BOSS_ASSIGN);
        assignment.setAssignTime(new Date());
        assignment.setLocked(0);
        assignment.setRemark(bo.getRemark());
        try {
            assignmentMapper.insert(assignment);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("[SeatAssign] 并发加人冲突 activityId={} seatId={} userId={}", activityId, seatId, userId);
            throw new ServiceException("该座位刚刚被占用，请刷新后重试");
        }
        log.info("[SeatAssign] Boss 加人成功 activityId={} seatId={} seatNo={} userId={} assignmentId={}",
            activityId, seatId, seatNo, userId, assignment.getId());
        return assignment.getId();
    }

    @SeatOperation(SeatOperationType.MOVE)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveSeat(SeatMoveBo bo) {
        Objects.requireNonNull(bo, "移座参数不能为空");
        Long activityId = bo.getActivityId();
        Long assignmentId = bo.getAssignmentId();
        Long toSeatId = bo.getToSeatId();
        if (activityId == null || assignmentId == null || toSeatId == null) {
            throw new ServiceException("移座参数不完整");
        }
        // ① 校验活动未锁定
        assertActivityEditable(activityId);
        // ② 校验占座记录归属本活动
        ActivitySeatAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new ServiceException("占座记录不存在");
        }
        if (!activityId.equals(assignment.getActivityId())) {
            throw new ServiceException("占座记录不属于本活动");
        }
        if (toSeatId.equals(assignment.getSeatId())) {
            throw new ServiceException("目标座位与当前座位相同");
        }
        // ③ 目标座位必须空闲（预留未指派记录先清除，避免 UNIQUE 冲突）
        assertSeatFreeForMove(activityId, toSeatId);
        // ④ 移动：预置人员（导游/司机）assignment_type 保持不变，仅改座位
        assignment.setSeatId(toSeatId);
        assignment.setSeatNo(resolveSeatNo(toSeatId, bo.getToSeatNo()));
        assignmentMapper.updateById(assignment);
        log.info("[SeatMove] Boss 移座成功 activityId={} assignmentId={} toSeatId={} userId={} type={}",
            activityId, assignmentId, toSeatId, assignment.getUserId(), assignment.getAssignmentType());
    }

    @SeatOperation(SeatOperationType.SWAP)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void swapSeat(SeatSwapBo bo) {
        Objects.requireNonNull(bo, "互换参数不能为空");
        Long activityId = bo.getActivityId();
        Long idA = bo.getAssignmentIdA();
        Long idB = bo.getAssignmentIdB();
        if (activityId == null || idA == null || idB == null) {
            throw new ServiceException("互换参数不完整");
        }
        if (idA.equals(idB)) {
            throw new ServiceException("不能与自身互换座位");
        }
        // ① 校验活动未锁定
        assertActivityEditable(activityId);
        // ② 校验两条占座记录均归属本活动
        ActivitySeatAssignment a = assignmentMapper.selectById(idA);
        ActivitySeatAssignment b = assignmentMapper.selectById(idB);
        if (a == null || b == null) {
            throw new ServiceException("占座记录不存在");
        }
        if (!activityId.equals(a.getActivityId()) || !activityId.equals(b.getActivityId())) {
            throw new ServiceException("占座记录不属于本活动");
        }
        // ③ 交换两条记录的占座人（seat_id/seat_no 固定在各自记录上，规避 UNIQUE(activity_id,seat_id) 冲突）
        Long userA = a.getUserId();
        String typeA = a.getAssignmentType();
        String remarkA = a.getRemark();
        a.setUserId(b.getUserId());
        a.setAssignmentType(b.getAssignmentType());
        a.setRemark(b.getRemark());
        b.setUserId(userA);
        b.setAssignmentType(typeA);
        b.setRemark(remarkA);
        assignmentMapper.updateById(a);
        assignmentMapper.updateById(b);
        log.info("[SeatSwap] Boss 互换成功 activityId={} assignmentA={} assignmentB={}", activityId, idA, idB);
    }

    // ============================================================
    // M28 T28.3 顾客移座接口（仅自己的）
    // ============================================================

    @SeatOperation(SeatOperationType.MOVE)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveMine(SeatMoveMineBo bo) {
        Objects.requireNonNull(bo, "移座参数不能为空");
        Long activityId = bo.getActivityId();
        Long fromSeatId = bo.getFromSeatId();
        Long toSeatId = bo.getToSeatId();
        if (activityId == null || fromSeatId == null || toSeatId == null) {
            throw new ServiceException("移座参数不完整");
        }
        Long currentUserId = LoginHelper.getUserId();
        if (currentUserId == null) {
            throw new ServiceException("用户未登录");
        }
        // ① 校验活动未锁定
        assertActivityEditable(activityId);
        if (fromSeatId.equals(toSeatId)) {
            throw new ServiceException("目标座位与当前座位相同");
        }
        // ② 归属校验：from 座位占座人必须为当前登录用户
        ActivitySeatAssignment mine = assignmentMapper.selectOne(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .eq(ActivitySeatAssignment::getSeatId, fromSeatId)
                .eq(ActivitySeatAssignment::getUserId, currentUserId));
        if (mine == null) {
            throw new ServiceException("未找到您在该座位的占座记录，只能移动本人的座位");
        }
        // ③ 目标座位必须空闲
        assertSeatFreeForMove(activityId, toSeatId);
        // ④ 移动本人座位
        mine.setSeatId(toSeatId);
        mine.setSeatNo(resolveSeatNo(toSeatId, bo.getToSeatNo()));
        assignmentMapper.updateById(mine);
        log.info("[SeatMoveMine] 顾客移座成功 activityId={} userId={} fromSeatId={} toSeatId={}",
            activityId, currentUserId, fromSeatId, toSeatId);
    }

    // ============================================================
    // M28 T28.4 操作日志查询（Boss 端回放）
    // ============================================================

    @Override
    public List<SeatOperationLogVo> listOperationLogs(Long activityId) {
        return operationLogService.listByActivity(activityId);
    }

    // ============================================================
    // T26.10 Boss 背景画布加人（候选人 + 批量保存）
    // ============================================================

    @Override
    public List<SeatCandidateVo> listSeatCandidates() {
        // 租户角色显示名（salesman→导游/司机，customer→游客），未配置回退通用名
        Map<String, String> displayNames = tenantRoleMappingService.roleDisplayNameMap();
        String staffLabel = displayNames.getOrDefault(ROLE_SALESMAN, "员工");
        String customerLabel = displayNames.getOrDefault(ROLE_CUSTOMER, "顾客");
        List<SeatCandidateVo> groups = new ArrayList<>(2);
        groups.add(buildCandidateGroup(ROLE_SALESMAN, staffLabel));
        groups.add(buildCandidateGroup(ROLE_CUSTOMER, customerLabel));
        return groups;
    }

    /**
     * 构建某标准角色下的候选人分组（仅启用用户，含头像）
     */
    private SeatCandidateVo buildCandidateGroup(String roleCode, String roleLabel) {
        SeatCandidateVo group = new SeatCandidateVo();
        group.setRoleCode(roleCode);
        group.setRoleLabel(roleLabel);
        group.setPersons(Collections.emptyList());
        // ★从 user_tenant_role 查询本租户角色用户（而非 sys_user_role，
        //   避免 sys_user_role 中存在未通过人员管理分配的脏数据导致多出人员）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, roleCode)
                .eq(UserTenantRole::getStatus, "0"));
        if (utrList.isEmpty()) {
            return group;
        }
        List<Long> userIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        // sys_user 回填昵称/头像（仅启用用户）
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        List<SeatCandidateVo.Person> persons = users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .map(u -> {
                SeatCandidateVo.Person p = new SeatCandidateVo.Person();
                p.setUserId(u.getUserId());
                p.setName(u.getNickName());
                p.setAvatar(u.getAvatar());
                return p;
            })
            .collect(Collectors.toList());
        group.setPersons(persons);
        return group;
    }

    @SeatOperation(SeatOperationType.PRESET)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCanvasPersons(SeatCanvasPersonBo bo) {
        // ① fail-fast
        Objects.requireNonNull(bo, "画布加人参数不能为空");
        Long activityId = bo.getActivityId();
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        // ② 活动未锁定
        assertActivityEditable(activityId);

        List<SeatCanvasPersonBo.Item> items = bo.getItems() == null ? Collections.emptyList() : bo.getItems();

        // ③ 明细校验：seatId/userId 非空、座位去重、人员去重
        Set<Long> seatIdSet = new HashSet<>();
        Set<Long> userIdSet = new HashSet<>();
        for (SeatCanvasPersonBo.Item it : items) {
            if (it.getSeatId() == null) {
                throw new ServiceException("座位ID不能为空");
            }
            if (it.getUserId() == null) {
                throw new ServiceException("被加人不能为空");
            }
            if (!seatIdSet.add(it.getSeatId())) {
                throw new ServiceException("同一座位不能重复加人");
            }
            if (!userIdSet.add(it.getUserId())) {
                throw new ServiceException("同一人员不能占用多个座位");
            }
        }

        // ④ 现有占座记录：区分顾客自选（保护）与预置/指派（全量替换）
        List<ActivitySeatAssignment> existing = assignmentMapper.selectList(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId));
        Set<Long> customerPickSeatIds = existing.stream()
            .filter(a -> ActivitySeatAssignment.TYPE_CUSTOMER_PICK.equals(a.getAssignmentType()))
            .map(ActivitySeatAssignment::getSeatId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> customerPickUserIds = existing.stream()
            .filter(a -> ActivitySeatAssignment.TYPE_CUSTOMER_PICK.equals(a.getAssignmentType()))
            .map(ActivitySeatAssignment::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        // 目标座位若已被顾客自选 → 拒绝
        for (Long seatId : seatIdSet) {
            if (customerPickSeatIds.contains(seatId)) {
                throw new ServiceException("目标座位已被顾客自选，不能设为预置人员");
            }
        }
        // 目标人员若已顾客自选占座 → 拒绝（避免同人双记录）
        for (Long uid : userIdSet) {
            if (customerPickUserIds.contains(uid)) {
                throw new ServiceException("该游客已自行选座，不能再由老板指派");
            }
        }

        // ⑤ 人员存在性 + 角色解析（salesman→PRESET，其余→BOSS_ASSIGN）
        Map<Long, String> userRoleMap = resolveUserRoles(userIdSet);
        if (!userIdSet.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(new ArrayList<>(userIdSet));
            Set<Long> existingUserIds = users.stream().map(SysUser::getUserId).collect(Collectors.toSet());
            for (Long uid : userIdSet) {
                if (!existingUserIds.contains(uid)) {
                    throw new ServiceException("被加人不存在，请刷新后重试");
                }
            }
        }

        // ⑥ 删除本活动全部非顾客自选记录（全量替换 PRESET/BOSS_ASSIGN）
        List<Long> toDeleteIds = existing.stream()
            .filter(a -> !ActivitySeatAssignment.TYPE_CUSTOMER_PICK.equals(a.getAssignmentType()))
            .map(ActivitySeatAssignment::getId).collect(Collectors.toList());
        if (!toDeleteIds.isEmpty()) {
            assignmentMapper.deleteBatchIds(toDeleteIds);
        }

        // ⑦ 逐条写入
        Date now = new Date();
        int presetCnt = 0;
        int assignCnt = 0;
        for (SeatCanvasPersonBo.Item it : items) {
            boolean isStaff = ROLE_SALESMAN.equals(userRoleMap.get(it.getUserId()));
            ActivitySeatAssignment entity = new ActivitySeatAssignment();
            entity.setActivityId(activityId);
            entity.setSeatId(it.getSeatId());
            entity.setSeatNo(resolveSeatNo(it.getSeatId(), it.getSeatNo()));
            entity.setUserId(it.getUserId());
            entity.setAssignmentType(isStaff ? ActivitySeatAssignment.TYPE_PRESET : ActivitySeatAssignment.TYPE_BOSS_ASSIGN);
            entity.setAssignTime(now);
            entity.setLocked(0);
            entity.setRemark(it.getRemark());
            try {
                assignmentMapper.insert(entity);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.warn("[SeatCanvas] 并发加人冲突 activityId={} seatId={} userId={}", activityId, it.getSeatId(), it.getUserId());
                throw new ServiceException("座位刚刚被占用，请刷新后重试");
            }
            if (isStaff) { presetCnt++; } else { assignCnt++; }
        }
        log.info("[SeatCanvas] 画布加人保存成功 activityId={} 员工(PRESET)={} 游客(BOSS_ASSIGN)={} 清空旧记录={}",
            activityId, presetCnt, assignCnt, toDeleteIds.size());
    }

    /**
     * 解析用户在本租户的标准角色编码（boss/salesman/customer），用于画布加人区分员工/游客。
     * <p>一个用户可能有多角色，按 boss &gt; salesman &gt; customer 优先级取主角色；无小微角色不入表。
     */
    private Map<Long, String> resolveUserRoles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // ★从 user_tenant_role 查询本租户角色（而非 sys_user_role，避免脏数据）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .in(UserTenantRole::getRoleCode, Arrays.asList("boss", ROLE_SALESMAN, ROLE_CUSTOMER))
                .in(UserTenantRole::getUserId, userIds)
                .eq(UserTenantRole::getStatus, "0"));
        if (utrList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (UserTenantRole utr : utrList) {
            String key = utr.getRoleCode();
            String cur = result.get(utr.getUserId());
            if (cur == null || rolePriority(key) < rolePriority(cur)) {
                result.put(utr.getUserId(), key);
            }
        }
        return result;
    }

    /** 小微角色优先级（boss &gt; salesman &gt; customer） */
    private int rolePriority(String roleCode) {
        if ("boss".equals(roleCode)) return 0;
        if (ROLE_SALESMAN.equals(roleCode)) return 1;
        if (ROLE_CUSTOMER.equals(roleCode)) return 2;
        return 9;
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    /**
     * 解析活动关联的主房间ID（selectedRoomIds JSON 数组，取第一个）
     */
    private Long resolvePrimaryRoomId(PromotionActivity activity) {
        String json = activity.getSelectedRoomIds();
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            List<Long> roomIds = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return (roomIds != null && !roomIds.isEmpty()) ? roomIds.get(0) : null;
        } catch (JsonProcessingException e) {
            log.warn("[SeatMap] 解析 selectedRoomIds JSON 失败: {}", json, e);
            return null;
        }
    }

    /**
     * 检测表是否存在（INFORMATION_SCHEMA 守卫；T26.1 room_seat_layout / room_seat 可能尚未落地）
     */
    private boolean isTableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("[SeatMap] 检测表存在性失败: {}", tableName, e);
            return false;
        }
    }

    /**
     * 查询房间布局主表（room_seat_layout）
     */
    private Map<String, Object> queryLayoutByRoomId(Long roomId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, bg_image_url, bg_image_source, layout_width, layout_height " +
                "FROM room_seat_layout WHERE room_id = ? LIMIT 1", roomId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.debug("[SeatMap] 查询 room_seat_layout 失败: roomId={}", roomId, e);
            return null;
        }
    }

    /**
     * 查询布局下的座位点（room_seat）
     */
    private List<Map<String, Object>> querySeatsByLayoutId(Long layoutId) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT id, seat_no, pos_x_percent, pos_y_percent, is_preset, preset_role, preset_user_id, remark " +
                "FROM room_seat WHERE layout_id = ? ORDER BY seat_no", layoutId);
        } catch (Exception e) {
            log.debug("[SeatMap] 查询 room_seat 失败: layoutId={}", layoutId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量回填用户昵称 / 头像到座位列表
     */
    private void fillUserInfo(List<SeatMapVo.SeatVo> seats) {
        if (seats == null || seats.isEmpty()) {
            return;
        }
        Set<Long> userIds = seats.stream()
            .map(SeatMapVo.SeatVo::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUser> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        // ★M15_32 解析占座人角色→显示名（顾客端弹窗展示，优先取用户级角色映射行的显示名，同 role_code 多业务名时区分导游/司机）
        Map<Long, String> roleCodeMap = resolveUserRoles(userIds);
        Map<String, String> displayNames = tenantRoleMappingService.roleDisplayNameMap();
        // 用户级角色映射ID → 显示名（同 UserTenantRoleServiceImpl.fillDisplay 逻辑）
        Map<Long, Long> userMappingIdMap = userTenantRoleMapper.selectList(
                Wrappers.<UserTenantRole>lambdaQuery().in(UserTenantRole::getUserId, userIds))
            .stream()
            .filter(r -> r.getRoleMappingId() != null)
            .collect(Collectors.toMap(UserTenantRole::getUserId, UserTenantRole::getRoleMappingId, (a, b) -> a));
        Map<Long, String> mappingNameMap = tenantRoleMappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery())
            .stream()
            .collect(Collectors.toMap(TenantRoleMapping::getId, TenantRoleMapping::getDisplayName, (a, b) -> a));
        for (SeatMapVo.SeatVo seat : seats) {
            if (seat.getUserId() == null) continue;
            SysUser u = userMap.get(seat.getUserId());
            if (u != null) {
                seat.setUserNickName(u.getNickName());
                seat.setUserAvatar(u.getAvatar());
            }
            // ★优先取用户级角色映射显示名（如司机/导游），回退到标准角色首个显示名
            Long mappingId = userMappingIdMap.get(seat.getUserId());
            if (mappingId != null && mappingNameMap.containsKey(mappingId)) {
                seat.setRoleDisplayName(mappingNameMap.get(mappingId));
            } else {
                String roleCode = roleCodeMap.get(seat.getUserId());
                if (StrUtil.isNotBlank(roleCode)) {
                    seat.setRoleDisplayName(displayNames.get(roleCode));
                }
            }
        }
    }

    /**
     * 实体列表 → VO 列表（含用户昵称 / 头像回填）
     */
    private List<SeatAssignmentVo> toVoList(List<ActivitySeatAssignment> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = list.stream()
            .map(ActivitySeatAssignment::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
            if (users != null && !users.isEmpty()) {
                userMap = users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
            }
        }
        final Map<Long, SysUser> finalUserMap = userMap;
        // ★T26.10 回填占座人标准角色（员工/游客），供画布回显与名单区分
        final Map<Long, String> roleMap = resolveUserRoles(userIds);
        return list.stream().map(entity -> {
            SeatAssignmentVo vo = new SeatAssignmentVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setAssignmentTypeStr(translateAssignmentType(entity.getAssignmentType()));
            SysUser u = finalUserMap.get(entity.getUserId());
            if (u != null) {
                vo.setUserNickName(u.getNickName());
                vo.setUserAvatar(u.getAvatar());
            }
            if (entity.getUserId() != null) {
                vo.setRoleCode(roleMap.get(entity.getUserId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 占座类型中文翻译
     */
    private String translateAssignmentType(String type) {
        if (type == null) return "";
        switch (type) {
            case ActivitySeatAssignment.TYPE_PRESET: return "预置人员";
            case ActivitySeatAssignment.TYPE_BOSS_ASSIGN: return "Boss 指派";
            case ActivitySeatAssignment.TYPE_CUSTOMER_PICK: return "顾客自选";
            default: return type;
        }
    }

    /**
     * 构建占座记录快照 JSON（用于操作日志 before/after）
     */
    private String buildAssignmentSnapshot(ActivitySeatAssignment a) {
        if (a == null) return null;
        Map<String, Object> snap = new LinkedHashMap<>(6);
        snap.put("assignmentId", a.getId());
        snap.put("activityId", a.getActivityId());
        snap.put("seatId", a.getSeatId());
        snap.put("seatNo", a.getSeatNo());
        snap.put("userId", a.getUserId());
        snap.put("assignmentType", a.getAssignmentType());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            log.warn("[SeatOpLog] 快照序列化失败", e);
            return null;
        }
    }

    /**
     * 写入操作日志（T26.8 / T28.4）
     */
    private void recordOperationLog(Long activityId, String operationType,
                                    Long operatorUserId, String operatorRole,
                                    String beforeSnapshot, String afterSnapshot, String remark) {
        try {
            ActivitySeatOperationLog logEntity = new ActivitySeatOperationLog();
            logEntity.setActivityId(activityId);
            logEntity.setOperationType(operationType);
            logEntity.setOperatorUserId(operatorUserId);
            logEntity.setOperatorRole(operatorRole);
            logEntity.setBeforeSnapshot(beforeSnapshot);
            logEntity.setAfterSnapshot(afterSnapshot);
            logEntity.setOperateTime(new Date());
            logEntity.setRemark(remark);
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            // 操作日志写入失败不影响主流程（降级记录）
            log.error("[SeatOpLog] 写入操作日志失败 activityId={} type={}", activityId, operationType, e);
        }
    }

    // ===== M28 编排校验辅助方法 =====

    /**
     * 校验活动存在且未锁定（锁定后所有编排动作拒绝，T28.1/T28.2/T28.3）
     *
     * @return 活动实体（供调用方复用）
     */
    private PromotionActivity assertActivityEditable(Long activityId) {
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (activity.getIsLocked() != null && activity.getIsLocked()) {
            throw new ServiceException("活动已锁定，请先解锁");
        }
        return activity;
    }

    /**
     * 按 (activityId, seatId) 查询占座记录（可能为 NULL）
     */
    private ActivitySeatAssignment findAssignmentBySeat(Long activityId, Long seatId) {
        if (activityId == null || seatId == null) {
            return null;
        }
        return assignmentMapper.selectOne(
            Wrappers.<ActivitySeatAssignment>lambdaQuery()
                .eq(ActivitySeatAssignment::getActivityId, activityId)
                .eq(ActivitySeatAssignment::getSeatId, seatId));
    }

    /**
     * 移座/互换前确保目标座位可用（T28.2/T28.3）：
     * <ul>
     *   <li>已占（user_id 非空）→ 抛 ServiceException("目标座位已被占用")</li>
     *   <li>预留未指派（user_id=NULL）→ 先删除该预留记录，腾出座位（避免 UNIQUE(activity_id,seat_id) 冲突）</li>
     *   <li>无记录 → 直接放行</li>
     * </ul>
     */
    private void assertSeatFreeForMove(Long activityId, Long seatId) {
        ActivitySeatAssignment exist = findAssignmentBySeat(activityId, seatId);
        if (exist == null) {
            return;
        }
        if (exist.getUserId() != null) {
            throw new ServiceException("目标座位已被占用，请选择其他座位");
        }
        // 预留未指派记录 → 清除
        assignmentMapper.deleteById(exist.getId());
        log.debug("[SeatMove] 清除目标座位预留记录 activityId={} seatId={} assignmentId={}", activityId, seatId, exist.getId());
    }

    /**
     * 解析座位号：前端传入优先；为空时防御式查 room_seat.seat_no（T26.1 未落地时返回 null）
     */
    private String resolveSeatNo(Long seatId, String fallback) {
        if (StrUtil.isNotBlank(fallback)) {
            return fallback;
        }
        if (seatId == null || !isTableExists("room_seat")) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT seat_no FROM room_seat WHERE id = ? LIMIT 1", seatId);
            return rows.isEmpty() ? null : toStr(rows.get(0).get("seat_no"));
        } catch (Exception e) {
            log.debug("[SeatOp] 查询座位号失败 seatId={}", seatId, e);
            return null;
        }
    }

    // ===== 类型转换工具 =====

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return null; }
    }

    private String toStr(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try { return new BigDecimal(obj.toString()); } catch (Exception e) { return null; }
    }
}
