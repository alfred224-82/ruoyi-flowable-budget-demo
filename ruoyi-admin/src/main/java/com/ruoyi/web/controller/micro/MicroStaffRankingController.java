package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.UserRating;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.UserRatingMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 老板员工业绩榜Controller（T18.13，小微版 M18）
 * <p>「员工业绩榜」菜单（micro/staffRanking）：
 * <ul>
 *   <li>员工综合业绩排行（接单数/营收/评分）</li>
 *   <li>支持按周期筛选（本周/本月/自定义）</li>
 * </ul>
 * <p>★仅 micro_pro 可用。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/staffRanking")
@RequireFeature("FEAT_RATING")
public class MicroStaffRankingController extends BaseController {

    private final BookingOrderMapper bookingOrderMapper;
    private final UserRatingMapper userRatingMapper;
    private final SysUserMapper sysUserMapper;

    /** ★合法周期类型白名单 */
    private static final Set<String> VALID_PERIODS = Set.of("week", "month", "custom");

    /**
     * 员工业绩排行列表
     *
     * @param period    周期类型：week(本周)/month(本月)/custom(自定义)
     * @param startDate 自定义开始日期（period=custom时必填）
     * @param endDate   自定义结束日期（period=custom时必填）
     * @param topN      返回前N名（默认20）
     */
    @SaCheckPermission("micro:staffRanking:list")
    @GetMapping
    public R<List<StaffRankingItem>> getRanking(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "20") int topN) {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // ★值域校验
        if (!VALID_PERIODS.contains(period)) {
            throw new ServiceException("周期类型不合法，可选：week/month/custom");
        }
        if (topN <= 0 || topN > 100) {
            throw new ServiceException("排行数量须在1~100之间");
        }

        // ★计算时间范围
        Date[] range = resolveDateRange(period, startDate, endDate);
        Date begin = range[0];
        Date end = range[1];

        // ① 查询时间范围内已完成订单（按员工分组）
        List<BookingOrder> orders = bookingOrderMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .eq(BookingOrder::getOrderStatus, "COMPLETED")
                .ge(BookingOrder::getBookingTime, begin)
                .le(BookingOrder::getBookingTime, end)
                .isNotNull(BookingOrder::getStaffUserId));
        if (orders == null) {
            orders = Collections.emptyList();
        }

        // ② 按 staffUserId 聚合：接单数 + 营收
        Map<Long, List<BookingOrder>> orderGrouped = orders.stream()
            .filter(o -> o.getStaffUserId() != null)
            .collect(Collectors.groupingBy(BookingOrder::getStaffUserId));

        // ③ 查询时间范围内评价（NORMAL 状态，按被评价人分组）
        List<UserRating> ratings = userRatingMapper.selectList(
            Wrappers.<UserRating>lambdaQuery()
                .eq(UserRating::getStatus, "NORMAL")
                .ge(UserRating::getCreateTime, begin)
                .le(UserRating::getCreateTime, end));
        if (ratings == null) {
            ratings = Collections.emptyList();
        }
        Map<Long, List<UserRating>> ratingGrouped = ratings.stream()
            .collect(Collectors.groupingBy(UserRating::getTargetUserId));

        // ④ 收集所有出现过的员工ID
        Set<Long> allStaffIds = new HashSet<>();
        allStaffIds.addAll(orderGrouped.keySet());
        allStaffIds.addAll(ratingGrouped.keySet());

        if (allStaffIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }

        // ⑤ 批量查询员工姓名
        List<SysUser> users = sysUserMapper.selectList(
            Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getUserId, allStaffIds));
        Map<Long, String> userNameMap = new HashMap<>();
        if (users != null) {
            for (SysUser u : users) {
                userNameMap.put(u.getUserId(), u.getNickName());
            }
        }

        // ⑥ 组装排行数据
        List<StaffRankingItem> ranking = new ArrayList<>();
        for (Long staffId : allStaffIds) {
            StaffRankingItem item = new StaffRankingItem();
            item.setStaffUserId(staffId);
            item.setStaffName(userNameMap.getOrDefault(staffId, "未知员工"));

            // 接单数 + 营收
            List<BookingOrder> staffOrders = orderGrouped.getOrDefault(staffId, Collections.emptyList());
            item.setOrderCount(staffOrders.size());
            BigDecimal totalRevenue = staffOrders.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
            item.setTotalRevenue(totalRevenue);

            // 评分
            List<UserRating> staffRatings = ratingGrouped.getOrDefault(staffId, Collections.emptyList());
            if (staffRatings.isEmpty()) {
                item.setAvgRating(0.0);
                item.setRatingCount(0);
            } else {
                double avg = staffRatings.stream()
                    .mapToInt(UserRating::getRating)
                    .average()
                    .orElse(0.0);
                item.setAvgRating(Math.round(avg * 10.0) / 10.0);
                item.setRatingCount(staffRatings.size());
            }

            // ★综合得分 = 接单数*10 + 营收*0.1 + 平均评分*20（用于排序）
            double score = item.getOrderCount() * 10.0
                + item.getTotalRevenue().doubleValue() * 0.1
                + item.getAvgRating() * 20.0;
            item.setCompositeScore(Math.round(score * 10.0) / 10.0);

            ranking.add(item);
        }

        // ⑦ 按综合得分降序排列，取 topN
        ranking.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));
        List<StaffRankingItem> result = ranking.subList(0, Math.min(topN, ranking.size()));

        // ⑧ 填充排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return R.ok(result);
    }

    /**
     * ★解析时间范围
     */
    private Date[] resolveDateRange(String period, Date startDate, Date endDate) {
        Calendar cal = Calendar.getInstance();
        Date begin;
        Date end = new Date();

        switch (period) {
            case "week":
                // 本周（周一开始）
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                begin = cal.getTime();
                break;
            case "month":
                // 本月
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                begin = cal.getTime();
                break;
            case "custom":
                // 自定义
                if (startDate == null || endDate == null) {
                    throw new ServiceException("自定义周期须传入 startDate 和 endDate");
                }
                begin = startDate;
                end = endDate;
                break;
            default:
                throw new ServiceException("周期类型不合法：" + period);
        }
        return new Date[]{begin, end};
    }

    // ===== 内部 VO =====

    @Data
    public static class StaffRankingItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 排名 */
        private Integer rank;
        /** 员工用户ID */
        private Long staffUserId;
        /** 员工姓名 */
        private String staffName;
        /** 接单数 */
        private Integer orderCount;
        /** 营收总额 */
        private BigDecimal totalRevenue;
        /** 平均评分 */
        private Double avgRating;
        /** 评价总数 */
        private Integer ratingCount;
        /** 综合得分 */
        private Double compositeScore;
    }
}
