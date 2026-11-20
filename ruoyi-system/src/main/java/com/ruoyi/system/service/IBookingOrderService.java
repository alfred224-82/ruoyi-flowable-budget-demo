package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.BookingOrderBo;
import com.ruoyi.system.domain.vo.BookingOrderVo;
import com.ruoyi.system.domain.vo.MicroRecallCustomerVo;

import java.util.List;

/**
 * 预约订单服务接口（T15.5/T15.12，小微版）
 * <p>老板 PC：预约列表/新建（帮顾客约）/编辑/取消/完成服务/标记已收款/详情；
 * 顾客小程序：提交预约（生成「待到店付」订单）/我的预约/取消预约。
 * <p>★V1 零支付合规：无在线支付；标记「已收款」仅改状态 + 自动写 cash_flow 收入流水，不调支付接口。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IBookingOrderService {

    /**
     * 分页查询预约订单（老板 PC，回填 customerName/staffName）
     */
    TableDataInfo<BookingOrderVo> queryPageList(BookingOrderBo bo, PageQuery pageQuery);

    /**
     * 预约订单详情
     */
    BookingOrderVo queryById(Long id);

    /**
     * 老板 PC 帮顾客新建预约（amount 为空则按服务项目单价×数量计算，快照 serviceName，生成 orderNo）
     *
     * @return 订单ID
     */
    Long insert(BookingOrderBo bo);

    /**
     * 顾客小程序提交预约（customerUserId 取当前登录人，生成「待到店付」订单）
     *
     * @return 订单ID
     */
    Long submitByCustomer(BookingOrderBo bo);

    /**
     * 老板 PC 编辑预约
     */
    int update(BookingOrderBo bo);

    /**
     * 取消预约（老板 PC / 顾客本人）：order_status=CANCELLED
     */
    int cancel(Long id);

    /**
     * 完成服务：order_status=COMPLETED，complete_time=now
     */
    int complete(Long id);

    /**
     * ★老板标记「已收款」：pay_status=RECEIVED，pay_time=now，并自动写 cash_flow 收入流水（不调支付接口）
     */
    int markReceived(Long id);

    /**
     * 顾客小程序「我的预约」（customerUserId=当前登录人）
     */
    List<BookingOrderVo> myBookings();

    /**
     * ★预约过期自动关单（T15.22 bookingExpireJob）：将「待服务」且预约到店时间已过的订单批量置为「已取消」。
     * <p>租户上下文由 {@code @TenantJob} 切面设置，多租户插件自动隔离，仅作用于当前租户。
     *
     * @return 关单条数
     */
    int expireOverdueBookings();

    /**
     * ★识别「待召回」顾客（T15.22 customerTagJob）：最近到店时间距今超过 {@code inactiveDays} 天的顾客清单。
     * <p>到店口径：已完成/已收款订单，取完成时间（回落收款时间）的最大值为最近到店时间。
     * <p>租户上下文由 {@code @TenantJob} 切面设置，多租户插件自动隔离，仅作用于当前租户。
     *
     * @param inactiveDays 未到店天数阈值（&lt;=0 时按默认 7 天）
     * @return 待召回顾客清单（回填昵称）
     */
    List<MicroRecallCustomerVo> listRecallCustomers(int inactiveDays);

    /**
     * ★业务员「我的排班」（T15.18，micro:staff:schedule）：本人被指派、待服务且预约时间自今日起的订单，按预约时间升序。
     * <p>staffUserId 强制取当前登录人，仅见本人排班。
     */
    List<BookingOrderVo> mySchedule();

    /**
     * ★业务员「每单明细」（T15.18，micro:staff:incomeDetail）：本人被指派且已完成(COMPLETED)的订单，按完成时间倒序。
     * <p>staffUserId 强制取当前登录人，仅见本人收入明细。
     */
    List<BookingOrderVo> myCompletedOrders();

    /**
     * ★业务员「打卡/确认完成服务」（T15.18，micro:staff:checkinSubmit）：仅可确认本人被指派的订单，
     * order_status=COMPLETED + complete_time=now；已完成幂等返回 0，已取消拒绝。
     */
    int completeByStaff(Long id);
}
