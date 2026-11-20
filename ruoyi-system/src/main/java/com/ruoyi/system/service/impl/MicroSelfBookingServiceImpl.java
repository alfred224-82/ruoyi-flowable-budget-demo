package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.ResourceStatusLog;
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.SelfBookingSubmitBo;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.enums.BookingPayStatus;
import com.ruoyi.system.domain.vo.BookingPolicyVo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IBookingPolicyService;
import com.ruoyi.system.service.IMicroSelfBookingService;
import com.ruoyi.system.service.IMicroStaffScheduleService;
import com.ruoyi.system.service.IStaffPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自助预约服务实现（T21.1~T21.4，小微版）
 * <p>★复用 booking_order 表写入，可选关联资源通过 remark JSON 存储。
 * <p>remark JSON 格式：{@code {"bookingType":"project","mainResourceId":1,"optionalProjectIds":[],"optionalStaffIds":[2,3],"optionalRoomIds":[4,5],"bookingTimeSlot":"09:00-11:00"}}
 * <p>资源状态三态：idle→reserved（预约提交）→in_use（活动开始）→idle（结束/取消）。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroSelfBookingServiceImpl implements IMicroSelfBookingService {

    private final ServiceItemMapper itemMapper;
    private final ServiceCategoryMapper categoryMapper;
    private final BookingOrderMapper bookingMapper;
    private final PromotionActivityMapper activityMapper;
    private final ResourceStatusLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final UserTenantRoleMapper userTenantRoleMapper;
    private final IStaffPricingService staffPricingService;
    /** ★预约策略服务：提交前复核顾客自助预约开关（后端最终防线） */
    private final IBookingPolicyService bookingPolicyService;
    /** ★T22.5 M22 联动：员工排班服务，替代原 booking_order 数据源 */
    private final IMicroStaffScheduleService staffScheduleService;
    private final ObjectMapper objectMapper;

    /** space 分类类型标识 */
    private static final String CATEGORY_TYPE_SPACE = "space";
    /** salesman 标准角色编码 */
    private static final String ROLE_KEY_SALESMAN = "salesman";
    /** 资源状态常量 */
    private static final String STATUS_IDLE = "idle";
    private static final String STATUS_RESERVED = "reserved";
    private static final String STATUS_IN_USE = "in_use";
    /** ★助教手动状态：工作 / 休息（仅 idle 可被预约） */
    private static final String STATUS_WORKING = "working";
    private static final String STATUS_RESTING = "resting";
    /** T21.8 兜底服务时长（小时）：bookingTimeSlot 缺失/无法解析时，in_use 窗口 = start + 该时长 */
    private static final int DEFAULT_IN_USE_HOURS = 2;

    // ===== 查询数据源 =====

    @Override
    public List<ServiceItemVo> listAvailableItems() {
        // 查出本租户 space 分类的 category_id 集合
        Set<Long> spaceCategoryIds = getSpaceCategoryIds();
        // 查上架的 service_item，排除 space 分类
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .eq(ServiceItem::getStatus, "0")
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        List<ServiceItem> items = itemMapper.selectList(wrapper);
        // 排除 space 分类下的项目
        return items.stream()
            .filter(item -> !spaceCategoryIds.contains(item.getCategoryId()))
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    @Override
    public List<ServiceItemVo> listAvailableRooms(LocalDate bookingDate, String timeSlot) {
        // 查出本租户 space 分类的 category_id 集合
        Set<Long> spaceCategoryIds = getSpaceCategoryIds();
        if (spaceCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 查上架的 space 分类 service_item
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .in(ServiceItem::getCategoryId, spaceCategoryIds)
            .eq(ServiceItem::getStatus, "0")
            .eq(ServiceItem::getResourceStatus, STATUS_IDLE)
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        List<ServiceItem> rooms = itemMapper.selectList(wrapper);
        // 排除该日期已有预约的房间（从 booking_order.remark JSON 中解析 optionalRoomIds）
        Set<Long> bookedRoomIds = getBookedResourceIds(bookingDate, timeSlot, true);
        // ★排除 Boss 已发布活动中占用的房间（promotion_activity.selectedRoomIds）
        Set<Long> activityRoomIds = getActivityBookedResourceIds(bookingDate, true);
        bookedRoomIds.addAll(activityRoomIds);
        // ★重复房间修复：按名称去重保留首条——防御历史并发自愈残留的同名重复房间在顾客选房间列表出现两次
        return rooms.stream()
            .filter(room -> !bookedRoomIds.contains(room.getId()))
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    ServiceItem::getName,
                    this::toVo,
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }

    @Override
    public List<Map<String, Object>> listAvailableStaff(LocalDate bookingDate) {
        // ① ★从 user_tenant_role 查询本租户 salesman 角色用户（而非 sys_user_role，
        //   避免 sys_user_role 中存在未通过人员管理分配的脏数据导致多出员工）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptyList();
        }
        List<Long> allStaffIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        // ③ ★T22.5 M22 联动改造：优先从 staff_schedule 查询「当天已发布排班」的助教 user_id 集合；
        //   若为空（免费版未启用 M22 或当天无排班数据），回退到原 booking_order 数据源以保证向后兼容。
        Set<Long> staffWithSchedule = staffScheduleService.listScheduledStaffIds(bookingDate);
        boolean fromSchedule = !staffWithSchedule.isEmpty();
        // 当天预约数（用于展示 bookingCount，仍走 booking_order 统计）
        Date dayStart = Date.from(bookingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(bookingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<BookingOrder> dayBookings = bookingMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .in(BookingOrder::getStaffUserId, allStaffIds)
                .ge(BookingOrder::getBookingTime, dayStart)
                .lt(BookingOrder::getBookingTime, dayEnd)
                .ne(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode()));
        if (!fromSchedule) {
            // 回退：无排班数据时，按 booking_order 推导「当天有预约的助教」
            staffWithSchedule = dayBookings.stream()
                .map(BookingOrder::getStaffUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        } else {
            // 与 salesman 全量取交集，避免历史脏数据（非 salesman 用户被误排班）
            Set<Long> finalAllStaffIds = new HashSet<>(allStaffIds);
            staffWithSchedule = staffWithSchedule.stream()
                .filter(finalAllStaffIds::contains).collect(Collectors.toSet());
        }
        if (staffWithSchedule.isEmpty()) {
            return Collections.emptyList();
        }
        // ④ 查 sys_user 获取头像/昵称/简介
        List<SysUser> users = userMapper.selectBatchIds(staffWithSchedule);
        // ⑤ ★T21.7 批量查询助教小时单价（顾客可见）
        List<Long> activeStaffIds = users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .map(SysUser::getUserId).collect(Collectors.toList());
        Map<Long, BigDecimal> priceMap = staffPricingService.batchGetHourlyRates(activeStaffIds);
        // ⑥ ★仅空闲助教可被预约（工作/休息/预约中/使用中 均不可约）
        Map<Long, String> staffStatusMap = batchCurrentStatus("staff", activeStaffIds);
        // ★排除 Boss 已发布活动中占用的助教（promotion_activity.selectedStaffIds）
        Set<Long> activityStaffIds = getActivityBookedResourceIds(bookingDate, false);
        // ★T22.6 当天预约时，校验助教是否在岗（未到上班时间/已下班 → 休息状态不可约）
        Map<Long, List<int[]>> shiftTimeMap = Collections.emptyMap();
        if (bookingDate.isEqual(LocalDate.now())) {
            shiftTimeMap = staffScheduleService.getStaffShiftTimes(bookingDate);
        }
        final Map<Long, List<int[]>> finalShiftTimeMap = shiftTimeMap;
        Set<Long> finalStaffWithSchedule = staffWithSchedule;
        return users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .filter(u -> STATUS_IDLE.equals(staffStatusMap.getOrDefault(u.getUserId(), STATUS_IDLE)))
            .filter(u -> !activityStaffIds.contains(u.getUserId()))
            .filter(u -> isWithinShiftTime(u.getUserId(), finalShiftTimeMap))
            .map(u -> {
                Map<String, Object> map = new LinkedHashMap<>(7);
                map.put("id", u.getUserId());
                map.put("name", u.getNickName());
                map.put("avatar", u.getAvatar());
                map.put("description", u.getRemark());
                map.put("bookingCount", dayBookings.stream()
                    .filter(b -> u.getUserId().equals(b.getStaffUserId())).count());
                // ★T21.7 小时单价（未设置返回 null）
                map.put("hourlyRate", priceMap.get(u.getUserId()));
                // ★T22.5 数据源标识：true=来自 staff_schedule 已发布排班；false=回退到 booking_order 历史逻辑
                map.put("scheduled", fromSchedule && finalStaffWithSchedule.contains(u.getUserId()));
                return map;
            })
            .collect(Collectors.toList());
    }

    // ===== 提交预约 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitSelfBooking(SelfBookingSubmitBo bo) {
        // ★预约策略闸门：租户关闭顾客自助预约时直接拒绝（前端隐藏入口仅为第一层防线）
        BookingPolicyVo policy = bookingPolicyService.queryPolicy();
        if (policy == null || policy.getCustomerSelfBooking() == null || policy.getCustomerSelfBooking() != 1) {
            throw new ServiceException("本店未开启顾客自助预约");
        }
        // ★REQ-01 三类资源控制最终防线：未开放资源拦截 + 必填资源校验 + 锁死行业强制项目
        validateResourcePolicy(policy, bo);
        // ① 校验预约日期（≤ 明天）
        LocalDate bookingDate = bo.getBookingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        validateBookingTime(bookingDate);
        // ★校验预约时段：当天预约时，开始时间必须晚于当前时间1小时、结束时间不能早于当前时间
        validateTimeSlotNotPast(bookingDate, bo.getBookingTimeSlot());

        // ② 构建 booking_order
        Long customerUserId = LoginHelper.getUserId();
        BookingOrder order = new BookingOrder();
        order.setOrderNo(genOrderNo());
        order.setCustomerUserId(customerUserId);
        order.setCustomerPhone(StrUtil.isNotBlank(bo.getCustomerPhone()) ? bo.getCustomerPhone() : getCustomerPhone(customerUserId));
        if (StrUtil.isBlank(order.getCustomerPhone())) {
            throw new ServiceException("顾客手机号不能为空");
        }

        // 按 bookingType 设置主资源
        String bookingType = bo.getBookingType();
        Long mainId = bo.getMainResourceId();
        switch (bookingType) {
            case "project":
                // 主资源=项目 → serviceItemId
                ServiceItem project = itemMapper.selectById(mainId);
                if (project == null) {
                    throw new ServiceException("服务项目不存在");
                }
                order.setServiceItemId(project.getId());
                order.setServiceName(project.getName());
                order.setStaffUserId(CollUtil.isNotEmpty(bo.getOptionalStaffIds()) ? bo.getOptionalStaffIds().get(0) : null);
                break;
            case "room":
                // 主资源=房间 → serviceItemId 设房间关联项目（如有），staffUserId 设可选助教首位
                ServiceItem room = itemMapper.selectById(mainId);
                if (room == null) {
                    throw new ServiceException("房间不存在");
                }
                // 房间作为主资源时，serviceItemId 仍指向房间本身（service_item.id）
                order.setServiceItemId(room.getId());
                order.setServiceName(room.getName());
                order.setStaffUserId(CollUtil.isNotEmpty(bo.getOptionalStaffIds()) ? bo.getOptionalStaffIds().get(0) : null);
                break;
            case "staff":
                // 主资源=助教 → staffUserId
                order.setStaffUserId(mainId);
                // 如果有可选项目，设为 serviceItemId
                if (CollUtil.isNotEmpty(bo.getOptionalProjectIds())) {
                    ServiceItem optProject = itemMapper.selectById(bo.getOptionalProjectIds().get(0));
                    if (optProject != null) {
                        order.setServiceItemId(optProject.getId());
                        order.setServiceName(optProject.getName());
                    }
                } else if (CollUtil.isNotEmpty(bo.getOptionalRoomIds())) {
                    // 无可选项目但有可选房间时，serviceItemId 指向第一个可选房间
                    ServiceItem optRoom = itemMapper.selectById(bo.getOptionalRoomIds().get(0));
                    if (optRoom != null) {
                        order.setServiceItemId(optRoom.getId());
                        order.setServiceName(optRoom.getName());
                    }
                } else {
                    // 无可选项目/房间，尝试用助教名称作为 serviceName
                    SysUser staff = userMapper.selectUserById(mainId);
                    order.setServiceName(staff != null ? staff.getNickName() + "（助教预约）" : "助教预约");
                }
                break;
            default:
                throw new ServiceException("不支持的预约类型：" + bookingType);
        }

        // ★校验提交资源ID存在性：无效ID（如前端数字转换丢精度）立即失败，
        //   避免订单费用明细名称回退"已删除"、单价回退 0
        assertResourceExists(bookingType, mainId, bo.getOptionalProjectIds(), bo.getOptionalStaffIds(), bo.getOptionalRoomIds());

        // ★T22.6 当天预约时，校验所选助教是否在班次时间内（未到上班时间/已下班不可约）
        validateStaffShiftTime(bookingDate, bookingType, mainId, bo.getOptionalStaffIds());

        // 预约时间
        order.setBookingTime(bo.getBookingDate());
        // ★服务时长（小时）：取前端提交的 quantity（选中时段数），缺省 1
        int hours = (bo.getQuantity() != null && bo.getQuantity() > 0) ? bo.getQuantity() : 1;
        order.setQuantity(BigDecimal.valueOf(hours));
        // ★金额明细：主资源 + 可选项目/助教/房间，各自 单价×时长；总额 = 明细之和
        List<Map<String, Object>> costDetails = buildCostDetails(bo, hours);
        BigDecimal amount = BigDecimal.ZERO;
        for (Map<String, Object> d : costDetails) {
            amount = amount.add((BigDecimal) d.get("subtotal"));
        }
        order.setAmount(amount);
        order.setPayStatus(BookingPayStatus.PENDING_PAY.getCode());
        order.setOrderStatus(BookingOrderStatus.PENDING_SERVICE.getCode());

        // 将自助预约扩展信息存入 remark（JSON）
        Map<String, Object> remarkMap = new LinkedHashMap<>();
        remarkMap.put("bookingType", bookingType);
        remarkMap.put("mainResourceId", mainId);
        remarkMap.put("optionalProjectIds", bo.getOptionalProjectIds());
        remarkMap.put("optionalStaffIds", bo.getOptionalStaffIds());
        remarkMap.put("optionalRoomIds", bo.getOptionalRoomIds());
        remarkMap.put("bookingTimeSlot", bo.getBookingTimeSlot());
        remarkMap.put("hours", hours);
        if (bo.getPeopleCount() != null) {
            remarkMap.put("peopleCount", bo.getPeopleCount());
        }
        remarkMap.put("costDetails", costDetails);
        remarkMap.put("totalAmount", amount);
        if (StrUtil.isNotBlank(bo.getCustomerName())) {
            remarkMap.put("customerName", bo.getCustomerName());
        }
        if (StrUtil.isNotBlank(bo.getRemark())) {
            remarkMap.put("remark", bo.getRemark());
        }
        try {
            order.setRemark(objectMapper.writeValueAsString(remarkMap));
        } catch (JsonProcessingException e) {
            log.warn("序列化 remark 失败", e);
            order.setRemark(bo.getRemark());
        }

        // ★仅空闲资源可被预约：校验主资源 + 可选关联助教/房间当前均为 idle
        assertBookable("staff", "staff".equals(bookingType) ? mainId : null, bo.getOptionalStaffIds());
        assertBookable("room", "room".equals(bookingType) ? mainId : null, bo.getOptionalRoomIds());

        bookingMapper.insert(order);

        // ③ 更新资源状态为 reserved + 写日志
        Long operatorId = LoginHelper.getUserId();
        // 主资源状态更新
        if ("project".equals(bookingType) || "room".equals(bookingType)) {
            updateResourceStatus("room", mainId, getResourceName("room", mainId),
                STATUS_IDLE, STATUS_RESERVED, operatorId, order.getId(), "自助预约提交");
        }
        // 可选关联助教
        if (CollUtil.isNotEmpty(bo.getOptionalStaffIds())) {
            for (Long staffId : bo.getOptionalStaffIds()) {
                updateResourceStatus("staff", staffId, getResourceName("staff", staffId),
                    STATUS_IDLE, STATUS_RESERVED, operatorId, order.getId(), "自助预约提交（关联助教）");
            }
        }
        // 可选关联房间
        if (CollUtil.isNotEmpty(bo.getOptionalRoomIds())) {
            for (Long roomId : bo.getOptionalRoomIds()) {
                // 跳过主资源（已处理）
                if ("room".equals(bookingType) && roomId.equals(mainId)) {
                    continue;
                }
                updateResourceStatus("room", roomId, getResourceName("room", roomId),
                    STATUS_IDLE, STATUS_RESERVED, operatorId, order.getId(), "自助预约提交（关联房间）");
            }
        }
        // 主资源=助教时，更新助教状态
        if ("staff".equals(bookingType)) {
            updateResourceStatus("staff", mainId, getResourceName("staff", mainId),
                STATUS_IDLE, STATUS_RESERVED, operatorId, order.getId(), "自助预约提交（助教）");
        }

        return order.getId();
    }

    /**
     * ★REQ-01 三类资源控制校验（后端最终防线）
     * <p>按 policy 拦截未开放资源、缺失必填资源；锁死行业（旅游/按摩/足浴）强制项目非空。
     * <p>enabled 缺省视为 1（开放），required 缺省视为 0（可选）。
     */
    private void validateResourcePolicy(BookingPolicyVo policy, SelfBookingSubmitBo bo) {
        String bookingType = bo.getBookingType();
        boolean usesProject = "project".equals(bookingType) || CollUtil.isNotEmpty(bo.getOptionalProjectIds());
        boolean usesRoom = "room".equals(bookingType) || CollUtil.isNotEmpty(bo.getOptionalRoomIds());
        boolean usesStaff = "staff".equals(bookingType) || CollUtil.isNotEmpty(bo.getOptionalStaffIds());

        int projectEnabled = policy.getProjectBookingEnabled() == null ? 1 : policy.getProjectBookingEnabled();
        int projectRequired = policy.getProjectBookingRequired() == null ? 0 : policy.getProjectBookingRequired();
        int roomEnabled = policy.getRoomBookingEnabled() == null ? 1 : policy.getRoomBookingEnabled();
        int roomRequired = policy.getRoomBookingRequired() == null ? 0 : policy.getRoomBookingRequired();
        int staffEnabled = policy.getStaffBookingEnabled() == null ? 1 : policy.getStaffBookingEnabled();
        int staffRequired = policy.getStaffBookingRequired() == null ? 0 : policy.getStaffBookingRequired();

        // 项目
        if (projectEnabled == 0 && usesProject) {
            throw new ServiceException("本店未开放项目预约");
        }
        if (projectRequired == 1 && !usesProject) {
            throw new ServiceException("请选择预约项目");
        }
        // 场地
        if (roomEnabled == 0 && usesRoom) {
            throw new ServiceException("本店未开放场地预约");
        }
        if (roomRequired == 1 && !usesRoom) {
            throw new ServiceException("请选择预约场地");
        }
        // 员工
        if (staffEnabled == 0 && usesStaff) {
            throw new ServiceException("本店未开放员工预约");
        }
        if (staffRequired == 1 && !usesStaff) {
            throw new ServiceException("请选择预约员工");
        }
        // 锁死行业（旅游/按摩/足浴）：项目强制必选（queryPolicy 已将 projectBookingRequired 置 1，此处双保险）
        String industry = policy.getIndustryCode();
        if (industry != null && ("travel".equals(industry) || "massage".equals(industry) || "foot_bath".equals(industry))
            && !usesProject) {
            throw new ServiceException("该行业必须选择预约项目");
        }
    }

    /**
     * 构建预约费用明细（主资源 + 可选项目/助教/房间，各自 单价×时长）。
     * <p>每项含 name/type/unitPrice/hours/subtotal，供顾客端展示明细与合计。
     *
     * @param bo    提交参数
     * @param hours 服务时长（小时）
     */
    private List<Map<String, Object>> buildCostDetails(SelfBookingSubmitBo bo, int hours) {
        List<Map<String, Object>> details = new ArrayList<>();
        String bookingType = bo.getBookingType();
        Long mainId = bo.getMainResourceId();
        // 主资源
        if ("project".equals(bookingType) || "room".equals(bookingType)) {
            ServiceItem item = itemMapper.selectById(mainId);
            if (item != null) {
                details.add(costDetail(item.getName(), "room".equals(bookingType) ? "房间" : "项目",
                    item.getPrice(), hours));
            }
        } else if ("staff".equals(bookingType)) {
            BigDecimal rate = staffPricingService.batchGetHourlyRates(Collections.singletonList(mainId)).get(mainId);
            details.add(costDetail(getResourceName("staff", mainId), "助教", rate, hours));
        }
        // 可选项目
        if (CollUtil.isNotEmpty(bo.getOptionalProjectIds())) {
            for (Long pid : bo.getOptionalProjectIds()) {
                if ("project".equals(bookingType) && pid.equals(mainId)) {
                    continue;
                }
                ServiceItem item = itemMapper.selectById(pid);
                if (item != null) {
                    details.add(costDetail(item.getName(), "项目", item.getPrice(), hours));
                }
            }
        }
        // 可选助教
        if (CollUtil.isNotEmpty(bo.getOptionalStaffIds())) {
            Map<Long, BigDecimal> rates = staffPricingService.batchGetHourlyRates(bo.getOptionalStaffIds());
            for (Long sid : bo.getOptionalStaffIds()) {
                if ("staff".equals(bookingType) && sid.equals(mainId)) {
                    continue;
                }
                details.add(costDetail(getResourceName("staff", sid), "助教", rates.get(sid), hours));
            }
        }
        // 可选房间
        if (CollUtil.isNotEmpty(bo.getOptionalRoomIds())) {
            for (Long rid : bo.getOptionalRoomIds()) {
                if ("room".equals(bookingType) && rid.equals(mainId)) {
                    continue;
                }
                ServiceItem item = itemMapper.selectById(rid);
                if (item != null) {
                    details.add(costDetail(item.getName(), "房间", item.getPrice(), hours));
                }
            }
        }
        return details;
    }

    /**
     * 单条费用明细：subtotal = unitPrice × hours（单价缺失按 0 计）
     */
    private Map<String, Object> costDetail(String name, String type, BigDecimal unitPrice, int hours) {
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        Map<String, Object> d = new LinkedHashMap<>(5);
        d.put("name", name);
        d.put("type", type);
        d.put("unitPrice", price);
        d.put("hours", hours);
        d.put("subtotal", price.multiply(BigDecimal.valueOf(hours)));
        return d;
    }

    /**
     * 校验提交资源ID存在性（助教→sys_user，项目/房间→service_item）。
     * <p>助教ID为雪花ID，前端若误用 Number() 转换会丢精度变成不存在的ID，
     * 不校验则费用明细名称回退"已删除"、单价回退 0，故在此快速失败。
     */
    private void assertResourceExists(String bookingType, Long mainId, List<Long> projectIds,
                                      List<Long> staffIds, List<Long> roomIds) {
        Set<Long> staffToCheck = new HashSet<>();
        if ("staff".equals(bookingType) && mainId != null) {
            staffToCheck.add(mainId);
        }
        if (CollUtil.isNotEmpty(staffIds)) {
            staffToCheck.addAll(staffIds);
        }
        if (!staffToCheck.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(staffToCheck);
            if (users.size() != staffToCheck.size()) {
                throw new ServiceException("部分助教不存在或已下架，请返回重新选择");
            }
        }
        Set<Long> itemToCheck = new HashSet<>();
        if (!"staff".equals(bookingType) && mainId != null) {
            itemToCheck.add(mainId);
        }
        if (CollUtil.isNotEmpty(projectIds)) {
            itemToCheck.addAll(projectIds);
        }
        if (CollUtil.isNotEmpty(roomIds)) {
            itemToCheck.addAll(roomIds);
        }
        if (!itemToCheck.isEmpty()) {
            List<ServiceItem> items = itemMapper.selectBatchIds(itemToCheck);
            if (items.size() != itemToCheck.size()) {
                throw new ServiceException("部分房间或服务项目不存在或已下架，请返回重新选择");
            }
        }
    }

    // ===== 校验 =====

    @Override
    public void validateBookingTime(LocalDate bookingDate) {
        if (bookingDate == null) {
            throw new ServiceException("预约日期不能为空");
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (bookingDate.isAfter(tomorrow)) {
            throw new ServiceException("预约日期不能超过明天");
        }
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new ServiceException("预约日期不能早于今天");
        }
    }

    /**
     * 校验预约时段是否已过期（仅当天预约需校验）。
     * <p>规则：结束时间不能早于当前时间；开始时间必须晚于当前时间至少 1 小时
     * （如当前 17 点，最早可选开始时间为 18 点）。
     *
     * @param bookingDate 预约日期
     * @param timeSlot    预约时段（HH:mm-HH:mm）
     */
    private void validateTimeSlotNotPast(LocalDate bookingDate, String timeSlot) {
        if (bookingDate == null || !bookingDate.isEqual(LocalDate.now())) {
            // 非当天预约（如明天）无需时段过期校验
            return;
        }
        if (StrUtil.isBlank(timeSlot) || !timeSlot.contains("-")) {
            throw new ServiceException("请选择预约时间段");
        }
        String[] parts = timeSlot.split("-");
        java.time.LocalTime start;
        java.time.LocalTime end;
        try {
            start = parseHhmm(parts[0].trim());
            end = parseHhmm(parts[1].trim());
        } catch (Exception e) {
            throw new ServiceException("预约时间段格式不正确");
        }
        java.time.LocalTime now = java.time.LocalTime.now();
        // ① 结束时间不能早于当前时间
        if (!end.isAfter(now)) {
            throw new ServiceException("预约结束时间不能早于当前时间");
        }
        // ② 开始时间必须晚于当前时间至少 1 小时（当前 17 点 → 最早 18 点）
        int earliestStartHour = now.getHour() + 1;
        if (start.getHour() < earliestStartHour) {
            throw new ServiceException("预约开始时间需晚于当前时间至少1小时，最早可选 " + earliestStartHour + ":00");
        }
    }

    // ===== 释放资源 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseResources(Long bookingId) {
        if (bookingId == null) {
            return;
        }
        BookingOrder order = bookingMapper.selectById(bookingId);
        if (order == null) {
            log.warn("releaseResources: 订单不存在，bookingId={}", bookingId);
            return;
        }
        // 解析 remark JSON 获取关联资源
        Map<String, Object> remarkMap = parseRemark(order.getRemark());
        if (remarkMap.isEmpty()) {
            return;
        }
        String bookingType = (String) remarkMap.get("bookingType");
        Long mainResourceId = toLong(remarkMap.get("mainResourceId"));
        List<Long> optionalStaffIds = toLongList(remarkMap.get("optionalStaffIds"));
        List<Long> optionalRoomIds = toLongList(remarkMap.get("optionalRoomIds"));

        Long operatorId = LoginHelper.getUserId();

        // 释放主资源
        if (mainResourceId != null) {
            if ("project".equals(bookingType) || "room".equals(bookingType)) {
                releaseSingleResource("room", mainResourceId, operatorId, bookingId, "预约取消/释放");
            } else if ("staff".equals(bookingType)) {
                releaseSingleResource("staff", mainResourceId, operatorId, bookingId, "预约取消/释放");
            }
        }
        // 释放可选关联助教
        if (CollUtil.isNotEmpty(optionalStaffIds)) {
            for (Long staffId : optionalStaffIds) {
                releaseSingleResource("staff", staffId, operatorId, bookingId, "预约取消/释放（关联助教）");
            }
        }
        // 释放可选关联房间
        if (CollUtil.isNotEmpty(optionalRoomIds)) {
            for (Long roomId : optionalRoomIds) {
                if ("room".equals(bookingType) && roomId.equals(mainResourceId)) {
                    continue; // 已处理
                }
                releaseSingleResource("room", roomId, operatorId, bookingId, "预约取消/释放（关联房间）");
            }
        }
    }

    // ===== 资源状态自动流转（T21.8）=====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int advanceResourceStatusByTime() {
        Date now = new Date();
        LocalDate today = LocalDate.now();
        Date dayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        // 扫描当天非已取消的预约订单
        List<BookingOrder> orders = bookingMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .ge(BookingOrder::getBookingTime, dayStart)
                .lt(BookingOrder::getBookingTime, dayEnd)
                .ne(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode()));
        int changed = 0;
        for (BookingOrder order : orders) {
            Map<String, Object> remarkMap = parseRemark(order.getRemark());
            if (remarkMap.isEmpty()) {
                continue;
            }
            String bookingType = (String) remarkMap.get("bookingType");
            String timeSlot = (String) remarkMap.get("bookingTimeSlot");
            Date[] window = parseTimeSlotWindow(order.getBookingTime(), timeSlot);
            Date start = window[0];
            Date end = window[1];
            String orderStatus = order.getOrderStatus();
            boolean finished = BookingOrderStatus.COMPLETED.getCode().equals(orderStatus)
                || BookingOrderStatus.NO_SHOW.getCode().equals(orderStatus);

            // 判定目标状态：已结束或超出时间窗 → idle；处于时间窗内且待服务 → in_use；否则不处理
            String targetStatus;
            if (finished || !now.before(end)) {
                targetStatus = STATUS_IDLE;
            } else if (!now.before(start) && BookingOrderStatus.PENDING_SERVICE.getCode().equals(orderStatus)) {
                targetStatus = STATUS_IN_USE;
            } else {
                continue;
            }

            // 收集本订单关联的全部资源（主资源 + 可选助教 + 可选房间），去重
            Long mainResourceId = toLong(remarkMap.get("mainResourceId"));
            List<Long> optionalStaffIds = toLongList(remarkMap.get("optionalStaffIds"));
            List<Long> optionalRoomIds = toLongList(remarkMap.get("optionalRoomIds"));
            Set<String> handled = new HashSet<>();
            // 主资源
            if (mainResourceId != null) {
                if ("staff".equals(bookingType)) {
                    changed += advanceOne("staff", mainResourceId, targetStatus, order.getId(), handled);
                } else if ("project".equals(bookingType) || "room".equals(bookingType)) {
                    changed += advanceOne("room", mainResourceId, targetStatus, order.getId(), handled);
                }
            }
            // 可选关联助教
            for (Long staffId : optionalStaffIds) {
                changed += advanceOne("staff", staffId, targetStatus, order.getId(), handled);
            }
            // 可选关联房间
            for (Long roomId : optionalRoomIds) {
                changed += advanceOne("room", roomId, targetStatus, order.getId(), handled);
            }
        }
        return changed;
    }

    /**
     * 推进单个资源到目标状态（幂等：当前状态==目标状态则跳过）
     *
     * @return 发生变更返回 1，否则 0
     */
    private int advanceOne(String resourceType, Long resourceId, String targetStatus,
                           Long bookingId, Set<String> handled) {
        if (resourceId == null) {
            return 0;
        }
        String key = resourceType + ":" + resourceId;
        if (!handled.add(key)) {
            return 0; // 本订单内已处理，去重
        }
        String currentStatus = getCurrentResourceStatus(resourceType, resourceId);
        if (targetStatus.equals(currentStatus)) {
            return 0; // 幂等：已达目标状态
        }
        // 仅处理 reserved→in_use 与 in_use→idle；idle 资源不被定时任务主动拉入 in_use
        boolean valid = (STATUS_RESERVED.equals(currentStatus) && STATUS_IN_USE.equals(targetStatus))
            || (STATUS_IN_USE.equals(currentStatus) && STATUS_IDLE.equals(targetStatus));
        if (!valid) {
            return 0;
        }
        updateResourceStatus(resourceType, resourceId, getResourceName(resourceType, resourceId),
            currentStatus, targetStatus, null, bookingId, "系统自动流转（resourceInUseFlowJob）");
        return 1;
    }

    /**
     * 解析预约时间窗 [start, end]（基于到店日期 + 时段 HH:mm-HH:mm）
     * <p>时段缺失/无法解析时兜底：start = bookingTime，end = start + {@link #DEFAULT_IN_USE_HOURS} 小时。
     */
    private Date[] parseTimeSlotWindow(Date bookingTime, String timeSlot) {
        Date fallbackStart = bookingTime != null ? bookingTime : new Date();
        if (StrUtil.isBlank(timeSlot) || !timeSlot.contains("-")) {
            return new Date[]{fallbackStart, DateUtil.offsetHour(fallbackStart, DEFAULT_IN_USE_HOURS)};
        }
        try {
            String[] parts = timeSlot.split("-");
            LocalDate baseDate = bookingTime != null
                ? bookingTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
            String startStr = parts[0].trim();
            String endStr = parts[1].trim();
            Date start = Date.from(baseDate.atTime(parseHhmm(startStr)).atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(baseDate.atTime(parseHhmm(endStr)).atZone(ZoneId.systemDefault()).toInstant());
            if (!end.after(start)) {
                // 时段非法（end <= start）走兜底
                return new Date[]{fallbackStart, DateUtil.offsetHour(fallbackStart, DEFAULT_IN_USE_HOURS)};
            }
            return new Date[]{start, end};
        } catch (Exception e) {
            log.warn("解析预约时段失败，timeSlot={}，走兜底窗口", timeSlot, e);
            return new Date[]{fallbackStart, DateUtil.offsetHour(fallbackStart, DEFAULT_IN_USE_HOURS)};
        }
    }

    /**
     * ★T22.6 校验助教是否在班次时间内（当天预约时生效）
     * <p>未到上班时间或已下班 → 视为休息状态，不可预约。
     *
     * @param bookingDate 预约日期
     * @param bookingType 预约类型
     * @param mainId      主资源ID
     * @param optionalStaffIds 可选助教ID
     */
    private void validateStaffShiftTime(LocalDate bookingDate, String bookingType, Long mainId, List<Long> optionalStaffIds) {
        if (!bookingDate.isEqual(LocalDate.now())) {
            return; // 非当天预约不校验班次时间
        }
        // 收集需要校验的助教ID
        Set<Long> staffIds = new HashSet<>();
        if ("staff".equals(bookingType) && mainId != null) {
            staffIds.add(mainId);
        }
        if (CollUtil.isNotEmpty(optionalStaffIds)) {
            staffIds.addAll(optionalStaffIds);
        }
        if (staffIds.isEmpty()) {
            return;
        }
        Map<Long, List<int[]>> shiftTimeMap = staffScheduleService.getStaffShiftTimes(bookingDate);
        int nowMin = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        for (Long staffId : staffIds) {
            List<int[]> shifts = shiftTimeMap.get(staffId);
            if (CollUtil.isEmpty(shifts)) {
                // 无排班数据时不拦截（可能为回退数据源），由 listAvailableStaff 层过滤
                continue;
            }
            boolean withinShift = shifts.stream()
                .anyMatch(s -> nowMin >= s[0] && nowMin <= s[1]);
            if (!withinShift) {
                String name = getResourceName("staff", staffId);
                throw new ServiceException("助教「" + name + "」当前不在班次时间内，无法预约");
            }
        }
    }

    /**
     * ★T22.6 判断助教当前是否在其班次时间内（stream filter 用）
     * <p>shiftTimeMap 为空（非当天或无排班数据）→ 放行。
     *
     * @param staffId      助教 user_id
     * @param shiftTimeMap staffId → 班次时间列表（int[2] = {startMin, endMin}）
     * @return true=在岗可约 / false=不在班次时间内
     */
    private boolean isWithinShiftTime(Long staffId, Map<Long, List<int[]>> shiftTimeMap) {
        if (shiftTimeMap.isEmpty()) {
            return true; // 非当天或无排班数据，放行
        }
        List<int[]> shifts = shiftTimeMap.get(staffId);
        if (CollUtil.isEmpty(shifts)) {
            return true; // 该助教无排班数据（回退数据源），放行
        }
        int nowMin = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        return shifts.stream().anyMatch(s -> nowMin >= s[0] && nowMin <= s[1]);
    }

    /**
     * 解析 HH:mm 格式时间为 LocalTime
     */
    private java.time.LocalTime parseHhmm(String hhmm) {
        String[] hm = hhmm.split(":");
        int hour = Integer.parseInt(hm[0].trim());
        int minute = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
        return java.time.LocalTime.of(hour, minute);
    }

    // ===== Boss PC 资源状态管理 =====

    @Override
    public List<ServiceItemVo> listRoomResourceStatus() {
        Set<Long> spaceCategoryIds = getSpaceCategoryIds();
        if (spaceCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .in(ServiceItem::getCategoryId, spaceCategoryIds)
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        List<ServiceItem> rooms = itemMapper.selectList(wrapper);
        // ★防御性去重：自愈多次触发或手动操作可能产生同名房间记录，按名称保留首条
        return rooms.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    ServiceItem::getName,
                    this::toVo,
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualReleaseResource(String resourceType, Long resourceId, String reason) {
        if (StrUtil.isBlank(resourceType) || resourceId == null) {
            throw new ServiceException("资源类型和资源ID不能为空");
        }
        if (StrUtil.isBlank(reason)) {
            throw new ServiceException("手动释放必须填写原因");
        }
        String resourceName = getResourceName(resourceType, resourceId);
        String currentStatus = getCurrentResourceStatus(resourceType, resourceId);
        if (STATUS_IDLE.equals(currentStatus)) {
            throw new ServiceException("资源当前已空闲，无需释放");
        }
        Long operatorId = LoginHelper.getUserId();
        updateResourceStatus(resourceType, resourceId, resourceName,
            currentStatus, STATUS_IDLE, operatorId, null, "手动释放：" + reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setResourceStatus(String resourceType, Long resourceId, String targetStatus, String reason) {
        if (StrUtil.isBlank(resourceType) || resourceId == null || StrUtil.isBlank(targetStatus)) {
            throw new ServiceException("资源类型/资源ID/目标状态不能为空");
        }
        // 校验目标状态合法性（助教：空闲/工作/休息；房间：空闲/使用）
        if ("staff".equals(resourceType)) {
            if (!STATUS_IDLE.equals(targetStatus) && !STATUS_WORKING.equals(targetStatus) && !STATUS_RESTING.equals(targetStatus)) {
                throw new ServiceException("助教状态仅支持：空闲/工作/休息");
            }
        } else if ("room".equals(resourceType)) {
            if (!STATUS_IDLE.equals(targetStatus) && !STATUS_IN_USE.equals(targetStatus)) {
                throw new ServiceException("房间状态仅支持：空闲/使用");
            }
        } else {
            throw new ServiceException("不支持的资源类型：" + resourceType);
        }
        String currentStatus = getCurrentResourceStatus(resourceType, resourceId);
        if (currentStatus.equals(targetStatus)) {
            throw new ServiceException("资源当前已是该状态，无需重复设置");
        }
        Long operatorId = LoginHelper.getUserId();
        String resourceName = getResourceName(resourceType, resourceId);
        updateResourceStatus(resourceType, resourceId, resourceName,
            currentStatus, targetStatus, operatorId, null,
            StrUtil.isNotBlank(reason) ? "手动设置：" + reason : "手动设置状态");
    }

    @Override
    public List<Map<String, Object>> listStaffResourceStatus() {
        // ① ★从 user_tenant_role 查询本租户 salesman 角色用户（而非 sys_user_role）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptyList();
        }
        List<Long> staffIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        // ③ 查 sys_user 获取头像/昵称/状态
        List<SysUser> users = userMapper.selectBatchIds(staffIds);
        // ④ 批量查每个助教最新的 resource_status_log 推导当前状态
        Map<Long, String> statusMap = batchCurrentStatus("staff", staffIds);
        // ⑤ 组装结果
        return users.stream()
            .map(u -> {
                Map<String, Object> map = new LinkedHashMap<>(6);
                map.put("userId", u.getUserId());
                map.put("nickName", u.getNickName());
                map.put("avatar", u.getAvatar());
                map.put("status", u.getStatus());
                map.put("resourceStatus", statusMap.getOrDefault(u.getUserId(), STATUS_IDLE));
                map.put("hourlyRate", staffPricingService.batchGetHourlyRates(
                    Collections.singletonList(u.getUserId())).get(u.getUserId()));
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<com.ruoyi.system.domain.vo.ResourceStatusLogVo> listResourceStatusLogs(String resourceType, Long resourceId) {
        LambdaQueryWrapper<ResourceStatusLog> wrapper = Wrappers.<ResourceStatusLog>lambdaQuery();
        if (StrUtil.isNotBlank(resourceType)) {
            wrapper.eq(ResourceStatusLog::getResourceType, resourceType);
        }
        if (resourceId != null) {
            wrapper.eq(ResourceStatusLog::getResourceId, resourceId);
        }
        wrapper.orderByDesc(ResourceStatusLog::getOperateTime)
            .last("LIMIT 50");
        List<ResourceStatusLog> logs = logMapper.selectList(wrapper);
        return logs.stream().map(this::toLogVo).collect(Collectors.toList());
    }

    // ===== 内部方法 =====

    /**
     * 释放单个资源（reserved/idle → idle + 写日志）
     */
    private void releaseSingleResource(String resourceType, Long resourceId, Long operatorId,
                                       Long bookingId, String reason) {
        String resourceName = getResourceName(resourceType, resourceId);
        String currentStatus = getCurrentResourceStatus(resourceType, resourceId);
        if (STATUS_IDLE.equals(currentStatus)) {
            return; // 已空闲，无需释放
        }
        updateResourceStatus(resourceType, resourceId, resourceName,
            currentStatus, STATUS_IDLE, operatorId, bookingId, reason);
    }

    /**
     * 更新资源状态 + 写日志
     */
    private void updateResourceStatus(String resourceType, Long resourceId, String resourceName,
                                      String fromStatus, String toStatus, Long operatorId,
                                      Long bookingId, String reason) {
        // 更新 service_item.resource_status（助教和房间都存在 service_item 表或通过 sys_user 管理）
        if ("room".equals(resourceType)) {
            ServiceItem update = new ServiceItem();
            update.setId(resourceId);
            update.setResourceStatus(toStatus);
            itemMapper.updateById(update);
        }
        // 助教状态也记录在 service_item 表中（如果有对应记录），否则仅写日志
        // ★V1 简化：助教状态统一通过 resource_status_log 追踪

        // 写日志
        ResourceStatusLog log = new ResourceStatusLog();
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResourceName(resourceName);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setOperateTime(new Date());
        log.setReason(reason);
        log.setBookingId(bookingId);
        logMapper.insert(log);
    }

    /**
     * 获取资源当前状态（房间读 service_item.resource_status；助教读最新 resource_status_log）
     */
    private String getCurrentResourceStatus(String resourceType, Long resourceId) {
        return batchCurrentStatus(resourceType, Collections.singletonList(resourceId))
            .getOrDefault(resourceId, STATUS_IDLE);
    }

    /**
     * 批量获取资源当前状态
     * <p>房间：读 service_item.resource_status；助教：读最新一条 resource_status_log.to_status（无记录=idle）。
     */
    private Map<Long, String> batchCurrentStatus(String resourceType, List<Long> resourceIds) {
        Map<Long, String> map = new HashMap<>();
        if (CollUtil.isEmpty(resourceIds)) {
            return map;
        }
        if ("room".equals(resourceType)) {
            List<ServiceItem> items = itemMapper.selectBatchIds(resourceIds);
            for (ServiceItem item : items) {
                map.put(item.getId(), StrUtil.isNotBlank(item.getResourceStatus())
                    ? item.getResourceStatus() : STATUS_IDLE);
            }
        } else {
            List<ResourceStatusLog> logs = logMapper.selectList(
                Wrappers.<ResourceStatusLog>lambdaQuery()
                    .eq(ResourceStatusLog::getResourceType, resourceType)
                    .in(ResourceStatusLog::getResourceId, resourceIds)
                    .orderByDesc(ResourceStatusLog::getOperateTime));
            for (ResourceStatusLog l : logs) {
                map.putIfAbsent(l.getResourceId(), l.getToStatus());
            }
        }
        return map;
    }

    /**
     * 预约前校验资源可约性（仅 idle 可被预约）
     *
     * @param resourceType staff/room
     * @param mainId       主资源ID（可空）
     * @param optionalIds  可选关联资源ID（可空）
     */
    private void assertBookable(String resourceType, Long mainId, List<Long> optionalIds) {
        Set<Long> ids = new HashSet<>();
        if (mainId != null) {
            ids.add(mainId);
        }
        if (CollUtil.isNotEmpty(optionalIds)) {
            ids.addAll(optionalIds);
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> statusMap = batchCurrentStatus(resourceType, new ArrayList<>(ids));
        for (Long id : ids) {
            String st = statusMap.getOrDefault(id, STATUS_IDLE);
            if (!STATUS_IDLE.equals(st)) {
                String prefix = "staff".equals(resourceType) ? "助教 " : "房间 ";
                throw new ServiceException(prefix + getResourceName(resourceType, id) + " 当前非空闲，不可预约");
            }
        }
    }

    /**
     * 获取资源名称
     */
    private String getResourceName(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return "未知";
        }
        if ("room".equals(resourceType)) {
            ServiceItem item = itemMapper.selectById(resourceId);
            return item != null ? item.getName() : "已删除";
        } else if ("staff".equals(resourceType)) {
            SysUser user = userMapper.selectUserById(resourceId);
            return user != null ? user.getNickName() : "已删除";
        }
        return "未知";
    }

    /**
     * 获取 space 分类 ID 集合
     */
    private Set<Long> getSpaceCategoryIds() {
        List<ServiceCategory> categories = categoryMapper.selectList(
            Wrappers.<ServiceCategory>lambdaQuery()
                .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE));
        return categories.stream().map(ServiceCategory::getId).collect(Collectors.toSet());
    }

    /**
     * 获取指定日期已预约的资源ID集合（从 booking_order.remark JSON 解析）
     *
     * @param isRoom true=查房间ID，false=查助教ID
     */
    private Set<Long> getBookedResourceIds(LocalDate bookingDate, String timeSlot, boolean isRoom) {
        Date dayStart = Date.from(bookingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(bookingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<BookingOrder> orders = bookingMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .ge(BookingOrder::getBookingTime, dayStart)
                .lt(BookingOrder::getBookingTime, dayEnd)
                .ne(BookingOrder::getOrderStatus, BookingOrderStatus.CANCELLED.getCode()));
        Set<Long> result = new HashSet<>();
        for (BookingOrder order : orders) {
            Map<String, Object> remarkMap = parseRemark(order.getRemark());
            if (remarkMap.isEmpty()) {
                continue;
            }
            // 主资源
            String bookingType = (String) remarkMap.get("bookingType");
            Long mainId = toLong(remarkMap.get("mainResourceId"));
            if (isRoom && "room".equals(bookingType) && mainId != null) {
                result.add(mainId);
            }
            // 可选关联房间
            if (isRoom) {
                List<Long> roomIds = toLongList(remarkMap.get("optionalRoomIds"));
                if (CollUtil.isNotEmpty(roomIds)) {
                    result.addAll(roomIds);
                }
            }
        }
        return result;
    }

    /**
     * ★获取指定日期 Boss 已发布活动中占用的资源ID集合（从 promotion_activity 解析）
     * <p>活动 startTime~endTime 覆盖 bookingDate 当天时，其 selectedRoomIds/selectedStaffIds 视为已占用。
     *
     * @param bookingDate 预约日期
     * @param isRoom      true=查房间ID，false=查助教ID
     */
    private Set<Long> getActivityBookedResourceIds(LocalDate bookingDate, boolean isRoom) {
        Date dayStart = Date.from(bookingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(bookingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        // 查询当天时间窗内有交集的已发布活动（startTime < dayEnd && endTime >= dayStart）
        List<PromotionActivity> activities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getStatus, "PUBLISHED")
                .lt(PromotionActivity::getStartTime, dayEnd)
                .ge(PromotionActivity::getEndTime, dayStart));
        Set<Long> result = new HashSet<>();
        for (PromotionActivity act : activities) {
            if (isRoom) {
                List<Long> roomIds = parseJsonLongList(act.getSelectedRoomIds());
                if (CollUtil.isNotEmpty(roomIds)) {
                    result.addAll(roomIds);
                }
            } else {
                List<Long> staffIds = parseJsonLongList(act.getSelectedStaffIds());
                if (CollUtil.isNotEmpty(staffIds)) {
                    result.addAll(staffIds);
                }
            }
        }
        return result;
    }

    /**
     * 解析 JSON 数组字符串为 Long 列表（如 "[1,2,3]"）
     */
    private List<Long> parseJsonLongList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析活动资源ID JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    // ===== 活动资源释放 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseActivityResources(List<Long> activityIds) {
        if (CollUtil.isEmpty(activityIds)) {
            return 0;
        }
        int released = 0;
        for (Long activityId : activityIds) {
            PromotionActivity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                continue;
            }
            // 释放关联房间
            List<Long> roomIds = parseJsonLongList(activity.getSelectedRoomIds());
            for (Long roomId : roomIds) {
                releaseSingleResource("room", roomId, null, null,
                    "活动结束/取消释放（活动ID=" + activityId + "）");
                released++;
            }
            // 释放关联助教
            List<Long> staffIds = parseJsonLongList(activity.getSelectedStaffIds());
            for (Long staffId : staffIds) {
                releaseSingleResource("staff", staffId, null, null,
                    "活动结束/取消释放（活动ID=" + activityId + "）");
                released++;
            }
        }
        return released;
    }

    // ===== BUG-02 活动资源占用 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int occupyActivityResources(Long activityId) {
        if (activityId == null) {
            return 0;
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return 0;
        }
        int occupied = 0;
        // 占用关联房间/大巴（→ in_use）
        List<Long> roomIds = parseJsonLongList(activity.getSelectedRoomIds());
        for (Long roomId : roomIds) {
            if (occupySingleResource("room", roomId, null, null,
                "活动发布占用（活动ID=" + activityId + "）")) {
                occupied++;
            }
        }
        // 占用关联员工（导游/司机/助教 → working）
        List<Long> staffIds = parseJsonLongList(activity.getSelectedStaffIds());
        for (Long staffId : staffIds) {
            if (occupySingleResource("staff", staffId, null, null,
                "活动发布占用（活动ID=" + activityId + "）")) {
                occupied++;
            }
        }
        return occupied;
    }

    /**
     * ★BUG-02 占用单个资源（idle → in_use/working + 写日志）
     * <p>非 idle 状态视为已被其他预约/活动占用，fail-soft 保留原状不覆盖，不阻断活动发布。
     *
     * @return true=实际变更了状态；false=已非空闲无需变更
     */
    private boolean occupySingleResource(String resourceType, Long resourceId, Long operatorId,
                                         Long bookingId, String reason) {
        String resourceName = getResourceName(resourceType, resourceId);
        String currentStatus = getCurrentResourceStatus(resourceType, resourceId);
        if (!STATUS_IDLE.equals(currentStatus)) {
            return false;
        }
        // 房间/车辆 → in_use；员工（导游/司机/助教）→ working
        String targetStatus = "room".equals(resourceType) ? STATUS_IN_USE : STATUS_WORKING;
        updateResourceStatus(resourceType, resourceId, resourceName,
            currentStatus, targetStatus, operatorId, bookingId, reason);
        return true;
    }

    /**
     * 解析 remark JSON
     */
    private Map<String, Object> parseRemark(String remark) {
        if (StrUtil.isBlank(remark)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(remark, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * ServiceItem → ServiceItemVo
     */
    private ServiceItemVo toVo(ServiceItem item) {
        ServiceItemVo vo = new ServiceItemVo();
        vo.setId(item.getId());
        vo.setTenantId(item.getTenantId());
        vo.setCategoryId(item.getCategoryId());
        vo.setName(item.getName());
        vo.setPrice(item.getPrice());
        vo.setUnit(item.getUnit());
        vo.setPricingType(item.getPricingType());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setDescription(item.getDescription());
        vo.setCapacity(item.getCapacity());
        vo.setSortOrder(item.getSortOrder());
        vo.setStatus(item.getStatus());
        vo.setResourceStatus(item.getResourceStatus());
        vo.setRemark(item.getRemark());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    /**
     * ResourceStatusLog → ResourceStatusLogVo
     */
    private com.ruoyi.system.domain.vo.ResourceStatusLogVo toLogVo(ResourceStatusLog log) {
        com.ruoyi.system.domain.vo.ResourceStatusLogVo vo = new com.ruoyi.system.domain.vo.ResourceStatusLogVo();
        vo.setId(log.getId());
        vo.setTenantId(log.getTenantId());
        vo.setResourceType(log.getResourceType());
        vo.setResourceId(log.getResourceId());
        vo.setResourceName(log.getResourceName());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperateTime(log.getOperateTime());
        vo.setReason(log.getReason());
        vo.setBookingId(log.getBookingId());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    /**
     * 生成订单编号：SB + yyyyMMddHHmmss + 4 位随机（区别于既有 BK 前缀）
     */
    private String genOrderNo() {
        return "SB" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }

    /**
     * 获取顾客手机号
     */
    private String getCustomerPhone(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectUserById(userId);
        return user != null ? user.getPhonenumber() : null;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }

    private List<Long> toLongList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) {
            return ((List<?>) obj).stream()
                .map(this::toLong).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
