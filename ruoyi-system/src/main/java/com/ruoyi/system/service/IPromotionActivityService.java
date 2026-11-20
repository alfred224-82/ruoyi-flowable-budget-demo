package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.PromotionActivityBo;
import com.ruoyi.system.domain.vo.MyActivityVo;
import com.ruoyi.system.domain.vo.PromotionActivityVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 活动/预告/海报服务接口（T15.28，小微版 lite 档活动浏览页数据源）
 * <p>老板 PC「推广发布」(micro:promo:*) 维护活动；lite 档顾客/助教「活动浏览页」(micro:activity:browse)
 * 按 tenant_id 只读本店 status=PUBLISHED 的活动（★无需登录可浏览）。
 * <p>★浏览返回体不含任何预约/支付/打卡动作字段（纯只读数据源；pro 档在同源数据上扩展预约动作）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
public interface IPromotionActivityService {

    /**
     * 分页查询本店活动（老板 PC 管理，支持按标题/类型/状态过滤）
     */
    TableDataInfo<PromotionActivityVo> queryPageList(PromotionActivityBo bo, PageQuery pageQuery);

    /**
     * 查询活动详情
     */
    PromotionActivityVo queryById(Long id);

    /**
     * ★T15.28 活动浏览页数据源：按指定租户只读返回 status=PUBLISHED 且未过期的公开活动。
     * <p>匿名可访问（走 security.excludes 白名单）；显式限定 tenantId 保证跨租户隔离，仅返回已发布活动。
     *
     * @param tenantId     目标租户ID（请求参数传入，非登录上下文）
     * @param activityType 可选类型过滤（activity/notice/poster）
     * @param title        可选标题模糊搜索
     * @param beginTime    可选开始时间下界（yyyy-MM-dd，按活动 startTime 过滤）
     * @param endTime      可选开始时间上界（yyyy-MM-dd，按活动 startTime 过滤）
     * @return 已发布活动清单（不含任何预约/支付/打卡动作字段）
     */
    List<PromotionActivityVo> listPublished(Long tenantId, String activityType, String title, String beginTime, String endTime);

    /**
     * ★历史活动分页查询（boss 小程序端「我的待处理 → 历史活动」）：
     * 返回已取消（DRAFT 下架）+ 已完成（ENDED，含 endTime 已过的 PUBLISHED）活动，支持按名称/时间范围搜索。
     * <p>租户上下文由登录态 + 多租户插件自动隔离。
     *
     * @param title     可选标题模糊搜索
     * @param beginTime 可选开始时间下界（yyyy-MM-dd，按活动 startTime 过滤）
     * @param endTime   可选开始时间上界（yyyy-MM-dd，按活动 startTime 过滤）
     */
    TableDataInfo<PromotionActivityVo> queryHistoryPage(String title, String beginTime, String endTime, PageQuery pageQuery);

    /**
     * 新建活动（默认 status=DRAFT 草稿）
     *
     * @return 活动ID
     */
    Long insert(PromotionActivityBo bo);

    /**
     * 编辑活动
     */
    int update(PromotionActivityBo bo);

    /**
     * 发布/下架活动（status：PUBLISHED 发布并记 publish_time / DRAFT 下架 / ENDED 结束）
     */
    int changeStatus(Long id, String status);

    /**
     * 逻辑删除活动（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * ★活动自动结束（T15.28，定时任务调用）：将 endTime 已过且仍为 PUBLISHED 的活动批量置为 ENDED。
     * <p>租户上下文由 {@code @TenantJob} 切面设置，多租户插件自动隔离，仅作用于当前租户。
     *
     * @return 本次新结束的活动ID列表（供调用方释放关联资源）
     */
    List<Long> autoEndActivities();

    /**
     * ★小程序「活动详情（报名活动）」聚合（只读，登录态 boss/顾客/业务员共用）：
     * 主持（活动选定员工）+ 报名名单（关联服务项目 project_signup）+ 已报名人数/名额上限/剩余空位/候补数，
     * 并扩展指定房间名（selected_room_ids → service_item）、当前登录用户报名状态、可取消标志。
     * <p>名额取 service_item.extra_config.quota；已报名人数=非候补报名 people_count 累计。
     *
     * @param activityId 活动ID
     * @return {hosts, members, signedCount, waitlistCount, quota, remaining, roomNames, projectId, mySigned, myWaitlist, canCancel}
     */
    Map<String, Object> getSignupDetail(Long activityId);

    /**
     * ★顾客端活动自助报名（小程序活动详情页「确认报名」）：
     * 校验活动发布状态/未过期/已关联报名容器/防重复报名后，委派 {@link IProjectSignupService#signup} 登记（满员进候补）。
     * <p>报名人取当前登录用户，人数固定 1。
     *
     * @param activityId 活动ID
     * @return {signupId, waitlist, quota, message}
     */
    Map<String, Object> customerSignup(Long activityId);

    /**
     * ★顾客端取消报名（活动开始前 1 小时可取消）
     *
     * @param signupId   报名记录ID
     * @param activityId 活动ID
     */
    void cancelSignup(Long signupId, Long activityId);

    /**
     * ★锁定/解锁活动（M22，锁定后不可报名/取消）
     *
     * @param activityId 活动ID
     * @param locked     true=锁定，false=解锁
     * @deprecated M28 T28.1 起改用 {@link #lockActivity(Long)} / {@link #unlockActivity(Long)}（带操作日志切面）；
     *             保留本方法仅为兼容旧端点 {@code /micro/promo/lock/{id}/{locked}}。
     */
    @Deprecated
    void lockActivity(Long activityId, boolean locked);

    /**
     * ★锁定活动（M28 T28.1）：is_locked=1 + activity_seat_assignment.locked 全量刷 1，座位变只读。
     * <p>操作日志 LOCK 由 {@code @SeatOperation} 切面自动记录（含锁定前后座位快照）。
     * <p>★运行时锁定以 is_locked 为权威标志，status 维持 PUBLISHED（避免顾客浏览页丢失已锁定活动）。
     *
     * @param activityId 活动ID
     */
    void lockActivity(Long activityId);

    /**
     * ★解锁活动（M28 T28.1）：is_locked=0 + activity_seat_assignment.locked 全量刷 0，座位可再次编排。
     * <p>解锁二次确认由前端弹窗完成，后端仅记录操作日志 UNLOCK（由 {@code @SeatOperation} 切面自动记录）。
     *
     * @param activityId 活动ID
     */
    void unlockActivity(Long activityId);

    /**
     * ★顾客「我的活动」合并列表（M22）：自助预约订单 + 报名参与的活动
     * @param status 状态过滤：null/空=全部，ONGOING=进行中，ENDED=已结束
     */
    List<MyActivityVo> listCustomerActivities(String status);

    /**
     * ★Boss「我的活动」：返回本店全部活动（支持状态过滤）
     * @param status 状态过滤：null/空=全部，ONGOING=进行中，ENDED=已结束
     */
    List<MyActivityVo> listBossActivities(String status);

    /**
     * ★助教「我的活动」合并列表（M22）：作为服务员工的预约 + 被选中的活动
     * @param status 状态过滤：null/空=全部，ONGOING=进行中，ENDED=已结束
     */
    List<MyActivityVo> listStaffActivities(String status);
}
