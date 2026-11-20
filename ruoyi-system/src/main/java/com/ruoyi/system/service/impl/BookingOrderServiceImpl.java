package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.ActivitySeatAssignment;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.ProjectSignup;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.BookingOrderBo;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.enums.BookingPayStatus;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.vo.BookingOrderVo;
import com.ruoyi.system.domain.vo.MicroRecallCustomerVo;
import com.ruoyi.system.mapper.ActivitySeatAssignmentMapper;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.ProjectSignupMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IBookingOrderService;
import com.ruoyi.system.service.ICashFlowService;
import com.ruoyi.system.service.ICustomerCreditService;
import com.ruoyi.system.service.IMicroSelfBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约订单服务实现（T15.5/T15.12，小微版）
 * <p>booking_order 租户级表，多租户插件自动隔离。
 * <p>★V1 零支付合规闭环：顾客提交预约生成「待到店付」(PENDING_PAY) 订单 → 老板线下收款后 {@link #markReceived}
 * 改「已收款」(RECEIVED) + pay_time，并调 {@link ICashFlowService#record} 写 cash_flow 收入流水；
 * 【全程不调任何支付接口】，顾客端无 micro:customer:pay 权限。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOrderServiceImpl implements IBookingOrderService {

    private final BookingOrderMapper bookingMapper;
    private final PromotionActivityMapper activityMapper;
    private final ServiceItemMapper itemMapper;
    private final SysUserMapper userMapper;
    /** ★活动订单顾客来源：画布加人（activity_seat_assignment，CUSTOMER_PICK 类型） */
    private final ActivitySeatAssignmentMapper seatAssignmentMapper;
    /** ★活动订单顾客来源②：项目报名（project_signup，selected_project_id 非空时兜底） */
    private final ProjectSignupMapper signupMapper;
    private final ICashFlowService cashFlowService;
    /** T15.32 防爽约黑名单：顾客提交预约前校验 is_blacklisted（仅 micro_pro 有信誉记录，lite 恒放行） */
    private final ICustomerCreditService customerCreditService;
    /** ★T21.5 取消预约时释放关联助教/房间资源 */
    private final IMicroSelfBookingService microSelfBookingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** customerTagJob 默认未到店天数阈值（7 天未到店→待召回） */
    private static final int DEFAULT_RECALL_INACTIVE_DAYS = 7;

    @Override
    public TableDataInfo<BookingOrderVo> queryPageList(BookingOrderBo bo, PageQuery pageQuery) {
        // 1. 顾客自主预约（booking_order）
        LambdaQueryWrapper<BookingOrder> wrapper = Wrappers.<BookingOrder>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getOrderNo()), BookingOrder::getOrderNo, bo.getOrderNo())
            .like(StrUtil.isNotBlank(bo.getCustomerPhone()), BookingOrder::getCustomerPhone, bo.getCustomerPhone())
            .eq(bo.getStaffUserId() != null, BookingOrder::getStaffUserId, bo.getStaffUserId())
            .eq(bo.getServiceItemId() != null, BookingOrder::getServiceItemId, bo.getServiceItemId())
            .eq(StrUtil.isNotBlank(bo.getPayStatus()), BookingOrder::getPayStatus, bo.getPayStatus())
            .eq(StrUtil.isNotBlank(bo.getOrderStatus()), BookingOrder::getOrderStatus, bo.getOrderStatus())
            .orderByDesc(BookingOrder::getBookingTime)
            .orderByDesc(BookingOrder::getId);
        List<BookingOrderVo> bookings = bookingMapper.selectVoList(wrapper);
        bookings.forEach(vo -> vo.setSourceType("BOOKING"));
        fillUserNames(bookings);

        // 2. boss 后台创建的活动（promotion_activity）映射为预约视图（活动=预约，仅创建者不同）
        List<BookingOrderVo> activities = listActivitiesAsBookings(bo);

        // 3. 合并 + 按预约时间倒序 + 内存分页（小微版数据量小，内存合并可接受）
        List<BookingOrderVo> merged = new ArrayList<>(bookings.size() + activities.size());
        merged.addAll(bookings);
        merged.addAll(activities);
        merged.sort(Comparator.comparing(BookingOrderVo::getBookingTime,
            Comparator.nullsLast(Comparator.reverseOrder())));
        long total = merged.size();
        int from = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        int to = Math.min(from + pageQuery.getPageSize(), merged.size());
        List<BookingOrderVo> pageList = from < merged.size()
            ? new ArrayList<>(merged.subList(from, to)) : new ArrayList<>();
        TableDataInfo<BookingOrderVo> rsp = TableDataInfo.build();
        rsp.setRows(pageList);
        rsp.setTotal(total);
        return rsp;
    }

    /**
     * 将 boss 后台创建的活动（promotion_activity）映射为预约订单视图。
     * <p>状态映射：endTime 已过或 status=ENDED → COMPLETED（已完成）；
     * status=PUBLISHED 且 endTime 未过 → PENDING_SERVICE（待服务）；status=DRAFT 不展示。
     * <p>★收款状态统一两态：活动行恒为 PENDING_PAY（未收款），payStatus 筛选正常响应（按 VO 值匹配）；
     * orderStatus 仍不响应 CANCELLED 筛选（活动无取消概念）。
     */
    private List<BookingOrderVo> listActivitiesAsBookings(BookingOrderBo bo) {
        List<PromotionActivity> acts = activityMapper.selectList(Wrappers.<PromotionActivity>lambdaQuery()
            .ne(PromotionActivity::getStatus, "DRAFT")
            .orderByDesc(PromotionActivity::getEndTime));
        List<BookingOrderVo> result = new ArrayList<>();
        for (PromotionActivity a : acts) {
            BookingOrderVo vo = mapActivityToVo(a);
            // ★收款状态筛选：活动行恒为未收款，筛选值与 VO 匹配时才纳入
            if (StrUtil.isNotBlank(bo.getPayStatus()) && !bo.getPayStatus().equals(vo.getPayStatus())) {
                continue;
            }
            // 按 orderStatus 过滤（活动不响应 CANCELLED）
            if (StrUtil.isNotBlank(bo.getOrderStatus()) && !bo.getOrderStatus().equals(vo.getOrderStatus())) {
                continue;
            }
            result.add(vo);
        }
        // ★回填活动对应的员工昵称
        fillUserNames(result);
        return result;
    }

    @Override
    public BookingOrderVo queryById(Long id) {
        // 1. 先查 booking_order 表
        BookingOrderVo vo = bookingMapper.selectVoById(id);
        if (vo != null) {
            vo.setSourceType("BOOKING");
            fillUserNames(CollUtil.newArrayList(vo));
            return vo;
        }
        // 2. 回落查 promotion_activity 表（列表合并了两张表，详情也需保持一致）
        PromotionActivity a = activityMapper.selectById(id);
        if (a != null) {
            vo = mapActivityToVo(a);
            fillUserNames(CollUtil.newArrayList(vo));
            return vo;
        }
        throw new ServiceException("预约订单不存在");
    }

    /**
     * 将活动映射为预约订单 VO（与 listActivitiesAsBookings 映射规则一致）
     */
    private BookingOrderVo mapActivityToVo(PromotionActivity a) {
        Date now = new Date();
        boolean ended = "ENDED".equals(a.getStatus())
            || (a.getEndTime() != null && a.getEndTime().before(now));
        BookingOrderVo vo = new BookingOrderVo();
        vo.setId(a.getId());
        vo.setSourceType("ACTIVITY");
        vo.setOrderNo("ACT" + a.getId());
        vo.setServiceName(a.getTitle());
        vo.setBookingTime(a.getStartTime());
        vo.setCompleteTime(a.getEndTime());
        vo.setOrderStatus(ended ? BookingOrderStatus.COMPLETED.getCode() : BookingOrderStatus.PENDING_SERVICE.getCode());
        // ★收款状态统一两态：活动行恒为「未收款」（到店支付、无在线支付态），避免列表/详情该列空渲染
        vo.setPayStatus(BookingPayStatus.PENDING_PAY.getCode());
        vo.setCreateTime(a.getCreateTime());
        vo.setRemark(a.getContent());
        // ★M22 费用回填：金额=Boss自定义总价优先、否则系统自动计算总价；数量=设定参与人数
        // （避免预约列表/详情合并活动行时金额列渲染为裸「￥」）
        vo.setAmount(a.getCustomPrice() != null ? a.getCustomPrice() : a.getAutoCalculatedPrice());
        if (a.getParticipantCount() != null) {
            vo.setQuantity(BigDecimal.valueOf(a.getParticipantCount()));
        }
        if (StrUtil.isNotBlank(a.getSelectedStaffIds())) {
            try {
                List<Long> staffIds = objectMapper.readValue(a.getSelectedStaffIds(), new TypeReference<List<Long>>() {});
                if (CollUtil.isNotEmpty(staffIds)) {
                    vo.setStaffUserId(staffIds.get(0));
                }
            } catch (Exception e) {
                log.warn("解析活动 selectedStaffIds 失败, activityId={}, selectedStaffIds={}", a.getId(), a.getSelectedStaffIds(), e);
            }
        }
        // ★活动订单回填顾客信息（不依赖 selected_project_id，画布加人为主要来源）
        // 来源①：画布加人 CUSTOMER_PICK（Boss 在座位画布上添加的顾客）
        Long customerUserId = null;
        List<ActivitySeatAssignment> customerSeats = seatAssignmentMapper.selectList(Wrappers.<ActivitySeatAssignment>lambdaQuery()
            .eq(ActivitySeatAssignment::getActivityId, a.getId())
            .eq(ActivitySeatAssignment::getAssignmentType, ActivitySeatAssignment.TYPE_CUSTOMER_PICK)
            .isNotNull(ActivitySeatAssignment::getUserId)
            .orderByAsc(ActivitySeatAssignment::getId)
            .last("LIMIT 1"));
        if (CollUtil.isNotEmpty(customerSeats)) {
            customerUserId = customerSeats.get(0).getUserId();
        }
        // 来源②：项目报名 project_signup（selected_project_id 非空时的兜底）
        if (customerUserId == null && a.getSelectedProjectId() != null) {
            List<ProjectSignup> signups = signupMapper.selectList(Wrappers.<ProjectSignup>lambdaQuery()
                .eq(ProjectSignup::getProjectId, a.getSelectedProjectId())
                .eq(ProjectSignup::getIsWaitlist, 0)
                .orderByAsc(ProjectSignup::getSignupTime)
                .last("LIMIT 1"));
            if (CollUtil.isNotEmpty(signups) && signups.get(0).getUserId() != null) {
                customerUserId = signups.get(0).getUserId();
            }
        }
        if (customerUserId != null) {
            vo.setCustomerUserId(customerUserId);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(BookingOrderBo bo) {
        BookingOrder order = buildOrder(bo);
        bookingMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitByCustomer(BookingOrderBo bo) {
        Long userId = LoginHelper.getUserId();
        bo.setCustomerUserId(userId);
        // ★T15.32 黑名单双重限制之一：已拉黑顾客禁止新预约（跨租户隔离，唯一出口为老板手动移出）
        if (customerCreditService.isBlacklisted(userId)) {
            throw new ServiceException("您已被商家限制预约，请联系商家");
        }
        // 手机号缺省时取登录用户手机号（游客预约须显式传手机号）
        if (StrUtil.isBlank(bo.getCustomerPhone()) && userId != null) {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                bo.setCustomerPhone(user.getPhonenumber());
            }
        }
        if (StrUtil.isBlank(bo.getCustomerPhone())) {
            throw new ServiceException("顾客手机号不能为空");
        }
        BookingOrder order = buildOrder(bo);
        bookingMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BookingOrderBo bo) {
        BookingOrder exist = bookingMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("预约订单不存在");
        }
        BookingOrder update = new BookingOrder();
        update.setId(bo.getId());
        update.setCustomerPhone(bo.getCustomerPhone());
        update.setStaffUserId(bo.getStaffUserId());
        update.setBookingTime(bo.getBookingTime());
        update.setQuantity(bo.getQuantity());
        update.setRemark(bo.getRemark());
        // 服务项目变更 → 重新快照名称
        Long itemId = bo.getServiceItemId();
        ServiceItem item = null;
        if (itemId != null && !itemId.equals(exist.getServiceItemId())) {
            item = itemMapper.selectById(itemId);
            if (item == null) {
                throw new ServiceException("服务项目不存在");
            }
            update.setServiceItemId(item.getId());
            update.setServiceName(item.getName());
        }
        // 金额：显式提交优先；否则当项目/数量变化时按单价×数量重算
        if (bo.getAmount() != null) {
            update.setAmount(bo.getAmount());
        } else if (item != null || bo.getQuantity() != null) {
            if (item == null) {
                item = itemMapper.selectById(exist.getServiceItemId());
            }
            BigDecimal price = (item != null && item.getPrice() != null) ? item.getPrice() : BigDecimal.ZERO;
            BigDecimal qty = bo.getQuantity() != null ? bo.getQuantity()
                : (exist.getQuantity() != null ? exist.getQuantity() : BigDecimal.ONE);
            update.setAmount(price.multiply(qty).setScale(2, RoundingMode.HALF_UP));
        }
        return bookingMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancel(Long id) {
        BookingOrder exist = requireOrder(id);
        if (BookingOrderStatus.COMPLETED.getCode().equals(exist.getOrderStatus())) {
            throw new ServiceException("已完成订单不可取消");
        }
        BookingOrder update = new BookingOrder();
        update.setId(id);
        update.setOrderStatus(BookingOrderStatus.CANCELLED.getCode());
        int rows = bookingMapper.updateById(update);
        // ★T21.5 取消预约时释放关联的助教/房间资源（reserved → idle + 写 resource_status_log）
        try {
            microSelfBookingService.releaseResources(id);
        } catch (Exception e) {
            log.warn("取消预约释放资源失败，bookingId={}", id, e);
            // 不阻断取消流程，资源释放失败仅记录警告
        }
        return rows;
    }

    @Override
    public int complete(Long id) {
        BookingOrder exist = requireOrder(id);
        if (BookingOrderStatus.CANCELLED.getCode().equals(exist.getOrderStatus())) {
            throw new ServiceException("已取消订单不可完成");
        }
        BookingOrder update = new BookingOrder();
        update.setId(id);
        update.setOrderStatus(BookingOrderStatus.COMPLETED.getCode());
        update.setCompleteTime(new Date());
        return bookingMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markReceived(Long id) {
        BookingOrder exist = requireOrder(id);
        // 幂等保护：避免重复标记产生重复收入流水
        if (BookingPayStatus.RECEIVED.getCode().equals(exist.getPayStatus())) {
            throw new ServiceException("订单已收款，请勿重复标记");
        }
        Date now = new Date();
        BookingOrder update = new BookingOrder();
        update.setId(id);
        update.setPayStatus(BookingPayStatus.RECEIVED.getCode());
        update.setPayTime(now);
        int rows = bookingMapper.updateById(update);
        // ★零支付：仅记账，写 cash_flow 收入流水（不调任何支付接口）
        cashFlowService.record(CashFlowType.INCOME.getCode(), exist.getAmount(), "订单收入",
            id, CashFlowType.RELATED_BOOKING, now, "预约订单收款：" + exist.getOrderNo());
        return rows;
    }

    @Override
    public List<BookingOrderVo> myBookings() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return new ArrayList<>();
        }
        List<BookingOrderVo> list = bookingMapper.selectVoList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getCustomerUserId, userId)
            // ★已取消的预约不再展示在顾客「我的活动」列表
            .ne(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode())
            .orderByDesc(BookingOrder::getBookingTime)
            .orderByDesc(BookingOrder::getId));
        fillUserNames(list);
        return list;
    }

    @Override
    public int expireOverdueBookings() {
        // ★零支付合规：仅关单不涉退款。待服务 + 预约到店时间已过 → 已取消（租户上下文由 @TenantJob 切面设置，插件自动隔离）
        Date now = new Date();
        return bookingMapper.update(null, Wrappers.<BookingOrder>lambdaUpdate()
            .set(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode())
            .set(BookingOrder::getUpdateTime, now)
            .set(BookingOrder::getUpdateBy, "bookingExpireJob")
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.PENDING_SERVICE.getCode())
            .isNotNull(BookingOrder::getBookingTime)
            .lt(BookingOrder::getBookingTime, now));
    }

    @Override
    public List<MicroRecallCustomerVo> listRecallCustomers(int inactiveDays) {
        int days = inactiveDays > 0 ? inactiveDays : DEFAULT_RECALL_INACTIVE_DAYS;
        Date now = new Date();
        Date threshold = DateUtil.offsetDay(now, -days);
        // 到店口径：非取消订单且有完成时间或收款时间（租户上下文由 @TenantJob 切面设置，插件自动隔离）
        List<BookingOrder> visited = bookingMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .ne(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode())
            .and(w -> w.isNotNull(BookingOrder::getCompleteTime).or().isNotNull(BookingOrder::getPayTime)));
        if (CollUtil.isEmpty(visited)) {
            return new ArrayList<>();
        }
        // 按顾客聚合最近到店时间（优先 customerUserId，回落 customerPhone）
        Map<String, MicroRecallCustomerVo> latest = new LinkedHashMap<>();
        for (BookingOrder o : visited) {
            Date visit = o.getCompleteTime() != null ? o.getCompleteTime() : o.getPayTime();
            if (visit == null) {
                continue;
            }
            String key = o.getCustomerUserId() != null ? "u:" + o.getCustomerUserId()
                : (StrUtil.isNotBlank(o.getCustomerPhone()) ? "p:" + o.getCustomerPhone() : null);
            if (key == null) {
                continue;
            }
            MicroRecallCustomerVo vo = latest.get(key);
            if (vo == null) {
                vo = new MicroRecallCustomerVo();
                vo.setCustomerUserId(o.getCustomerUserId());
                vo.setCustomerPhone(o.getCustomerPhone());
                vo.setLastVisitTime(visit);
                latest.put(key, vo);
            } else {
                if (visit.after(vo.getLastVisitTime())) {
                    vo.setLastVisitTime(visit);
                }
                if (vo.getCustomerUserId() == null) {
                    vo.setCustomerUserId(o.getCustomerUserId());
                }
                if (StrUtil.isBlank(vo.getCustomerPhone())) {
                    vo.setCustomerPhone(o.getCustomerPhone());
                }
            }
        }
        // 过滤：最近到店早于阈值（即 ≥ days 天未到店）的顾客
        List<MicroRecallCustomerVo> recall = new ArrayList<>();
        for (MicroRecallCustomerVo vo : latest.values()) {
            if (vo.getLastVisitTime().before(threshold)) {
                vo.setInactiveDays(DateUtil.betweenDay(vo.getLastVisitTime(), now, true));
                recall.add(vo);
            }
        }
        fillRecallCustomerNames(recall);
        return recall;
    }

    @Override
    public List<BookingOrderVo> mySchedule() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) {
            return new ArrayList<>();
        }
        // 本人被指派、待服务、预约时间自今日起（今日及以后排班）
        List<BookingOrderVo> list = bookingMapper.selectVoList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getStaffUserId, uid)
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.PENDING_SERVICE.getCode())
            .ge(BookingOrder::getBookingTime, DateUtil.beginOfDay(new Date()))
            .orderByAsc(BookingOrder::getBookingTime)
            .orderByDesc(BookingOrder::getId));
        fillUserNames(list);
        return list;
    }

    @Override
    public List<BookingOrderVo> myCompletedOrders() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) {
            return new ArrayList<>();
        }
        // 本人被指派且已完成的订单（每单收入明细）
        List<BookingOrderVo> list = bookingMapper.selectVoList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getStaffUserId, uid)
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.COMPLETED.getCode())
            .orderByDesc(BookingOrder::getCompleteTime)
            .orderByDesc(BookingOrder::getId));
        fillUserNames(list);
        return list;
    }

    @Override
    public int completeByStaff(Long id) {
        BookingOrder exist = requireOrder(id);
        Long uid = LoginHelper.getUserId();
        // 归属校验：业务员只能确认本人被指派的订单
        if (exist.getStaffUserId() == null || !exist.getStaffUserId().equals(uid)) {
            throw new ServiceException("只能确认本人被指派的预约");
        }
        if (BookingOrderStatus.CANCELLED.getCode().equals(exist.getOrderStatus())) {
            throw new ServiceException("已取消订单不可完成");
        }
        // 幂等：已完成直接返回 0
        if (BookingOrderStatus.COMPLETED.getCode().equals(exist.getOrderStatus())) {
            return 0;
        }
        BookingOrder update = new BookingOrder();
        update.setId(id);
        update.setOrderStatus(BookingOrderStatus.COMPLETED.getCode());
        update.setCompleteTime(new Date());
        return bookingMapper.updateById(update);
    }

    /**
     * 构建预约订单（快照服务项目名称，amount 缺省按单价×数量计算，初始「待到店付/待服务」）
     */
    private BookingOrder buildOrder(BookingOrderBo bo) {
        if (bo.getServiceItemId() == null) {
            throw new ServiceException("服务项目不能为空");
        }
        ServiceItem item = itemMapper.selectById(bo.getServiceItemId());
        if (item == null) {
            throw new ServiceException("服务项目不存在");
        }
        BookingOrder order = new BookingOrder();
        order.setOrderNo(genOrderNo());
        order.setCustomerUserId(bo.getCustomerUserId());
        order.setCustomerPhone(bo.getCustomerPhone());
        order.setStaffUserId(bo.getStaffUserId());
        order.setServiceItemId(item.getId());
        order.setServiceName(item.getName());
        order.setBookingTime(bo.getBookingTime());
        BigDecimal qty = bo.getQuantity() != null ? bo.getQuantity() : BigDecimal.ONE;
        order.setQuantity(qty);
        BigDecimal amount = bo.getAmount();
        if (amount == null) {
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            amount = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        }
        order.setAmount(amount);
        order.setPayStatus(BookingPayStatus.PENDING_PAY.getCode());
        order.setOrderStatus(BookingOrderStatus.PENDING_SERVICE.getCode());
        order.setRemark(bo.getRemark());
        return order;
    }

    private BookingOrder requireOrder(Long id) {
        if (id == null) {
            throw new ServiceException("订单ID不能为空");
        }
        BookingOrder exist = bookingMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("预约订单不存在");
        }
        return exist;
    }

    /**
     * 生成订单编号：BK + yyyyMMddHHmmss + 4 位随机
     */
    private String genOrderNo() {
        return "BK" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }

    /**
     * 批量回填顾客/员工昵称（sys_user.nick_name）
     */
    private void fillUserNames(List<BookingOrderVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        list.forEach(vo -> {
            if (vo.getCustomerUserId() != null) {
                userIds.add(vo.getCustomerUserId());
            }
            if (vo.getStaffUserId() != null) {
                userIds.add(vo.getStaffUserId());
            }
        });
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, userIds));
        Map<Long, String> nameMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
        Map<Long, String> phoneMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, u -> u.getPhonenumber() != null ? u.getPhonenumber() : "", (a, b) -> a));
        list.forEach(vo -> {
            String name = nameMap.get(vo.getCustomerUserId());
            // ★nickName 为空时回退手机号，避免前端显示"游客"且无手机号
            if (StrUtil.isBlank(name)) {
                name = phoneMap.get(vo.getCustomerUserId());
            }
            vo.setCustomerName(name);
            vo.setStaffName(nameMap.get(vo.getStaffUserId()));
            // ★顾客手机号为空时从 sys_user 回填（活动订单场景）
            if (StrUtil.isBlank(vo.getCustomerPhone()) && vo.getCustomerUserId() != null) {
                vo.setCustomerPhone(phoneMap.get(vo.getCustomerUserId()));
            }
        });
    }

    /**
     * 批量回填待召回顾客昵称（sys_user.nick_name，仅对有 customerUserId 的记录）
     */
    private void fillRecallCustomerNames(List<MicroRecallCustomerVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = list.stream().map(MicroRecallCustomerVo::getCustomerUserId)
            .filter(ObjectUtil::isNotNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, userIds));
        Map<Long, String> nameMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
        list.forEach(vo -> vo.setCustomerName(nameMap.get(vo.getCustomerUserId())));
    }
}
