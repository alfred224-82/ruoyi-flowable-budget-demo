package com.ruoyi.job.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IMessageService;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 计费到期提醒任务（T14.2）
 * <p>
 * XXL-JOB 配置：
 * - billingRemindJob: cron = "0 0 9 * * ?"（每日 09:00）
 * <p>
 * 执行逻辑：扫描到期前 7/3/1 天的正常租户，经 {@link IMessageService} 走对应通道发送续费提醒。
 * <p>
 * 【严禁自动扣款】：本任务仅发送提醒，绝不发起任何扣款/续费扣费；到期停用由
 * {@link TenantExpireCheckJob}（T3.4）负责，二者职责分离。
 * <p>
 * Job 无登录态，故显式以租户 access_channel 指定消息通道，不依赖上下文推断；发送失败降级不阻塞。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingRemindJob {

    /** 到期提醒提前天数（7/3/1 天） */
    private static final int[] REMIND_DAYS = {7, 3, 1};

    private final SysTenantMapper tenantMapper;
    private final IMessageService messageService;

    @XxlJob("billingRemindJob")
    public void execute() {
        XxlJobHelper.log("开始执行计费到期提醒任务（仅提醒，严禁自动扣款）");
        Date now = new Date();
        int total = 0;
        for (int days : REMIND_DAYS) {
            Date dayStart = DateUtil.beginOfDay(DateUtil.offsetDay(now, days));
            Date dayEnd = DateUtil.offsetDay(dayStart, 1);
            List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getStatus, "NORMAL")
                .isNotNull(SysTenant::getExpireDate)
                .ge(SysTenant::getExpireDate, dayStart)
                .lt(SysTenant::getExpireDate, dayEnd));
            for (SysTenant tenant : tenants) {
                if (sendRemind(tenant, days)) {
                    total++;
                }
            }
        }
        XxlJobHelper.log("计费到期提醒完成，发送提醒 {} 条", total);
    }

    /**
     * 向单个租户发送到期提醒（接收人优先取管理员用户ID，回退联系电话）
     */
    private boolean sendRemind(SysTenant tenant, int days) {
        String receiver = tenant.getAdminUserId() != null
            ? String.valueOf(tenant.getAdminUserId()) : tenant.getContactPhone();
        if (StrUtil.isBlank(receiver)) {
            XxlJobHelper.log("租户 {} 无可用接收人，跳过到期提醒", tenant.getTenantCode());
            return false;
        }
        MessageContext context = new MessageContext();
        // Job 无登录态：显式指定通道，避免依赖当前租户推断
        context.setChannel(tenant.getAccessChannel());
        context.setBizType("expire_remind");
        context.setReceiver(receiver);
        context.setTitle("服务到期提醒");
        context.setContent(StrUtil.format("您的服务将于 {} 到期（剩余 {} 天），请及时续费以避免影响使用。系统不会自动扣款。",
            DateUtil.formatDate(tenant.getExpireDate()), days));
        context.getData().put("tenantId", tenant.getId());
        context.getData().put("expireDate", DateUtil.formatDate(tenant.getExpireDate()));
        context.getData().put("remainDays", days);
        try {
            MessageResult result = messageService.send(context);
            String state = result == null ? "NULL" : result.getResult();
            XxlJobHelper.log("租户 {} 到期提醒（剩余{}天）发送结果：{}", tenant.getTenantCode(), days, state);
        } catch (Exception e) {
            // 提醒失败不阻塞任务（降级）
            log.warn("[BillingRemind] 租户 {} 到期提醒发送异常", tenant.getTenantCode(), e);
            XxlJobHelper.log("租户 {} 到期提醒发送异常：{}", tenant.getTenantCode(), e.getMessage());
        }
        return true;
    }
}
