package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.StaffCommission;
import com.ruoyi.system.domain.bo.StaffCommissionBo;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.vo.StaffCommissionVo;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.StaffCommissionMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IMicroStaffScheduleService;
import com.ruoyi.system.service.IStaffCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 员工提成比例配置服务实现（REQ-03，v1.3 需求清单，仅 PC）
 * <p>staff_commission 为租户级表，多租户插件自动隔离。
 * <p>提成基数口径：员工当月已完成订单金额（booking_order.staff_user_id + order_status=COMPLETED
 * + complete_time 落在当月，汇总 amount），与 StaffSettlement 已完成订单口径一致。
 * <p>★fail-soft：任何聚合均做 null 保护，比例为空/0 或无订单时提成为 0，不抛异常。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffCommissionServiceImpl implements IStaffCommissionService {

    private final StaffCommissionMapper commissionMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    /** 员工下拉选项复用排班服务（同为 salesman/助教口径，服务层不受 FEAT_STAFF_SCHEDULE 开关限制） */
    private final IMicroStaffScheduleService staffScheduleService;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public TableDataInfo<StaffCommissionVo> queryPageList(StaffCommissionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StaffCommission> wrapper = buildQueryWrapper(bo);
        Page<StaffCommissionVo> page = commissionMapper.selectVoPage(pageQuery.build(), wrapper);
        fillExtraInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<StaffCommissionVo> queryList(StaffCommissionBo bo) {
        List<StaffCommissionVo> list = commissionMapper.selectVoList(buildQueryWrapper(bo));
        fillExtraInfo(list);
        return list;
    }

    @Override
    public StaffCommissionVo queryById(Long id) {
        StaffCommissionVo vo = commissionMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("员工提成配置不存在");
        }
        fillExtraInfo(CollUtil.newArrayList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(StaffCommissionBo bo) {
        validateRate(bo.getCommissionRate());
        // 每员工唯一：已存在启用/停用的配置均视为重复
        Long dup = commissionMapper.selectCount(Wrappers.<StaffCommission>lambdaQuery()
            .eq(StaffCommission::getStaffUserId, bo.getStaffUserId()));
        if (dup != null && dup > 0) {
            throw new ServiceException("该员工已存在提成配置，请直接修改");
        }
        StaffCommission entity = BeanCopyUtils.copy(bo, StaffCommission.class);
        entity.setId(null);
        entity.setStaffName(resolveStaffName(bo.getStaffUserId(), bo.getStaffName()));
        if (entity.getBaseSalary() == null) {
            entity.setBaseSalary(BigDecimal.ZERO);
        }
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        commissionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(StaffCommissionBo bo) {
        StaffCommission exist = commissionMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("员工提成配置不存在");
        }
        validateRate(bo.getCommissionRate());
        // 若变更了员工，需保证目标员工未被其它配置占用
        if (bo.getStaffUserId() != null && !bo.getStaffUserId().equals(exist.getStaffUserId())) {
            Long dup = commissionMapper.selectCount(Wrappers.<StaffCommission>lambdaQuery()
                .eq(StaffCommission::getStaffUserId, bo.getStaffUserId())
                .ne(StaffCommission::getId, bo.getId()));
            if (dup != null && dup > 0) {
                throw new ServiceException("该员工已存在提成配置，请勿重复配置");
            }
        }
        StaffCommission update = BeanCopyUtils.copy(bo, StaffCommission.class);
        update.setStaffName(resolveStaffName(bo.getStaffUserId(), bo.getStaffName()));
        if (update.getBaseSalary() == null) {
            update.setBaseSalary(BigDecimal.ZERO);
        }
        return commissionMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return commissionMapper.deleteBatchIds(ids);
    }

    @Override
    public BigDecimal calcMonthlyCommission(Long staffUserId, String month) {
        if (staffUserId == null) {
            return BigDecimal.ZERO;
        }
        StaffCommission cfg = commissionMapper.selectOne(Wrappers.<StaffCommission>lambdaQuery()
            .eq(StaffCommission::getStaffUserId, staffUserId)
            .last("LIMIT 1"));
        return commissionOf(cfg, sumCompletedAmount(staffUserId, month));
    }

    @Override
    public BigDecimal sumMonthlyLaborCost(String month) {
        List<StaffCommission> configs = commissionMapper.selectList(Wrappers.<StaffCommission>lambdaQuery()
            .eq(StaffCommission::getStatus, "0"));
        if (CollUtil.isEmpty(configs)) {
            return BigDecimal.ZERO;
        }
        // 一次性汇总当月各员工已完成订单金额，避免 N+1
        Map<Long, BigDecimal> baseMap = completedAmountByStaff(month);
        BigDecimal total = BigDecimal.ZERO;
        for (StaffCommission cfg : configs) {
            BigDecimal base = baseMap.getOrDefault(cfg.getStaffUserId(), BigDecimal.ZERO);
            BigDecimal commission = commissionOf(cfg, base);
            BigDecimal salary = cfg.getBaseSalary() != null ? cfg.getBaseSalary() : BigDecimal.ZERO;
            total = total.add(salary).add(commission);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<Map<String, Object>> listStaffOptions() {
        return staffScheduleService.listStaffOptions();
    }

    // ===== 私有方法 =====

    private LambdaQueryWrapper<StaffCommission> buildQueryWrapper(StaffCommissionBo bo) {
        return Wrappers.<StaffCommission>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getStaffName()), StaffCommission::getStaffName, bo.getStaffName())
            .eq(bo.getStaffUserId() != null, StaffCommission::getStaffUserId, bo.getStaffUserId())
            .eq(StrUtil.isNotBlank(bo.getStatus()), StaffCommission::getStatus, bo.getStatus())
            .orderByDesc(StaffCommission::getCreateTime)
            .orderByDesc(StaffCommission::getId);
    }

    /** 回填部门名 + 当月基数/提成/人力成本（实时计算） */
    private void fillExtraInfo(List<StaffCommissionVo> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        String month = LocalDate.now().format(MONTH_FMT);
        Map<Long, BigDecimal> baseMap = completedAmountByStaff(month);
        Map<Long, String> deptNameMap = loadDeptNameMap(records);
        for (StaffCommissionVo vo : records) {
            BigDecimal base = baseMap.getOrDefault(vo.getStaffUserId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal rate = vo.getCommissionRate() != null ? vo.getCommissionRate() : BigDecimal.ZERO;
            BigDecimal commission = base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal salary = vo.getBaseSalary() != null ? vo.getBaseSalary() : BigDecimal.ZERO;
            vo.setMonthlyCompletedAmount(base);
            vo.setMonthlyCommission(commission);
            vo.setMonthlyLaborCost(salary.add(commission).setScale(2, RoundingMode.HALF_UP));
            vo.setDeptName(deptNameMap.get(vo.getStaffUserId()));
        }
    }

    /** 批量解析员工所属部门名（staffUserId → deptName） */
    private Map<Long, String> loadDeptNameMap(List<StaffCommissionVo> records) {
        Set<Long> userIds = records.stream()
            .map(StaffCommissionVo::getStaffUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, String> result = new HashMap<>();
        if (CollUtil.isEmpty(userIds)) {
            return result;
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, userIds));
        if (CollUtil.isEmpty(users)) {
            return result;
        }
        Set<Long> deptIds = users.stream()
            .map(SysUser::getDeptId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, String> deptMap = new HashMap<>();
        if (CollUtil.isNotEmpty(deptIds)) {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            if (CollUtil.isNotEmpty(depts)) {
                for (SysDept d : depts) {
                    deptMap.put(d.getDeptId(), d.getDeptName());
                }
            }
        }
        for (SysUser u : users) {
            if (u.getDeptId() != null) {
                result.put(u.getUserId(), deptMap.get(u.getDeptId()));
            }
        }
        return result;
    }

    /** 汇总某员工某月已完成订单金额（提成基数） */
    private BigDecimal sumCompletedAmount(Long staffUserId, String month) {
        if (staffUserId == null) {
            return BigDecimal.ZERO;
        }
        Date[] range = monthRange(month);
        List<BookingOrder> orders = bookingOrderMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getStaffUserId, staffUserId)
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.COMPLETED.getCode())
            .ge(BookingOrder::getCompleteTime, range[0])
            .le(BookingOrder::getCompleteTime, range[1]));
        BigDecimal total = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(orders)) {
            for (BookingOrder o : orders) {
                if (o.getAmount() != null) {
                    total = total.add(o.getAmount());
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** 一次性汇总某月各员工已完成订单金额（staffUserId → amount），避免 N+1 */
    private Map<Long, BigDecimal> completedAmountByStaff(String month) {
        Map<Long, BigDecimal> map = new HashMap<>();
        Date[] range = monthRange(month);
        List<BookingOrder> orders = bookingOrderMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.COMPLETED.getCode())
            .ge(BookingOrder::getCompleteTime, range[0])
            .le(BookingOrder::getCompleteTime, range[1])
            .isNotNull(BookingOrder::getStaffUserId));
        if (CollUtil.isEmpty(orders)) {
            return map;
        }
        for (BookingOrder o : orders) {
            if (o.getStaffUserId() == null) {
                continue;
            }
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            map.merge(o.getStaffUserId(), amt, BigDecimal::add);
        }
        return map;
    }

    /** 提成 = 基数 × 比例（比例为空/0 返回 0；2 位 HALF_UP） */
    private BigDecimal commissionOf(StaffCommission cfg, BigDecimal base) {
        if (cfg == null || cfg.getCommissionRate() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal b = base != null ? base : BigDecimal.ZERO;
        return b.multiply(cfg.getCommissionRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /** 解析员工姓名快照（优先传入值，其次查 sys_user.nick_name） */
    private String resolveStaffName(Long staffUserId, String given) {
        if (StrUtil.isNotBlank(given)) {
            return given;
        }
        if (staffUserId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(staffUserId);
        return user != null ? user.getNickName() : null;
    }

    /** 提成比例值域校验（0~1，服务端最终防线） */
    private void validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new ServiceException("提成比例不能为空");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new ServiceException("提成比例必须在 0~1 之间（如 0.1 表示 10%）");
        }
    }

    /** 月份（yyyy-MM）→ [月初 00:00:00, 月末 23:59:59]；非法则回退当月 */
    private Date[] monthRange(String month) {
        YearMonth ym;
        try {
            ym = StrUtil.isNotBlank(month) ? YearMonth.parse(month, MONTH_FMT) : YearMonth.now();
        } catch (Exception e) {
            log.warn("[StaffCommission] 月份格式非法，按当月处理: month={}", month);
            ym = YearMonth.now();
        }
        Date start = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        return new Date[]{start, end};
    }
}
