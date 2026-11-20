package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PromotionActivityBo;
import com.ruoyi.system.domain.vo.MyActivityVo;
import com.ruoyi.system.domain.vo.PromotionActivityVo;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.IUserRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动浏览页Controller（T15.28，小微版 lite 档唯一对客只读页）
 * <p>★无需登录可浏览：本接口 {@code /micro/activity/browse} 已登记 application.yml security.excludes 白名单，
 * 匿名放行；不签发会话、不写 TenantContext，改由 {@code tenantId} 请求参数 + TenantHelper.supply 显式限定门店。
 * <p>★lite 档浏览页仅返回本店 status=PUBLISHED 的公开活动，返回体不含任何预约/支付/打卡动作字段（纯只读数据源）。
 * pro 档顾客/助教在同源数据上由各自小程序页扩展「预约」动作，本接口保持只读语义不变。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/activity")
public class MicroActivityBrowseController extends BaseController {

    private final IPromotionActivityService promotionActivityService;
    private final IUserRatingService userRatingService;

    /** 点赞场景：顾客在活动详情页点赞本场主持人 */
    private static final String SCENE_ACTIVITY_HOST = "activity_host";

    /**
     * 活动浏览页数据源（匿名可访问）：按 tenantId 只读返回本店已发布且未过期的活动。
     *
     * @param tenantId     目标门店（租户）ID，必填——匿名场景无登录上下文，须显式指定
     * @param activityType 可选类型过滤：activity(活动)/notice(预告)/poster(海报)
     * @param title        可选标题模糊搜索
     * @param beginTime    可选开始时间下界（yyyy-MM-dd）
     * @param endTime      可选开始时间上界（yyyy-MM-dd）
     */
    @GetMapping("/browse")
    public R<List<PromotionActivityVo>> browse(@RequestParam Long tenantId,
                                               @RequestParam(required = false) String activityType,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) String beginTime,
                                               @RequestParam(required = false) String endTime) {
        return R.ok(promotionActivityService.listPublished(tenantId, activityType, title, beginTime, endTime));
    }

    /**
     * 历史活动列表（boss 小程序端「我的待处理 → 历史活动」）：返回已取消(DRAFT)+已完成(ENDED)活动，支持名称/时间搜索分页。
     * <p>★需登录 + micro:promo:list 权限；租户隔离由登录上下文 + 多租户插件自动完成。
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/history")
    public TableDataInfo<PromotionActivityVo> history(@RequestParam(required = false) String title,
                                                      @RequestParam(required = false) String beginTime,
                                                      @RequestParam(required = false) String endTime,
                                                      PageQuery pageQuery) {
        return promotionActivityService.queryHistoryPage(title, beginTime, endTime, pageQuery);
    }

    /**
     * 活动情况统计（boss 小程序端）：返回当前租户活动列表（含浏览量/参与人数/分享次数）。
     * <p>★需登录 + micro:promo:list 权限。
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/stats")
    public R<List<PromotionActivityVo>> stats(PromotionActivityBo bo, PageQuery pageQuery) {
        TableDataInfo<PromotionActivityVo> page = promotionActivityService.queryPageList(bo, pageQuery);
        return R.ok(page.getRows());
    }

    /**
     * ★小程序「活动详情（报名活动）」聚合（主持/名单/人数/名额/房间/成团目标/我的报名状态）。
     * <p>★登录态即可访问（boss/顾客/业务员共用详情页）；匿名浏览页不开详情聚合，租户隔离由登录上下文完成。
     */
    @GetMapping("/signupDetail")
    public R<Map<String, Object>> signupDetail(@RequestParam Long activityId) {
        return R.ok(promotionActivityService.getSignupDetail(activityId));
    }

    /**
     * ★顾客端活动自助报名（活动详情页「确认报名」）：登录态 + 防重复报名，满员进候补。
     */
    @PostMapping("/signup/{activityId}")
    public R<Map<String, Object>> signup(@PathVariable Long activityId) {
        return R.ok(promotionActivityService.customerSignup(activityId));
    }

    /**
     * ★Boss「我的活动」：返回本店全部活动（支持状态过滤）
     * @param status 状态过滤：空=全部，ONGOING=进行中，ENDED=已结束
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/bossActivities")
    public R<List<MyActivityVo>> bossActivities(
            @RequestParam(required = false) String status) {
        return R.ok(promotionActivityService.listBossActivities(status));
    }

    /**
     * ★顾客端取消报名（活动开始前 1 小时可取消）
     */
    @PostMapping("/cancelSignup")
    public R<Void> cancelSignup(@RequestParam Long signupId, @RequestParam Long activityId) {
        promotionActivityService.cancelSignup(signupId, activityId);
        return R.ok();
    }

    // ============================================================
    // ★顾客点赞主持人（user_like toggle，scene=activity_host，M17 点赞体系顾客侧入口）
    // ============================================================

    /**
     * ★顾客点赞/取消点赞本场主持人（toggle：已赞→取消、未赞→点赞，每人仅一次由 toggle 天然防重）。
     * <p>★登录态即可（与 signupDetail/signup 同口径，顾客详情页共用）；FEAT_RATING 版本门槛；
     *   禁止自赞与租户隔离由 Service 层保证（user_like 为租户表）。
     *
     * @return true=已点赞 false=已取消
     */
    @RequireFeature("FEAT_RATING")
    @Log(title = "顾客点赞主持人", businessType = BusinessType.UPDATE)
    @PostMapping("/hostLike/toggle")
    public R<Boolean> hostLikeToggle(@RequestParam Long hostUserId) {
        return R.ok(userRatingService.toggleLike(hostUserId, SCENE_ACTIVITY_HOST));
    }

    /**
     * ★主持人点赞状态（顾客详情页红心渲染）：liked=当前顾客是否已赞，likeCount=该主持人总点赞数。
     */
    @RequireFeature("FEAT_RATING")
    @GetMapping("/hostLike/status")
    public R<Map<String, Object>> hostLikeStatus(@RequestParam Long hostUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", userRatingService.isLiked(hostUserId, SCENE_ACTIVITY_HOST));
        result.put("likeCount", userRatingService.countLikes(hostUserId, null));
        return R.ok(result);
    }

    // ============================================================
    // M28 T28.1 活动锁定 / 解锁（Boss PC + 小程序编排入口）
    // ============================================================

    /**
     * ★锁定活动（M28 T28.1）：锁定后 is_locked=1，activity_seat_assignment.locked 全量刷 1，
     * 顾客端选座/移座接口拒绝，Boss 编排接口拒绝，座位变只读。
     * <p>★需 {@code micro:activity:lock} 权限 + FEAT_ACTIVITY_LOCK 功能开关；
     *   操作日志 LOCK 由 {@code @SeatOperation} 切面在 Service 层自动记录（含锁定前后座位快照）。
     * <p>★运行时锁定以 is_locked 为权威标志，status 维持 PUBLISHED（避免顾客浏览页丢失已锁定活动）。
     */
    @SaCheckPermission("micro:activity:lock")
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @Log(title = "活动锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/lock")
    public R<Void> lock(@PathVariable Long id) {
        promotionActivityService.lockActivity(id);
        return R.ok();
    }

    /**
     * ★解锁活动（M28 T28.1）：解锁后 is_locked=0，activity_seat_assignment.locked 全量刷 0，座位可再次编排。
     * <p>★需 {@code micro:activity:unlock} 权限 + FEAT_ACTIVITY_LOCK 功能开关；
     *   解锁二次确认由前端弹窗完成（「解锁后座位可调整，确定解锁吗？」），后端仅记录操作日志 UNLOCK（切面自动）。
     */
    @SaCheckPermission("micro:activity:unlock")
    @RequireFeature("FEAT_ACTIVITY_LOCK")
    @Log(title = "活动解锁", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/unlock")
    public R<Void> unlock(@PathVariable Long id) {
        promotionActivityService.unlockActivity(id);
        return R.ok();
    }
}
