package com.ruoyi.job.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.TenantJob;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.system.domain.ProjectSignup;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.vo.MicroRecallCustomerVo;
import com.ruoyi.system.mapper.ProjectSignupMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.service.IBookingOrderService;
import com.ruoyi.system.service.ICustomerCreditService;
import com.ruoyi.system.service.IMicroReportService;
import com.ruoyi.system.service.IMicroSelfBookingService;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.ISysMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小微版业务定时任务（T15.22）
 * <p>三个 XXL-JOB Handler 均标注 {@link TenantJob}{@code (versions={"micro"})}，由 {@code TenantJobAspect}
 * 逐个 edition=MICRO 的活跃租户执行、单租户异常隔离、租户ID 注入 MDC；XXL-JOB 调度中心各配一条任务，
 * executor_param 留空（租户列表由切面自查），job_group=1（见 M15_08_xxl_job_micro_tasks.sql）。
 * <p>★本类不实现任何接口 → Spring 采用 CGLIB 代理；xxl-job 经 {@code MethodIntrospector.selectMethods}
 * 解包代理发现 {@code @XxlJob} 方法并以代理 bean 为 target 注册，{@code Method.invoke} 虚分派触发切面（与
 * {@code @Transactional} 同理），故 {@code @XxlJob} + {@code @TenantJob} 同方法可协同生效。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MicroBizJob {

    /** customerTagJob 未到店天数阈值（7 天未到店 → 待召回） */
    private static final int RECALL_INACTIVE_DAYS = 7;

    private final IMicroReportService microReportService;
    private final IBookingOrderService bookingOrderService;
    private final ICustomerCreditService customerCreditService;
    private final IPromotionActivityService promotionActivityService;
    private final IMicroSelfBookingService selfBookingService;
    private final ISysMessageService sysMessageService;
    private final PromotionActivityMapper activityMapper;
    private final ProjectSignupMapper signupMapper;

    /**
     * 每日营收快照（Cron {@code 0 0 0 * * ?}）：每日 00:00 汇总「上一自然日」（此时当日已完结，快照完整），
     * 写 micro_daily_revenue；uk_tenant_date 幂等 upsert，重复执行安全。
     */
    @XxlJob("microDailyRevenueJob")
    @TenantJob(versions = {"micro"})
    public void microDailyRevenueJob() {
        Long tenantId = TenantHelper.getTenantId();
        Date yesterday = DateUtil.yesterday();
        int rows = microReportService.generateDailyRevenue(tenantId, yesterday);
        XxlJobHelper.log("[microDailyRevenueJob] 租户 {} 生成 {} 营收快照，影响 {} 条",
            tenantId, DateUtil.formatDate(yesterday), rows);
        log.info("[microDailyRevenueJob] 租户 {} 生成 {} 营收快照，影响 {} 条",
            tenantId, DateUtil.formatDate(yesterday), rows);
    }

    /**
     * 预约过期自动关单（Cron {@code 0 *&#47;30 * * * ?}）：「待服务」且预约到店时间已过的订单批量置为「已取消」。
     * <p>★零支付合规：仅关单不涉退款（顾客未到店、无收款流水）。
     */
    @XxlJob("bookingExpireJob")
    @TenantJob(versions = {"micro"})
    public void bookingExpireJob() {
        Long tenantId = TenantHelper.getTenantId();
        int rows = bookingOrderService.expireOverdueBookings();
        XxlJobHelper.log("[bookingExpireJob] 租户 {} 预约过期自动关单 {} 条", tenantId, rows);
        log.info("[bookingExpireJob] 租户 {} 预约过期自动关单 {} 条", tenantId, rows);
    }

    /**
     * 顾客「待召回」识别（Cron {@code 0 0 1 * * ?}）：最近到店距今超过 7 天的顾客打「待召回」标签。
     * <p>★delivered M15 schema 无顾客标签表/列，故「打标签」落地为识别 + 输出待召回清单到 XXL-JOB 日志
     * （租户ID 已由切面注入 MDC，日志可按租户检索），不臆造持久化表。
     */
    @XxlJob("customerTagJob")
    @TenantJob(versions = {"micro"})
    public void customerTagJob() {
        Long tenantId = TenantHelper.getTenantId();
        List<MicroRecallCustomerVo> recall = bookingOrderService.listRecallCustomers(RECALL_INACTIVE_DAYS);
        XxlJobHelper.log("[customerTagJob] 租户 {} 识别「待召回」顾客 {} 人（{} 天未到店）",
            tenantId, recall.size(), RECALL_INACTIVE_DAYS);
        for (MicroRecallCustomerVo vo : recall) {
            XxlJobHelper.log("待召回顾客：{} {} 最近到店 {}，已 {} 天未到店",
                vo.getCustomerName() != null ? vo.getCustomerName() : "-",
                vo.getCustomerPhone() != null ? vo.getCustomerPhone() : "-",
                vo.getLastVisitTime() != null ? DateUtil.formatDateTime(vo.getLastVisitTime()) : "-",
                vo.getInactiveDays());
        }
        log.info("[customerTagJob] 租户 {} 识别「待召回」顾客 {} 人", tenantId, recall.size());
    }

    /**
     * 防爽约判定（T15.32，Cron {@code 0 *&#47;10 * * * ?}）：扫描 expire_time 已过仍「待服务」的预约 →
     * 标记 NO_SHOW + 累计爽约（满 3 次原子拉黑）+ 写爽约明细。
     * <p>★仅 micro_pro 生效：{@code @TenantJob(versions={"micro"}, versionTypes={"micro_pro"})}，lite 档无预约履约闭环，切面过滤跳过。
     * <p>★主动取消(CANCELLED)不算爽约；跨租户隔离（A 店黑名单不影响 B 店）。
     */
    @XxlJob("noShowCheckJob")
    @TenantJob(versions = {"micro"}, versionTypes = {"micro_pro"})
    public void noShowCheckJob() {
        Long tenantId = TenantHelper.getTenantId();
        int rows = customerCreditService.processNoShow();
        XxlJobHelper.log("[noShowCheckJob] 租户 {} 判定爽约 {} 条（满 3 次自动拉黑）", tenantId, rows);
        log.info("[noShowCheckJob] 租户 {} 判定爽约 {} 条", tenantId, rows);
    }

    /**
     * 活动自动结束 + 资源释放（Cron {@code 0 0 *&#47;6 * * ?}）：每 6 小时扫描一次，将 endTime 已过且仍为 PUBLISHED 的活动批量置为 ENDED，
     * 并释放关联的房间和助教资源（→ idle），确保顾客自助预约可选资源与实际占用同步。
     * <p>★浏览页 listPublished 已增加 endTime 过滤，此任务为数据状态兜底，保证 PC 管理列表状态一致。
     */
    @XxlJob("activityEndJob")
    @TenantJob(versions = {"micro"})
    public void activityEndJob() {
        Long tenantId = TenantHelper.getTenantId();
        List<Long> endedIds = promotionActivityService.autoEndActivities();
        XxlJobHelper.log("[activityEndJob] 租户 {} 自动结束活动 {} 条，关联资源已释放", tenantId, endedIds.size());
        log.info("[activityEndJob] 租户 {} 自动结束活动 {} 条，关联资源已释放", tenantId, endedIds.size());
    }

    /**
     * 资源状态自动流转（T21.8，Cron {@code 0 *&#47;10 * * * ?}）：每 10 分钟扫描当天预约，按时间窗 +
     * 订单状态推进助教/房间资源 reserved→in_use（服务开始）、in_use→idle（结束/超窗/完成/爽约），
     * 并写 resource_status_log；幂等（当前状态==目标状态则跳过）。
     * <p>★零支付无关：仅推进资源状态，不涉收款/退款；单租户异常隔离由 {@code TenantJobAspect} 负责。
     * <p>★仅小微版：{@code @TenantJob(versions={"micro"})}；micro_lite 无自助预约数据自然空跑。
     */
    @XxlJob("resourceInUseFlowJob")
    @TenantJob(versions = {"micro"})
    public void resourceInUseFlowJob() {
        Long tenantId = TenantHelper.getTenantId();
        int rows = selfBookingService.advanceResourceStatusByTime();
        XxlJobHelper.log("[resourceInUseFlowJob] 租户 {} 资源状态自动流转 {} 条", tenantId, rows);
        log.info("[resourceInUseFlowJob] 租户 {} 资源状态自动流转 {} 条", tenantId, rows);
    }

    /**
     * ★M22 活动开团前支付提醒（Cron {@code 0 *&#47;5 * * * ?}）：每 5 分钟扫描即将开始（30±5 分钟窗口）的已锁定活动，
     * 向每位待支付参与者发送系统消息通知人均应付金额（到店支付），并将 pay_status 由 PENDING_PAY 置为 REMIND_SENT 防重发。
     * <p>★零支付合规：仅发送提醒消息，不触在线支付流程。
     */
    @XxlJob("activityPayReminderJob")
    @TenantJob(versions = {"micro"})
    public void activityPayReminderJob() {
        Long tenantId = TenantHelper.getTenantId();
        Date now = new Date();
        Date windowStart = new Date(now.getTime() + 25 * 60 * 1000L);  // now + 25min
        Date windowEnd = new Date(now.getTime() + 35 * 60 * 1000L);    // now + 35min
        // ① 查询已发布 + 已锁定 + startTime 在提醒窗口内的活动
        List<PromotionActivity> activities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getStatus, "PUBLISHED")
                .eq(PromotionActivity::getIsLocked, true)
                .ge(PromotionActivity::getStartTime, windowStart)
                .le(PromotionActivity::getStartTime, windowEnd));
        if (activities.isEmpty()) {
            XxlJobHelper.log("[activityPayReminderJob] 租户 {} 无即将开始的活动", tenantId);
            return;
        }
        int totalReminded = 0;
        for (PromotionActivity act : activities) {
            // ② 查询该项目下待支付的报名记录
            if (act.getSelectedProjectId() == null) {
                continue;
            }
            List<ProjectSignup> pendingSignups = signupMapper.selectList(
                Wrappers.<ProjectSignup>lambdaQuery()
                    .eq(ProjectSignup::getProjectId, act.getSelectedProjectId())
                    .eq(ProjectSignup::getPayStatus, "PENDING_PAY"));
            if (pendingSignups.isEmpty()) {
                continue;
            }
            // ③ 计算人均费用
            BigDecimal effectivePrice = act.getCustomPrice() != null
                ? act.getCustomPrice() : act.getAutoCalculatedPrice();
            if (effectivePrice == null || act.getParticipantCount() == null || act.getParticipantCount() <= 0) {
                continue;
            }
            BigDecimal perPerson = effectivePrice.divide(
                BigDecimal.valueOf(act.getParticipantCount()), 2, RoundingMode.HALF_UP);
            // ④ 批量发送系统消息
            List<Long> userIds = pendingSignups.stream()
                .map(ProjectSignup::getUserId)
                .filter(uid -> uid != null)
                .distinct()
                .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                String startTimeStr = DateUtil.formatDateTime(act.getStartTime());
                String title = "活动支付提醒";
                String content = String.format("您参与的【%s】将于 %s 开始，每人需支付 ¥%s，请到店支付。",
                    act.getTitle(), startTimeStr, perPerson.toPlainString());
                sysMessageService.sendBatchMessage(userIds, title, content,
                    "ACTIVITY_PAY_REMIND", "promotion_activity", act.getId());
                totalReminded += userIds.size();
            }
            // ⑤ 更新 pay_status 为 REMIND_SENT 防重发
            for (ProjectSignup s : pendingSignups) {
                ProjectSignup upd = new ProjectSignup();
                upd.setId(s.getId());
                upd.setPayStatus("REMIND_SENT");
                signupMapper.updateById(upd);
            }
        }
        XxlJobHelper.log("[activityPayReminderJob] 租户 {} 提醒 {} 条活动，共通知 {} 人",
            tenantId, activities.size(), totalReminded);
        log.info("[activityPayReminderJob] 租户 {} 提醒 {} 条活动，共通知 {} 人",
            tenantId, activities.size(), totalReminded);
    }
}
