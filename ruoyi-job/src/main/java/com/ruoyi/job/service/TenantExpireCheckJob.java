package com.ruoyi.job.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.TenantStatusChecker;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 租户到期每日检测任务（T3.4）
 * <p>
 * XXL-JOB 配置：
 * - tenantExpireCheckJob: cron = "0 30 0 * * ?"（每日 00:30）
 * <p>
 * 执行逻辑：
 * 1. 扫描 status=NORMAL 且 expire_date 已过的租户 → 进入 GRACE 宽限期（3 天只读）；
 * 2. 扫描 status=GRACE 且 grace_period_until 已过的租户 → 状态转 EXPIRED；
 * 3. 清除租户状态缓存（拦截即时生效）；
 * 4. 踢出 EXPIRED 租户的全部在线会话（GRACE 租户保留会话，只读模式）。
 * <p>
 * 注：即使本任务未按时运行，{@link TenantStatusChecker} 的被动检测兜底
 * 仍会在登录/接口访问时纠正过期状态并拦截（防"过期续用"）。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpireCheckJob {

    private final SysTenantMapper tenantMapper;
    private final TenantStatusChecker tenantStatusChecker;

    @XxlJob("tenantExpireCheckJob")
    public void execute() {
        XxlJobHelper.log("开始执行租户到期每日检测任务（含 3 天只读宽限期 T14.2）");
        Date now = new Date();

        // 1. 扫描已到期但仍为 NORMAL 的租户 → 进入 GRACE 宽限期
        List<SysTenant> expiredTenants = tenantMapper.selectList(
            Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getStatus, "NORMAL")
                .isNotNull(SysTenant::getExpireDate)
                .lt(SysTenant::getExpireDate, now));

        int graceCount = 0;
        for (SysTenant tenant : expiredTenants) {
            // 进入 3 天只读宽限期
            Date graceUntil = DateUtil.offsetDay(tenant.getExpireDate(), 3);
            SysTenant update = new SysTenant();
            update.setId(tenant.getId());
            update.setStatus("GRACE");
            update.setGracePeriodUntil(graceUntil);
            update.setUpdateBy("tenantExpireCheckJob");
            tenantMapper.updateById(update);
            tenantStatusChecker.evictStatusCache(tenant.getId());
            graceCount++;
            XxlJobHelper.log("租户 {} ({}) 已进入只读宽限期，截止 {}",
                tenant.getTenantCode(), tenant.getCompanyName(), DateUtil.formatDateTime(graceUntil));
            log.info("[Tenant] 到期检测：租户 {} ({}) 进入 GRACE 宽限期，截止 {}",
                tenant.getTenantCode(), tenant.getCompanyName(), graceUntil);
        }

        // 2. 扫描 GRACE 状态但宽限期已过的租户 → 转 EXPIRED + 踢出会话
        List<SysTenant> graceExpired = tenantMapper.selectList(
            Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getStatus, "GRACE")
                .isNotNull(SysTenant::getGracePeriodUntil)
                .lt(SysTenant::getGracePeriodUntil, now));

        int expiredCount = 0;
        int kickedCount = 0;
        for (SysTenant tenant : graceExpired) {
            SysTenant update = new SysTenant();
            update.setId(tenant.getId());
            update.setStatus("EXPIRED");
            update.setUpdateBy("tenantExpireCheckJob");
            tenantMapper.updateById(update);
            tenantStatusChecker.evictStatusCache(tenant.getId());
            kickedCount += kickoutTenantSessions(tenant.getId());
            expiredCount++;
            XxlJobHelper.log("租户 {} ({}) 宽限期已过，状态转 EXPIRED",
                tenant.getTenantCode(), tenant.getCompanyName());
            log.info("[Tenant] 宽限期检测：租户 {} ({}) 状态转 EXPIRED",
                tenant.getTenantCode(), tenant.getCompanyName());
        }

        XxlJobHelper.log("租户到期检测完成，新进入宽限期: {} 个，宽限期到期转EXPIRED: {} 个，踢出会话: {} 个",
            graceCount, expiredCount, kickedCount);
    }

    /**
     * 踢出指定租户的全部在线会话（与在线用户监控同模式：searchTokenValue + kickoutByTokenValue）
     */
    private int kickoutTenantSessions(Long tenantId) {
        int kicked = 0;
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        for (String key : keys) {
            String token = key.substring(key.lastIndexOf(':') + 1);
            try {
                // 已过期的 token 跳过
                if (StpUtil.stpLogic.getTokenActivityTimeoutByToken(token) < -1) {
                    continue;
                }
                LoginUser loginUser = LoginHelper.getLoginUser(token);
                if (loginUser != null && tenantId.equals(loginUser.getTenantId())) {
                    StpUtil.kickoutByTokenValue(token);
                    kicked++;
                }
            } catch (Exception e) {
                // 会话可能已失效，忽略
                log.debug("[Tenant] 踢出会话跳过（已失效）：{}", token);
            }
        }
        return kicked;
    }
}
