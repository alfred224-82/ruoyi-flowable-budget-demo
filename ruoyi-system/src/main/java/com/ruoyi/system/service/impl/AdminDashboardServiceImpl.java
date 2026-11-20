package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.VoiceComplianceHit;
import com.ruoyi.system.domain.vo.AdminDashboardVo;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.VoiceComplianceHitMapper;
import com.ruoyi.system.service.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin 平台驾驶舱首页服务实现（T18.5，小微版 M18）
 * <p>★fail-fast：所有聚合查询均做 null 保护，返回空集合不返回 null。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final SysTenantMapper tenantMapper;
    private final VoiceComplianceHitMapper hitMapper;

    @Override
    public AdminDashboardVo getDashboard() {
        AdminDashboardVo vo = new AdminDashboardVo();

        // ===== 租户大盘 =====
        fillTenantDashboard(vo);

        // ===== 系统健康度 =====
        fillSystemHealth(vo);

        // ===== 全局合规风险监控 =====
        fillComplianceRisk(vo);

        return vo;
    }

    /**
     * 租户大盘：总数/活跃度/行业分布/版本分布/7天趋势
     */
    private void fillTenantDashboard(AdminDashboardVo vo) {
        // ★全量租户查询（Admin 视角，跨租户；数据量可控，V1 直接全量查）
        List<SysTenant> allTenants = tenantMapper.selectList(Wrappers.emptyWrapper());
        if (allTenants == null) {
            allTenants = Collections.emptyList();
        }

        vo.setTenantTotal((long) allTenants.size());
        vo.setTenantActive(allTenants.stream()
            .filter(t -> "NORMAL".equals(t.getStatus())).count());
        vo.setTenantExpired(allTenants.stream()
            .filter(t -> "EXPIRED".equals(t.getStatus()) || "GRACE".equals(t.getStatus())).count());

        // 行业分布
        Map<String, Long> industryMap = allTenants.stream()
            .filter(t -> t.getIndustry() != null)
            .collect(Collectors.groupingBy(SysTenant::getIndustry, Collectors.counting()));
        List<Map<String, Object>> industryDist = new ArrayList<>();
        industryMap.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("industry", k);
            item.put("count", v);
            industryDist.add(item);
        });
        vo.setIndustryDistribution(industryDist);

        // 版本档位分布
        Map<String, Long> versionMap = allTenants.stream()
            .filter(t -> t.getVersionType() != null)
            .collect(Collectors.groupingBy(SysTenant::getVersionType, Collectors.counting()));
        List<Map<String, Object>> versionDist = new ArrayList<>();
        versionMap.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("versionType", k);
            item.put("count", v);
            versionDist.add(item);
        });
        vo.setVersionDistribution(versionDist);

        // 近 7 天新增租户趋势
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = cal.getTime();

        List<SysTenant> recentTenants = allTenants.stream()
            .filter(t -> t.getCreateTime() != null && t.getCreateTime().after(sevenDaysAgo))
            .collect(Collectors.toList());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Long> trendMap = recentTenants.stream()
            .collect(Collectors.groupingBy(
                t -> sdf.format(t.getCreateTime()), Collectors.counting()));
        // 补齐 7 天（无数据的日期补 0）
        List<Map<String, Object>> trend7d = new ArrayList<>();
        Calendar dayCal = Calendar.getInstance();
        dayCal.add(Calendar.DAY_OF_MONTH, -6); // 从 6 天前开始到今天
        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(dayCal.getTime());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("count", trendMap.getOrDefault(dateStr, 0L));
            trend7d.add(item);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        vo.setTenantTrend7d(trend7d);
    }

    /**
     * 系统健康度：到期预警/禁用统计/综合健康状态
     */
    private void fillSystemHealth(AdminDashboardVo vo) {
        // 到期预警：30 天内到期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        Date thirtyDaysLater = cal.getTime();
        Date now = new Date();

        List<SysTenant> allTenants = tenantMapper.selectList(Wrappers.emptyWrapper());
        if (allTenants == null) {
            allTenants = Collections.emptyList();
        }

        long expiringSoon = allTenants.stream()
            .filter(t -> "NORMAL".equals(t.getStatus()))
            .filter(t -> t.getExpireDate() != null
                && t.getExpireDate().after(now)
                && t.getExpireDate().before(thirtyDaysLater))
            .count();
        vo.setExpiringSoonCount(expiringSoon);

        long disabled = allTenants.stream()
            .filter(t -> "DISABLED".equals(t.getStatus())).count();
        vo.setDisabledTenantCount(disabled);

        // 综合健康状态判定
        String health;
        if (disabled > 0 || vo.getTenantExpired() > 5) {
            health = "CRITICAL";
        } else if (expiringSoon > 0 || vo.getTenantExpired() > 0) {
            health = "WARNING";
        } else {
            health = "HEALTH";
        }
        vo.setSystemHealth(health);
    }

    /**
     * 全局合规风险监控：近 7 天命中趋势/按类型分布/待处理数
     */
    private void fillComplianceRisk(AdminDashboardVo vo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = cal.getTime();

        // ★跨租户查全量合规命中（Admin 视角）
        LambdaQueryWrapper<VoiceComplianceHit> wrapper = Wrappers.<VoiceComplianceHit>lambdaQuery()
            .ge(VoiceComplianceHit::getHitTime, sevenDaysAgo);
        List<VoiceComplianceHit> hits = hitMapper.selectList(wrapper);
        if (hits == null) {
            hits = Collections.emptyList();
        }

        vo.setComplianceHits7d((long) hits.size());
        vo.setCompliancePendingCount(hits.stream()
            .filter(h -> "PENDING".equals(h.getStatus())).count());

        // 按类型分布
        Map<String, Long> typeMap = hits.stream()
            .collect(Collectors.groupingBy(
                h -> h.getHitType() != null ? h.getHitType() : "UNKNOWN",
                Collectors.counting()));
        List<Map<String, Object>> byType = new ArrayList<>();
        typeMap.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hitType", k);
            item.put("count", v);
            byType.add(item);
        });
        vo.setComplianceByType(byType);

        // 按天趋势（补齐 7 天）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Long> dayMap = hits.stream()
            .filter(h -> h.getHitTime() != null)
            .collect(Collectors.groupingBy(
                h -> sdf.format(h.getHitTime()), Collectors.counting()));
        List<Map<String, Object>> trend7d = new ArrayList<>();
        Calendar dayCal = Calendar.getInstance();
        dayCal.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(dayCal.getTime());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("count", dayMap.getOrDefault(dateStr, 0L));
            trend7d.add(item);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        vo.setComplianceTrend7d(trend7d);

        log.info("[AdminDashboard] complianceHits7d={}, pending={}", hits.size(), vo.getCompliancePendingCount());
    }
}
