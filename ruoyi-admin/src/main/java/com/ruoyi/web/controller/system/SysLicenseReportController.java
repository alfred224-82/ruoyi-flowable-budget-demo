package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.SysLicense;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysLicenseMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * License使用报表Controller（T18.14，小微版 M18）
 * <p>「License使用报表」菜单（system/licenseReport）：
 * <ul>
 *   <li>租户版本分布（按 edition 分组统计）</li>
 *   <li>到期预警（30天内到期租户列表）</li>
 *   <li>License 健康度汇总</li>
 * </ul>
 * <p>★Admin 专属，跨租户聚合。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/licenseReport")
public class SysLicenseReportController extends BaseController {

    private final SysTenantMapper tenantMapper;
    private final SysLicenseMapper licenseMapper;

    /**
     * License 使用报表聚合数据（一次请求返回全部）
     */
    @SaCheckPermission("system:licenseReport:list")
    @GetMapping
    public R<LicenseReportVo> report() {
        LicenseReportVo vo = new LicenseReportVo();

        // ① 全量租户查询
        List<SysTenant> allTenants = tenantMapper.selectList(Wrappers.emptyWrapper());
        if (allTenants == null) {
            allTenants = Collections.emptyList();
        }
        vo.setTenantTotal(allTenants.size());

        // ② 按版本类型分组统计
        Map<String, Long> versionDist = allTenants.stream()
            .collect(Collectors.groupingBy(
                t -> t.getVersionType() != null ? t.getVersionType() : "UNKNOWN",
                Collectors.counting()));
        vo.setVersionDistribution(versionDist);

        // ③ 按租户状态分组统计
        Map<String, Long> statusDist = allTenants.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                Collectors.counting()));
        vo.setStatusDistribution(statusDist);

        // ④ 全量 License 查询
        List<SysLicense> allLicenses = licenseMapper.selectList(Wrappers.emptyWrapper());
        if (allLicenses == null) {
            allLicenses = Collections.emptyList();
        }
        vo.setLicenseTotal(allLicenses.size());

        // ⑤ License 状态分布
        Map<String, Long> licenseStatusDist = allLicenses.stream()
            .collect(Collectors.groupingBy(
                l -> l.getStatus() != null ? l.getStatus() : "UNKNOWN",
                Collectors.counting()));
        vo.setLicenseStatusDistribution(licenseStatusDist);

        // ⑥ 到期预警：30天内到期
        long nowMs = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        List<ExpiringTenantItem> expiringList = new ArrayList<>();

        for (SysTenant tenant : allTenants) {
            // 查该租户最新 ACTIVE License
            SysLicense activeLicense = allLicenses.stream()
                .filter(l -> tenant.getId() != null && tenant.getId().equals(l.getTenantId()))
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .max(Comparator.comparingInt(l -> l.getLicenseVersion() != null ? l.getLicenseVersion() : 0))
                .orElse(null);

            if (activeLicense == null || activeLicense.getExpiresAt() == null) {
                continue; // 永久授权或无 License，跳过
            }

            long diffMs = activeLicense.getExpiresAt().getTime() - nowMs;
            int remainingDays = (int) TimeUnit.MILLISECONDS.toDays(diffMs);

            if (remainingDays <= 30) {
                ExpiringTenantItem item = new ExpiringTenantItem();
                item.setTenantId(tenant.getId());
                item.setCompanyName(tenant.getCompanyName());
                item.setVersionType(tenant.getVersionType());
                item.setLicenseExpireDate(activeLicense.getExpiresAt());
                item.setRemainingDays(Math.max(remainingDays, 0));
                item.setUrgent(remainingDays <= 7);
                expiringList.add(item);
            }
        }

        // 按剩余天数升序排列（最紧急的在前）
        expiringList.sort(Comparator.comparingInt(ExpiringTenantItem::getRemainingDays));
        vo.setExpiringTenants(expiringList);
        vo.setExpiringSoonCount(expiringList.size());

        // ⑦ License 健康度
        long activeCount = allLicenses.stream().filter(l -> "ACTIVE".equals(l.getStatus())).count();
        long expiredCount = allLicenses.stream().filter(l -> "EXPIRED".equals(l.getStatus())).count();
        vo.setActiveLicenseCount((int) activeCount);
        vo.setExpiredLicenseCount((int) expiredCount);
        if (allLicenses.isEmpty()) {
            vo.setHealthRate(0.0);
        } else {
            vo.setHealthRate(Math.round(activeCount * 1000.0 / allLicenses.size()) / 10.0);
        }

        return R.ok(vo);
    }

    // ===== 内部 VO =====

    @Data
    public static class LicenseReportVo implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 租户总数 */
        private Integer tenantTotal;
        /** 版本分布（versionType → count） */
        private Map<String, Long> versionDistribution;
        /** 租户状态分布（status → count） */
        private Map<String, Long> statusDistribution;
        /** License 总数 */
        private Integer licenseTotal;
        /** License 状态分布 */
        private Map<String, Long> licenseStatusDistribution;
        /** 有效 License 数 */
        private Integer activeLicenseCount;
        /** 已过期 License 数 */
        private Integer expiredLicenseCount;
        /** 健康率（%） */
        private Double healthRate;
        /** 30天内到期租户数 */
        private Integer expiringSoonCount;
        /** 到期预警租户列表 */
        private List<ExpiringTenantItem> expiringTenants;
    }

    @Data
    public static class ExpiringTenantItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 租户ID */
        private Long tenantId;
        /** 公司名称 */
        private String companyName;
        /** 版本类型 */
        private String versionType;
        /** License 到期日 */
        private Date licenseExpireDate;
        /** 剩余天数 */
        private Integer remainingDays;
        /** 是否紧急（<=7天） */
        private Boolean urgent;
    }
}
