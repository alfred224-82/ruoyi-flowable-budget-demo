package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ScheduleChangeLog;
import com.ruoyi.system.domain.StaffSchedule;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.StaffScheduleBo;
import com.ruoyi.system.domain.vo.ScheduleChangeLogVo;
import com.ruoyi.system.domain.vo.StaffScheduleVo;
import com.ruoyi.system.mapper.ScheduleChangeLogMapper;
import com.ruoyi.system.mapper.StaffScheduleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.UserTenantRoleMapper;
import com.ruoyi.system.service.IMicroStaffScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工排班服务实现（T22.2，小微版 M22）
 * <p>★租户级表，多租户插件自动隔离；同一员工同一天同一班次 UNIQUE。
 * <p>★班次分早班 morning / 晚班 evening；起止时间由 Boss 自定，禁止硬编码。
 * <p>★draft→published；发布后仍可修正，修正写 {@link ScheduleChangeLog} 留痕。
 * <p>★员工/顾客端仅可见 published（由 Controller 传 statusOnlyPublished 控制）。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroStaffScheduleServiceImpl implements IMicroStaffScheduleService {

    private final StaffScheduleMapper scheduleMapper;
    private final ScheduleChangeLogMapper changeLogMapper;
    private final SysUserMapper userMapper;
    private final UserTenantRoleMapper userTenantRoleMapper;

    /** salesman 标准角色编码 */
    private static final String ROLE_KEY_SALESMAN = "salesman";

    /** 状态：草稿 */
    private static final String STATUS_DRAFT = "draft";
    /** 状态：已发布 */
    private static final String STATUS_PUBLISHED = "published";

    /** 班次类型：早班 */
    private static final String SHIFT_MORNING = "morning";
    /** 班次类型：晚班 */
    private static final String SHIFT_EVENING = "evening";

    /** 变更类型 */
    private static final String CHANGE_TYPE_TIME = "TIME";
    private static final String CHANGE_TYPE_SHIFT = "SHIFT";
    private static final String CHANGE_TYPE_STATUS = "STATUS";
    private static final String CHANGE_TYPE_DELETE = "DELETE";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ============================================================
    // Boss PC 管理端
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(StaffScheduleBo bo) {
        if (bo == null || CollUtil.isEmpty(bo.getItems())) {
            throw new ServiceException("排班条目不能为空");
        }
        if (StrUtil.isBlank(bo.getPeriodType()) || StrUtil.isBlank(bo.getPeriodKey())) {
            throw new ServiceException("周期类型/周期键不能为空");
        }
        // ① 校验员工范围（必须为本租户 salesman 角色）
        Set<Long> salesmanUserIds = getSalesmanUserIds();
        // ② 预查已存在的记录，避免 UNIQUE 冲突（tenant_id + staff_id + schedule_date + shift_type）
        Set<String> existKeys = loadExistingKeys(bo.getItems());
        // ③ 逐条插入
        int inserted = 0;
        for (StaffScheduleBo.ScheduleItemBo item : bo.getItems()) {
            if (item.getStaffId() == null || item.getScheduleDate() == null
                || StrUtil.isBlank(item.getShiftType())
                || item.getStartTime() == null || item.getEndTime() == null) {
                continue; // 数据不完整则跳过（前端应已校验）
            }
            if (!salesmanUserIds.isEmpty() && !salesmanUserIds.contains(item.getStaffId())) {
                log.warn("[staffSchedule] 跳过非 salesman 角色员工排班：staffId={}", item.getStaffId());
                continue;
            }
            if (!isValidShift(item.getShiftType())) {
                throw new ServiceException("班次类型非法：" + item.getShiftType());
            }
            if (item.getEndTime().isBefore(item.getStartTime())) {
                throw new ServiceException("班次结束时间不能早于开始时间："
                    + item.getScheduleDate() + " " + item.getShiftType());
            }
            String uniqueKey = buildUniqueKey(item.getStaffId(), item.getScheduleDate(), item.getShiftType());
            if (existKeys.contains(uniqueKey)) {
                // 幂等：同一天同一班次同一员工已存在，跳过
                continue;
            }
            StaffSchedule entity = new StaffSchedule();
            entity.setStaffId(item.getStaffId());
            entity.setScheduleDate(item.getScheduleDate());
            entity.setShiftType(item.getShiftType());
            entity.setStartTime(item.getStartTime());
            entity.setEndTime(item.getEndTime());
            entity.setStatus(STATUS_DRAFT);
            entity.setPeriodType(bo.getPeriodType());
            entity.setPeriodKey(bo.getPeriodKey());
            entity.setRemark(item.getRemark());
            scheduleMapper.insert(entity);
            existKeys.add(uniqueKey);
            inserted++;
        }
        return inserted;
    }

    @Override
    public List<StaffScheduleVo> listSchedules(String periodType, String periodKey, String status,
                                                Long staffId, LocalDate startDate, LocalDate endDate,
                                                boolean statusOnlyPublished) {
        LambdaQueryWrapper<StaffSchedule> wrapper = Wrappers.<StaffSchedule>lambdaQuery();
        if (StrUtil.isNotBlank(periodType)) {
            wrapper.eq(StaffSchedule::getPeriodType, periodType);
        }
        if (StrUtil.isNotBlank(periodKey)) {
            wrapper.eq(StaffSchedule::getPeriodKey, periodKey);
        }
        if (statusOnlyPublished) {
            // 员工/顾客端强制仅返回 published
            wrapper.eq(StaffSchedule::getStatus, STATUS_PUBLISHED);
        } else if (StrUtil.isNotBlank(status)) {
            wrapper.eq(StaffSchedule::getStatus, status);
        }
        if (staffId != null) {
            wrapper.eq(StaffSchedule::getStaffId, staffId);
        }
        if (startDate != null) {
            wrapper.ge(StaffSchedule::getScheduleDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(StaffSchedule::getScheduleDate, endDate);
        }
        wrapper.orderByAsc(StaffSchedule::getScheduleDate)
            .orderByAsc(StaffSchedule::getShiftType)
            .orderByAsc(StaffSchedule::getStaffId);
        List<StaffSchedule> list = scheduleMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 回填员工昵称/头像
        Map<Long, SysUser> userMap = loadUserMap(
            list.stream().map(StaffSchedule::getStaffId).distinct().collect(Collectors.toList()));
        return list.stream().map(entity -> {
            StaffScheduleVo vo = new StaffScheduleVo();
            BeanUtils.copyProperties(entity, vo);
            SysUser user = userMap.get(entity.getStaffId());
            if (user != null) {
                vo.setStaffName(user.getNickName());
                vo.setStaffAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publish(String periodType, String periodKey) {
        if (StrUtil.isBlank(periodType) || StrUtil.isBlank(periodKey)) {
            throw new ServiceException("周期类型/周期键不能为空");
        }
        // ① 查询该周期下所有 draft 记录
        List<StaffSchedule> drafts = scheduleMapper.selectList(
            Wrappers.<StaffSchedule>lambdaQuery()
                .eq(StaffSchedule::getPeriodType, periodType)
                .eq(StaffSchedule::getPeriodKey, periodKey)
                .eq(StaffSchedule::getStatus, STATUS_DRAFT));
        if (CollUtil.isEmpty(drafts)) {
            return 0;
        }
        // ② 批量更新状态
        Long operatorId = LoginHelper.getUserId();
        String operatorName = LoginHelper.getNickName();
        Date now = new Date();
        int published = 0;
        for (StaffSchedule draft : drafts) {
            StaffSchedule update = new StaffSchedule();
            update.setId(draft.getId());
            update.setStatus(STATUS_PUBLISHED);
            scheduleMapper.updateById(update);
            // ③ 写留痕日志（changeType=STATUS）
            ScheduleChangeLog logEntity = buildChangeLog(
                draft, CHANGE_TYPE_STATUS, null, null,
                STATUS_DRAFT, STATUS_PUBLISHED, "批量发布",
                operatorId, operatorName, now);
            changeLogMapper.insert(logEntity);
            published++;
        }
        return published;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long modify(StaffScheduleBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("排班记录ID不能为空");
        }
        StaffSchedule exist = scheduleMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("排班记录不存在");
        }
        if (StrUtil.isBlank(bo.getChangeReason())) {
            throw new ServiceException("修正原因不能为空");
        }
        // 计算变更类型（TIME / SHIFT）
        boolean timeChanged = (bo.getStartTime() != null && !bo.getStartTime().equals(exist.getStartTime()))
            || (bo.getEndTime() != null && !bo.getEndTime().equals(exist.getEndTime()));
        boolean shiftChanged = StrUtil.isNotBlank(bo.getShiftType())
            && !bo.getShiftType().equals(exist.getShiftType());
        if (!timeChanged && !shiftChanged) {
            // 无实际变更
            return exist.getId();
        }
        // 校验新班次
        String newShiftType = shiftChanged ? bo.getShiftType() : exist.getShiftType();
        if (shiftChanged && !isValidShift(newShiftType)) {
            throw new ServiceException("班次类型非法：" + newShiftType);
        }
        LocalTime newStartTime = bo.getStartTime() != null ? bo.getStartTime() : exist.getStartTime();
        LocalTime newEndTime = bo.getEndTime() != null ? bo.getEndTime() : exist.getEndTime();
        if (newEndTime.isBefore(newStartTime)) {
            throw new ServiceException("班次结束时间不能早于开始时间");
        }
        // 若变更了班次或日期，需检查 UNIQUE 冲突
        LocalDate newDate = bo.getScheduleDate() != null ? bo.getScheduleDate() : exist.getScheduleDate();
        Long newStaffId = bo.getStaffId() != null ? bo.getStaffId() : exist.getStaffId();
        if (shiftChanged || (bo.getScheduleDate() != null && !bo.getScheduleDate().equals(exist.getScheduleDate()))
            || (bo.getStaffId() != null && !bo.getStaffId().equals(exist.getStaffId()))) {
            Long conflict = scheduleMapper.selectCount(
                Wrappers.<StaffSchedule>lambdaQuery()
                    .eq(StaffSchedule::getStaffId, newStaffId)
                    .eq(StaffSchedule::getScheduleDate, newDate)
                    .eq(StaffSchedule::getShiftType, newShiftType)
                    .ne(StaffSchedule::getId, exist.getId()));
            if (conflict != null && conflict > 0) {
                throw new ServiceException("该员工当天当班次已存在排班，请勿重复");
            }
        }
        // 执行更新
        StaffSchedule update = new StaffSchedule();
        update.setId(exist.getId());
        if (shiftChanged) update.setShiftType(newShiftType);
        if (bo.getStartTime() != null) update.setStartTime(newStartTime);
        if (bo.getEndTime() != null) update.setEndTime(newEndTime);
        if (bo.getScheduleDate() != null) update.setScheduleDate(newDate);
        if (bo.getStaffId() != null) update.setStaffId(newStaffId);
        if (bo.getRemark() != null) update.setRemark(bo.getRemark());
        scheduleMapper.updateById(update);
        // 写留痕日志
        Long operatorId = LoginHelper.getUserId();
        String operatorName = LoginHelper.getNickName();
        String changeType = shiftChanged ? CHANGE_TYPE_SHIFT : CHANGE_TYPE_TIME;
        String beforeTime = formatTimeRange(exist.getStartTime(), exist.getEndTime());
        String afterTime = formatTimeRange(newStartTime, newEndTime);
        ScheduleChangeLog logEntity = buildChangeLog(
            exist, changeType, beforeTime, afterTime,
            exist.getStatus(), exist.getStatus(), bo.getChangeReason(),
            operatorId, operatorName, new Date());
        changeLogMapper.insert(logEntity);
        return exist.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setShiftTime(String periodType, String periodKey, String shiftType,
                             LocalTime startTime, LocalTime endTime) {
        if (StrUtil.isBlank(periodType) || StrUtil.isBlank(periodKey)) {
            throw new ServiceException("周期类型/周期键不能为空");
        }
        if (!isValidShift(shiftType)) {
            throw new ServiceException("班次类型非法：" + shiftType);
        }
        if (startTime == null || endTime == null) {
            throw new ServiceException("班次起止时间不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new ServiceException("班次结束时间不能早于开始时间");
        }
        List<StaffSchedule> targets = scheduleMapper.selectList(
            Wrappers.<StaffSchedule>lambdaQuery()
                .eq(StaffSchedule::getPeriodType, periodType)
                .eq(StaffSchedule::getPeriodKey, periodKey)
                .eq(StaffSchedule::getShiftType, shiftType));
        if (CollUtil.isEmpty(targets)) {
            return 0;
        }
        int updated = 0;
        for (StaffSchedule target : targets) {
            StaffSchedule update = new StaffSchedule();
            update.setId(target.getId());
            update.setStartTime(startTime);
            update.setEndTime(endTime);
            scheduleMapper.updateById(update);
            updated++;
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id) {
        if (id == null) {
            throw new ServiceException("排班记录ID不能为空");
        }
        StaffSchedule exist = scheduleMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("排班记录不存在");
        }
        if (!STATUS_DRAFT.equals(exist.getStatus())) {
            throw new ServiceException("仅草稿排班可删除；已发布排班请通过修正调整");
        }
        scheduleMapper.deleteById(id);
        // 写留痕日志（DELETE）
        ScheduleChangeLog logEntity = buildChangeLog(
            exist, CHANGE_TYPE_DELETE,
            formatTimeRange(exist.getStartTime(), exist.getEndTime()), null,
            exist.getStatus(), null, "删除草稿",
            LoginHelper.getUserId(), LoginHelper.getNickName(), new Date());
        changeLogMapper.insert(logEntity);
    }

    @Override
    public List<ScheduleChangeLogVo> listChangeLogs(Long scheduleId, String periodType, String periodKey) {
        LambdaQueryWrapper<ScheduleChangeLog> wrapper = Wrappers.<ScheduleChangeLog>lambdaQuery();
        if (scheduleId != null) {
            wrapper.eq(ScheduleChangeLog::getScheduleId, scheduleId);
        } else {
            // 按周期查询：先查周期下所有排班 ID
            if (StrUtil.isNotBlank(periodType) && StrUtil.isNotBlank(periodKey)) {
                List<StaffSchedule> schedules = scheduleMapper.selectList(
                    Wrappers.<StaffSchedule>lambdaQuery()
                        .select(StaffSchedule::getId)
                        .eq(StaffSchedule::getPeriodType, periodType)
                        .eq(StaffSchedule::getPeriodKey, periodKey));
                if (CollUtil.isEmpty(schedules)) {
                    return Collections.emptyList();
                }
                List<Long> ids = schedules.stream().map(StaffSchedule::getId).collect(Collectors.toList());
                wrapper.in(ScheduleChangeLog::getScheduleId, ids);
            }
        }
        wrapper.orderByDesc(ScheduleChangeLog::getOperateTime);
        List<ScheduleChangeLog> list = changeLogMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 回填员工昵称
        Map<Long, SysUser> userMap = loadUserMap(
            list.stream().map(ScheduleChangeLog::getStaffId).distinct().collect(Collectors.toList()));
        return list.stream().map(entity -> {
            ScheduleChangeLogVo vo = new ScheduleChangeLogVo();
            BeanUtils.copyProperties(entity, vo);
            SysUser user = userMap.get(entity.getStaffId());
            if (user != null) {
                vo.setStaffName(user.getNickName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // 员工小程序
    // ============================================================

    @Override
    public List<StaffScheduleVo> mySchedules(String periodType, String periodKey) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return listSchedules(periodType, periodKey, STATUS_PUBLISHED,
            userId, null, null, true);
    }

    // ============================================================
    // 顾客小程序
    // ============================================================

    @Override
    public Map<String, List<StaffScheduleVo>> storeSchedules(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusDays(6); // 默认一周
        }
        List<StaffScheduleVo> list = listSchedules(null, null, STATUS_PUBLISHED,
            null, startDate, endDate, true);
        // 按日期分组
        Map<String, List<StaffScheduleVo>> grouped = new LinkedHashMap<>();
        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (StaffScheduleVo vo : list) {
            String key = vo.getScheduleDate().format(dateFmt);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(vo);
        }
        return grouped;
    }

    // ============================================================
    // M21 联动
    // ============================================================

    @Override
    public Set<Long> listScheduledStaffIds(LocalDate date) {
        if (date == null) {
            return Collections.emptySet();
        }
        List<StaffSchedule> list = scheduleMapper.selectList(
            Wrappers.<StaffSchedule>lambdaQuery()
                .select(StaffSchedule::getStaffId)
                .eq(StaffSchedule::getScheduleDate, date)
                .eq(StaffSchedule::getStatus, STATUS_PUBLISHED));
        if (CollUtil.isEmpty(list)) {
            return Collections.emptySet();
        }
        return list.stream().map(StaffSchedule::getStaffId).collect(Collectors.toSet());
    }

    @Override
    public Map<Long, List<int[]>> getStaffShiftTimes(LocalDate date) {
        if (date == null) {
            return Collections.emptyMap();
        }
        List<StaffSchedule> list = scheduleMapper.selectList(
            Wrappers.<StaffSchedule>lambdaQuery()
                .select(StaffSchedule::getStaffId, StaffSchedule::getStartTime, StaffSchedule::getEndTime)
                .eq(StaffSchedule::getScheduleDate, date)
                .eq(StaffSchedule::getStatus, STATUS_PUBLISHED));
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<Long, List<int[]>> result = new HashMap<>();
        for (StaffSchedule s : list) {
            if (s.getStaffId() == null || s.getStartTime() == null || s.getEndTime() == null) {
                continue;
            }
            int startMin = s.getStartTime().getHour() * 60 + s.getStartTime().getMinute();
            int endMin = s.getEndTime().getHour() * 60 + s.getEndTime().getMinute();
            result.computeIfAbsent(s.getStaffId(), k -> new ArrayList<>()).add(new int[]{startMin, endMin});
        }
        return result;
    }

    // ============================================================
    // 员工下拉选项
    // ============================================================

    @Override
    public List<Map<String, Object>> listStaffOptions() {
        // ★从 user_tenant_role 查询本租户 salesman 角色用户（而非 sys_user_role，
        //   避免 sys_user_role 中存在未通过人员管理分配的脏数据导致下拉多出员工）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptyList();
        }
        List<Long> staffIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> users = userMapper.selectBatchIds(staffIds);
        return users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .map(u -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("userId", u.getUserId());
                map.put("nickName", u.getNickName());
                map.put("avatar", u.getAvatar());
                return map;
            })
            .collect(Collectors.toList());
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    /**
     * 获取本租户 salesman 角色下的 user_id 集合（用于 batchCreate 校验）
     * ★从 user_tenant_role 查询（与 listStaffOptions 保持一致，避免 sys_user_role 脏数据）
     */
    private Set<Long> getSalesmanUserIds() {
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptySet();
        }
        return utrList.stream().map(UserTenantRole::getUserId).collect(Collectors.toSet());
    }

    /**
     * 批量加载用户信息（用于回填 staffName / staffAvatar）
     */
    private Map<Long, SysUser> loadUserMap(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
    }

    /**
     * 预加载批量创建时已存在的 UNIQUE 键（tenant_id 由 MP 插件自动填充）
     */
    private Set<String> loadExistingKeys(List<StaffScheduleBo.ScheduleItemBo> items) {
        // 收集所有涉及的员工 + 日期
        Set<Long> staffIds = items.stream()
            .map(StaffScheduleBo.ScheduleItemBo::getStaffId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<LocalDate> dates = items.stream()
            .map(StaffScheduleBo.ScheduleItemBo::getScheduleDate)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        if (staffIds.isEmpty() || dates.isEmpty()) {
            return new HashSet<>();
        }
        List<StaffSchedule> exists = scheduleMapper.selectList(
            Wrappers.<StaffSchedule>lambdaQuery()
                .in(StaffSchedule::getStaffId, staffIds)
                .in(StaffSchedule::getScheduleDate, dates));
        if (CollUtil.isEmpty(exists)) {
            return new HashSet<>();
        }
        return exists.stream()
            .map(e -> buildUniqueKey(e.getStaffId(), e.getScheduleDate(), e.getShiftType()))
            .collect(Collectors.toSet());
    }

    private String buildUniqueKey(Long staffId, LocalDate date, String shiftType) {
        return staffId + "|" + date + "|" + shiftType;
    }

    private boolean isValidShift(String shiftType) {
        return SHIFT_MORNING.equals(shiftType) || SHIFT_EVENING.equals(shiftType);
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return start.format(TIME_FMT) + "-" + end.format(TIME_FMT);
    }

    private ScheduleChangeLog buildChangeLog(StaffSchedule snapshot, String changeType,
                                              String beforeTime, String afterTime,
                                              String beforeStatus, String afterStatus,
                                              String reason, Long operatorId,
                                              String operatorName, Date operateTime) {
        ScheduleChangeLog logEntity = new ScheduleChangeLog();
        logEntity.setScheduleId(snapshot.getId());
        logEntity.setStaffId(snapshot.getStaffId());
        logEntity.setScheduleDate(snapshot.getScheduleDate());
        logEntity.setShiftType(snapshot.getShiftType());
        logEntity.setChangeType(changeType);
        logEntity.setBeforeTime(beforeTime);
        logEntity.setAfterTime(afterTime);
        logEntity.setBeforeStatus(beforeStatus);
        logEntity.setAfterStatus(afterStatus);
        logEntity.setChangeReason(reason);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setOperateTime(operateTime);
        return logEntity;
    }
}
