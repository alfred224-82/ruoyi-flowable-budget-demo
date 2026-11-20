package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.StaffContactPublic;
import com.ruoyi.system.domain.vo.BookingOrderVo;
import com.ruoyi.system.domain.vo.MyActivityVo;
import com.ruoyi.system.domain.vo.StaffIncomeVo;
import com.ruoyi.system.domain.vo.StaffSettlementVo;
import com.ruoyi.system.service.IActivityLockRequestService;
import com.ruoyi.system.service.IBookingOrderService;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.IStaffContactPublicService;
import com.ruoyi.system.service.IStaffSettlementService;
import com.ruoyi.system.service.IUserRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 业务员小程序Controller（T15.18，client_type=mini，salesman 视图）
 * <p>我的排班（micro:staff:schedule/scheduleDetail）/我的收入（micro:staff:income/incomeWeek/incomeMonth/incomeDetail）
 * /打卡确认完成服务（micro:staff:checkin/checkinSubmit）。
 * <p>★数据限本人：staffUserId 一律强制取当前登录人，业务员看不到任何管理类菜单与他人数据；
 * 收入为「应发」口径（本人已完成订单金额汇总，复用结算 preview），★V1 只记账不打款。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/staff")
public class MicroStaffController extends BaseController {

    private final IBookingOrderService bookingOrderService;
    private final IStaffSettlementService staffSettlementService;
    private final IPromotionActivityService promotionActivityService;
    private final IUserRatingService userRatingService;
    private final IStaffContactPublicService staffContactPublicService;
    private final IActivityLockRequestService activityLockRequestService;

    /**
     * 我的排班（本人被指派、今日及以后待服务）
     */
    @SaCheckPermission("micro:staff:schedule")
    @GetMapping("/schedule")
    public R<List<BookingOrderVo>> schedule() {
        return R.ok(bookingOrderService.mySchedule());
    }

    /**
     * 排班详情（仅限本人被指派的订单）
     */
    @SaCheckPermission("micro:staff:scheduleDetail")
    @GetMapping("/schedule/{id}")
    public R<BookingOrderVo> scheduleDetail(@PathVariable Long id) {
        BookingOrderVo vo = bookingOrderService.queryById(id);
        Long uid = LoginHelper.getUserId();
        if (vo.getStaffUserId() == null || !vo.getStaffUserId().equals(uid)) {
            return R.fail("只能查看本人的排班");
        }
        return R.ok(vo);
    }

    /**
     * 我的收入汇总（本周 + 本月应发单量/金额）
     */
    @SaCheckPermission("micro:staff:income")
    @GetMapping("/income")
    public R<StaffIncomeVo> income() {
        Long me = LoginHelper.getUserId();
        Date now = new Date();
        StaffSettlementVo week = staffSettlementService.preview(me, DateUtil.beginOfWeek(now), DateUtil.endOfWeek(now));
        StaffSettlementVo month = staffSettlementService.preview(me, DateUtil.beginOfMonth(now), DateUtil.endOfMonth(now));
        StaffIncomeVo vo = new StaffIncomeVo();
        vo.setWeekOrders(week.getTotalOrders());
        vo.setWeekAmount(week.getTotalAmount());
        vo.setMonthOrders(month.getTotalOrders());
        vo.setMonthAmount(month.getTotalAmount());
        return R.ok(vo);
    }

    /**
     * 本周收入（应发单量/金额）
     */
    @SaCheckPermission("micro:staff:incomeWeek")
    @GetMapping("/income/week")
    public R<StaffSettlementVo> incomeWeek() {
        Date now = new Date();
        return R.ok(staffSettlementService.preview(LoginHelper.getUserId(),
            DateUtil.beginOfWeek(now), DateUtil.endOfWeek(now)));
    }

    /**
     * 本月收入（应发单量/金额）
     */
    @SaCheckPermission("micro:staff:incomeMonth")
    @GetMapping("/income/month")
    public R<StaffSettlementVo> incomeMonth() {
        Date now = new Date();
        return R.ok(staffSettlementService.preview(LoginHelper.getUserId(),
            DateUtil.beginOfMonth(now), DateUtil.endOfMonth(now)));
    }

    /**
     * 每单明细（本人已完成订单，含服务项目/金额/完成时间）
     */
    @SaCheckPermission("micro:staff:incomeDetail")
    @GetMapping("/income/detail")
    public R<List<BookingOrderVo>> incomeDetail() {
        return R.ok(bookingOrderService.myCompletedOrders());
    }

    /**
     * 打卡入口（本人今日及待服务排班，用于确认完成服务）
     */
    @SaCheckPermission("micro:staff:checkin")
    @GetMapping("/checkin")
    public R<List<BookingOrderVo>> checkin() {
        return R.ok(bookingOrderService.mySchedule());
    }

    /**
     * 确认完成服务（打卡提交）：仅限本人被指派订单，order_status→COMPLETED + complete_time
     */
    @SaCheckPermission("micro:staff:checkinSubmit")
    @Log(title = "业务员确认完成服务", businessType = BusinessType.UPDATE)
    @PostMapping("/checkinSubmit/{id}")
    public R<Void> checkinSubmit(@PathVariable Long id) {
        return toAjax(bookingOrderService.completeByStaff(id));
    }

    /**
     * ★我的活动（M22，合并服务预约 + 被选中的活动）
     * @param status 状态过滤：空=全部，ONGOING=进行中，ENDED=已结束
     */
    @SaCheckPermission("micro:staff:schedule")
    @GetMapping("/myActivities")
    public R<List<MyActivityVo>> myActivities(
            @RequestParam(required = false) String status) {
        return R.ok(promotionActivityService.listStaffActivities(status));
    }

    /**
     * ★我的总点赞数（员工首页「点赞数」指标：本人被点赞跨场景汇总，替代原平均评分）。
     * <p>★登录态即可：数据仅聚合当前登录人，无跨用户泄露；FEAT_RATING 版本门槛。
     */
    @RequireFeature("FEAT_RATING")
    @GetMapping("/myLikeCount")
    public R<Long> myLikeCount() {
        return R.ok(userRatingService.countLikes(LoginHelper.getUserId(), null));
    }

    // ==================== T31.5 员工联系方式公开开关（§八之二） ====================

    /**
     * ★M34 T34.5 列出本租户全部业务员及其联系方式公开开关状态（老板 PC 端配置页）。
     * <p>★默认全关：无记录的员工返回 showPhone=0 / showWechat=0。
     * <p>路径为字面量 /list，优先于 /contactPublic/{staffId} 模板匹配。
     */
    @SaCheckPermission("micro:staff:manage")
    @GetMapping("/contactPublic/list")
    public R<List<java.util.Map<String, Object>>> listContactPublic() {
        return R.ok(staffContactPublicService.listAllWithStaff());
    }

    /**
     * 查询员工联系方式公开设置（顾客端 H5 弹名片 / 老板 PC 端配置页 / 员工自查）。
     * <p>★默认全关：无记录时返回 showPhone=0, showWechat=0 的默认对象，不落库。
     * <p>★顾客端仅需登录（@SaCheckLogin），不要求 micro:staff:manage 权限，
     *   否则顾客点头像弹名片时因无权限拉取失败，fail-safe 默认全关导致永远显示「未公开」。
     *
     * @param staffId 员工用户ID
     */
    @SaCheckLogin
    @GetMapping("/contactPublic/{staffId}")
    public R<StaffContactPublic> getContactPublic(@PathVariable Long staffId) {
        return R.ok(staffContactPublicService.getByStaffId(staffId));
    }

    /**
     * 更新员工联系方式公开开关（不存在则新建，UNIQUE(tenant_id, staff_id)）。
     * <p>★租户隔离由 MyBatis-Plus 多租户插件自动注入 tenant_id；
     *   前端仅需传 staffId / showPhone / showWechat / wechatQrcodeUrl。
     */
    @SaCheckPermission("micro:staff:manage")
    @Log(title = "员工联系方式公开开关", businessType = BusinessType.UPDATE)
    @PutMapping("/contactPublic")
    public R<Void> updateContactPublic(@RequestBody StaffContactPublic body) {
        return toAjax(staffContactPublicService.saveOrUpdate(body));
    }

    // ==================== M33 §九 员工申请锁定活动 ====================

    /**
     * ★M33 §九 员工「申请锁定」活动：全员确认后提交，进入老板待审批（员工无直接锁定权限）。
     * <p>老板审批通过后由 {@code ActivityLockRequestServiceImpl#approve} 委派 lockActivity 真正锁定。
     * <p>★仅需登录（申请人限本租户员工，Service 层校验活动归属/未锁定/无重复待审批）；
     *   FEAT_ACTIVITY_LOCK 版本门槛与锁定/解锁能力一致。
     *
     * @param body 请求体：activityId（必填）+ reason（可选申请说明）
     * @return 新建锁定申请记录ID
     */
    @SaCheckLogin
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @Log(title = "员工申请锁定活动", businessType = BusinessType.INSERT)
    @PostMapping("/lockApply")
    public R<Long> lockApply(@RequestBody Map<String, Object> body) {
        Object activityIdObj = body == null ? null : body.get("activityId");
        if (activityIdObj == null) {
            return R.fail("活动ID不能为空");
        }
        Long activityId = Long.valueOf(String.valueOf(activityIdObj));
        Object reasonObj = body.get("reason");
        String reason = reasonObj == null ? null : String.valueOf(reasonObj);
        return R.ok(activityLockRequestService.apply(activityId, reason));
    }
}
