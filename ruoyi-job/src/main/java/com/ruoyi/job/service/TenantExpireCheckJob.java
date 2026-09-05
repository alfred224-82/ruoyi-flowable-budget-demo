package com.ruoyi.job.service;

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
 * 1. 扫描 status=NORMAL 且 expire_date 已过的租户 → 状态转 EXPIRED；
 * 2. 清除租户状态缓存（拦截即时生效）；
 * 3. 踢出该租户全部在线会话（验收用例 7：在线会话到期后被拦截）。
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
        XxlJobHelper.log("开始执行租户到期每日检测任务");
        Date now = new Date();

        // 1. 扫描已到期但仍为 NORMAL 的租户（含 PENDING 到期未启用的场景不处理，仅针对正常租户）
        List<SysTenant> expiredTenants = tenantMapper.selectList(
            Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getStatus, "NORMAL")
                .isNotNull(SysTenant::getExpireDate)
                .lt(SysTenant::getExpireDate, now));

        int expiredCount = 0;
        int kickedCount = 0;
        for (SysTenant tenant : expiredTenants) {
            // 2. 状态转 EXPIRED（幂等：按主键更新）
            SysTenant update = new SysTenant();
            update.setId(tenant.getId());
            update.setStatus("EXPIRED");
            update.setUpdateBy("tenantExpireCheckJob");
            tenantMapper.updateById(update);

            // 3. 清除状态缓存 + 通知其他节点
            tenantStatusChecker.evictStatusCache(tenant.getId());

            // 4. 踢出该租户全部在线会话
            kickedCount += kickoutTenantSessions(tenant.getId());

            expiredCount++;
            XxlJobHelper.log("租户 {} ({}) 已到期，状态转 EXPIRED", tenant.getTenantCode(), tenant.getCompanyName());
            log.info("[Tenant] 到期检测：租户 {} ({}) 状态转 EXPIRED", tenant.getTenantCode(), tenant.getCompanyName());
        }

        XxlJobHelper.log("租户到期检测完成，到期租户: {} 个，踢出会话: {} 个", expiredCount, kickedCount);
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
