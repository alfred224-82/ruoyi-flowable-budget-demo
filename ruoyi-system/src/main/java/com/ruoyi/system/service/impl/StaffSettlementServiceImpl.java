package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.StaffSettlement;
import com.ruoyi.system.domain.bo.StaffSettlementBo;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.enums.SettlePayStatus;
import com.ruoyi.system.domain.vo.SettlementOrderDetailVo;
import com.ruoyi.system.domain.vo.StaffSettlementVo;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.StaffSettlementMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ICashFlowService;
import com.ruoyi.system.service.IStaffSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 人员结算服务实现（T15.5/T15.13，老板 PC，★V1 只记账不打款）
 * <p>staff_settlement 租户级表，多租户插件自动隔离。
 * <p>应发金额 = 该员工周期内【已完成】(COMPLETED) 预约订单金额汇总（complete_time 落在周期内）；
 * ★结算规则表 settle_rule（micro:settleRule，按单/按时/提成比例）未在 M15_05 交付，故按订单金额汇总，
 * 待规则表交付后再扩展提成口径（不臆造 schema）。
 * <p>★零支付合规闭环：{@link #markPaidOffline} 仅改状态 + marked_by/marked_time 并写 cash_flow 支出流水，
 * 【不调任何支付/转账/代付接口】。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffSettlementServiceImpl implements IStaffSettlementService {

    private final StaffSettlementMapper settlementMapper;
    private final BookingOrderMapper bookingMapper;
    private final SysUserMapper userMapper;
    private final ICashFlowService cashFlowService;

    @Override
    public TableDataInfo<StaffSettlementVo> queryPageList(StaffSettlementBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StaffSettlement> wrapper = Wrappers.<StaffSettlement>lambdaQuery()
            .eq(bo.getStaffUserId() != null, StaffSettlement::getStaffUserId, bo.getStaffUserId())
            .eq(StrUtil.isNotBlank(bo.getPayStatus()), StaffSettlement::getPayStatus, bo.getPayStatus())
            .orderByDesc(StaffSettlement::getPeriodEnd)
            .orderByDesc(StaffSettlement::getId);
        Page<StaffSettlementVo> page = settlementMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public StaffSettlementVo queryById(Long id) {
        StaffSettlementVo vo = settlementMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("结算单不存在");
        }
        return vo;
    }

    @Override
    public StaffSettlementVo preview(Long staffUserId, Date periodStart, Date periodEnd) {
        return summarize(staffUserId, periodStart, periodEnd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generate(StaffSettlementBo bo) {
        StaffSettlementVo sum = summarize(bo.getStaffUserId(), bo.getPeriodStart(), bo.getPeriodEnd());
        // 防重复结算：同员工同周期已存在结算单则拒绝（避免重复发放）
        Long dup = settlementMapper.selectCount(Wrappers.<StaffSettlement>lambdaQuery()
            .eq(StaffSettlement::getStaffUserId, bo.getStaffUserId())
            .eq(StaffSettlement::getPeriodStart, bo.getPeriodStart())
            .eq(StaffSettlement::getPeriodEnd, bo.getPeriodEnd()));
        if (dup != null && dup > 0) {
            throw new ServiceException("该员工此结算周期已生成结算单，请勿重复生成");
        }
        StaffSettlement entity = new StaffSettlement();
        entity.setSettleNo(genSettleNo());
        entity.setStaffUserId(sum.getStaffUserId());
        entity.setStaffName(sum.getStaffName());
        entity.setPeriodStart(bo.getPeriodStart());
        entity.setPeriodEnd(bo.getPeriodEnd());
        entity.setTotalOrders(sum.getTotalOrders());
        entity.setTotalAmount(sum.getTotalAmount());
        entity.setPayStatus(SettlePayStatus.PENDING.getCode());
        entity.setRemark(bo.getRemark());
        settlementMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markPaidOffline(Long id) {
        if (id == null) {
            throw new ServiceException("结算单ID不能为空");
        }
        StaffSettlement exist = settlementMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("结算单不存在");
        }
        // 幂等保护：避免重复标记产生重复支出流水
        if (SettlePayStatus.PAID_OFFLINE.getCode().equals(exist.getPayStatus())) {
            throw new ServiceException("结算单已标记发放，请勿重复操作");
        }
        Date now = new Date();
        StaffSettlement update = new StaffSettlement();
        update.setId(id);
        update.setPayStatus(SettlePayStatus.PAID_OFFLINE.getCode());
        update.setMarkedBy(safeUsername());
        update.setMarkedById(LoginHelper.getUserId());
        update.setMarkedTime(now);
        int rows = settlementMapper.updateById(update);
        // ★零支付：仅记账，写 cash_flow 支出流水（线下转账后标记，不调任何支付接口）
        cashFlowService.record(CashFlowType.EXPENSE.getCode(), exist.getTotalAmount(), "工资结算",
            id, CashFlowType.RELATED_SETTLEMENT, now, "人员结算发放：" + exist.getSettleNo());
        return rows;
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        // 仅允许删除「待发放」状态的结算单，已发放的不允许删除
        List<StaffSettlement> settlements = settlementMapper.selectBatchIds(ids);
        for (StaffSettlement s : settlements) {
            if (SettlePayStatus.PAID_OFFLINE.getCode().equals(s.getPayStatus())) {
                throw new ServiceException("结算单「" + s.getSettleNo() + "」已线下发放，不允许删除");
            }
        }
        return settlementMapper.deleteBatchIds(ids);
    }

    @Override
    public List<SettlementOrderDetailVo> listSettlementOrders(Long settlementId) {
        if (settlementId == null) {
            throw new ServiceException("结算单ID不能为空");
        }
        StaffSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new ServiceException("结算单不存在");
        }
        // 查询该结算单对应的已完成订单（同员工 + 同周期）
        List<BookingOrder> orders = bookingMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getStaffUserId, settlement.getStaffUserId())
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.COMPLETED.getCode())
            .ge(BookingOrder::getCompleteTime, DateUtil.beginOfDay(settlement.getPeriodStart()))
            .le(BookingOrder::getCompleteTime, DateUtil.endOfDay(settlement.getPeriodEnd()))
            .orderByAsc(BookingOrder::getBookingTime));
        if (CollUtil.isEmpty(orders)) {
            return new ArrayList<>();
        }
        // 批量查询顾客昵称
        Set<Long> customerIds = orders.stream()
            .map(BookingOrder::getCustomerUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(customerIds)) {
            List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getUserId, customerIds));
            customerNameMap = users.stream()
                .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
        }
        // 组装明细
        List<SettlementOrderDetailVo> result = new ArrayList<>();
        for (BookingOrder o : orders) {
            SettlementOrderDetailVo vo = new SettlementOrderDetailVo();
            vo.setOrderId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setCustomerName(customerNameMap.get(o.getCustomerUserId()));
            vo.setCustomerPhone(o.getCustomerPhone());
            vo.setServiceName(o.getServiceName());
            vo.setHours(o.getQuantity());
            vo.setStaffFee(extractStaffFee(o.getRemark()));
            vo.setStartTime(o.getBookingTime());
            // 结束时间 = bookingTime + hours 小时
            if (o.getBookingTime() != null && o.getQuantity() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(o.getBookingTime());
                cal.add(Calendar.HOUR_OF_DAY, o.getQuantity().intValue());
                vo.setEndTime(cal.getTime());
            }
            vo.setCompleteTime(o.getCompleteTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 汇总员工周期内已完成订单：单量 + 应发金额（不落库）
     * <p>★应发金额 = 订单 remark.costDetails 中 type='助教' 的费用小计之和（不含房间/项目费用）。
     * 若历史订单 remark 中无 costDetails（兼容旧数据），则回退使用订单总额。
     */
    private StaffSettlementVo summarize(Long staffUserId, Date periodStart, Date periodEnd) {
        if (staffUserId == null || periodStart == null || periodEnd == null) {
            throw new ServiceException("员工与结算周期不能为空");
        }
        if (periodStart.after(periodEnd)) {
            throw new ServiceException("结算周期起不能晚于周期止");
        }
        List<BookingOrder> orders = bookingMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getStaffUserId, staffUserId)
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.COMPLETED.getCode())
            .ge(BookingOrder::getCompleteTime, DateUtil.beginOfDay(periodStart))
            .le(BookingOrder::getCompleteTime, DateUtil.endOfDay(periodEnd)));
        BigDecimal total = BigDecimal.ZERO;
        for (BookingOrder o : orders) {
            BigDecimal staffFee = extractStaffFee(o.getRemark());
            if (staffFee == null) {
                // 兼容旧数据：remark 中无 costDetails 时回退使用订单总额
                staffFee = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            }
            total = total.add(staffFee);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        StaffSettlementVo vo = new StaffSettlementVo();
        vo.setStaffUserId(staffUserId);
        vo.setPeriodStart(periodStart);
        vo.setPeriodEnd(periodEnd);
        vo.setTotalOrders(orders.size());
        vo.setTotalAmount(total);
        SysUser user = userMapper.selectById(staffUserId);
        vo.setStaffName(user != null ? user.getNickName() : null);
        return vo;
    }

    /**
     * 从订单 remark JSON 的 costDetails 中提取 type='助教' 的费用小计之和。
     * <p>costDetails 结构：[{"name":"xxx","type":"助教","unitPrice":50,"hours":2,"subtotal":100}, ...]
     *
     * @return 助教费用合计；remark 为空或无 costDetails 时返回 null（由调用方回退）
     */
    private BigDecimal extractStaffFee(String remark) {
        if (StrUtil.isBlank(remark)) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(remark);
            JSONArray details = obj.getJSONArray("costDetails");
            if (details == null || details.isEmpty()) {
                return null;
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < details.size(); i++) {
                JSONObject d = details.getJSONObject(i);
                if ("助教".equals(d.getStr("type"))) {
                    BigDecimal subtotal = d.getBigDecimal("subtotal");
                    if (subtotal != null) {
                        sum = sum.add(subtotal);
                    }
                }
            }
            return sum;
        } catch (Exception e) {
            log.warn("解析订单remark.costDetails失败，remark={}", remark, e);
            return null;
        }
    }

    /**
     * 生成结算单号：ST + yyyyMMddHHmmss + 4 位随机
     */
    private String genSettleNo() {
        return "ST" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }

    private String safeUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
