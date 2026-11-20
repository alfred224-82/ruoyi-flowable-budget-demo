package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.vo.TenantOverviewVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.ITenantOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 租户管理员企业概览首页服务实现（T18.6，小微版 M18）
 * <p>★fail-fast：所有聚合查询均做 null 保护。
 * <p>★配置健康度自检：原子数据完整度 + 周期规则状态 + 合规状态，红/黄/绿三态。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantOverviewServiceImpl implements ITenantOverviewService {

    private final SysTenantMapper tenantMapper;
    private final SysLicenseMapper licenseMapper;
    private final FixedCostTagMapper fixedCostTagMapper;
    private final PeriodRuleMapper periodRuleMapper;
    private final VoiceComplianceRuleMapper complianceRuleMapper;

    @Override
    public TenantOverviewVo getOverview() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户ID不能为空");
        }

        TenantOverviewVo vo = new TenantOverviewVo();

        // ===== 企业基本信息 =====
        fillEnterpriseInfo(vo, tenantId);

        // ===== 配置健康度自检 =====
        fillConfigHealth(vo, tenantId);

        return vo;
    }

    /**
     * 企业基本信息 + License 状态
     */
    private void fillEnterpriseInfo(TenantOverviewVo vo, Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new RuntimeException("租户不存在");
        }

        vo.setCompanyName(tenant.getCompanyName());
        vo.setIndustry(tenant.getIndustry());
        vo.setVersionType(tenant.getVersionType());
        vo.setTenantStatus(tenant.getStatus());
        vo.setCreateTime(tenant.getCreateTime());

        // License 状态：查本租户最新 ACTIVE License
        SysLicense license = licenseMapper.selectOne(
            Wrappers.<SysLicense>lambdaQuery()
                .eq(SysLicense::getTenantId, tenantId)
                .eq(SysLicense::getStatus, "ACTIVE")
                .orderByDesc(SysLicense::getLicenseVersion)
                .last("LIMIT 1")
        );

        if (license != null) {
            vo.setLicenseStatus("ACTIVE");
            vo.setLicenseExpireDate(license.getExpiresAt());
            if (license.getExpiresAt() == null) {
                // 永久授权
                vo.setLicenseRemainingDays(-1);
            } else {
                long diffMs = license.getExpiresAt().getTime() - System.currentTimeMillis();
                int remaining = (int) TimeUnit.MILLISECONDS.toDays(diffMs);
                vo.setLicenseRemainingDays(Math.max(remaining, 0));
            }
        } else {
            vo.setLicenseStatus("NONE");
            vo.setLicenseRemainingDays(0);
        }
    }

    /**
     * 配置健康度自检：原子数据完整度 + 周期规则 + 合规状态
     */
    private void fillConfigHealth(TenantOverviewVo vo, Long tenantId) {
        List<TenantOverviewVo.ConfigCheckItem> checks = new ArrayList<>();

        // ① 固定支出标签：至少 1 个启用标签
        Long enabledTagCount = fixedCostTagMapper.selectCount(
            Wrappers.<FixedCostTag>lambdaQuery()
                .eq(FixedCostTag::getStatus, "ENABLED"));
        TenantOverviewVo.ConfigCheckItem tagCheck = new TenantOverviewVo.ConfigCheckItem();
        tagCheck.setName("固定支出标签");
        tagCheck.setDescription("至少配置 1 个启用的固定支出标签");
        if (enabledTagCount != null && enabledTagCount > 0) {
            tagCheck.setStatus("GREEN");
        } else {
            tagCheck.setStatus("RED");
            tagCheck.setSuggestion("请前往「标签管理」添加并启用至少一个标签");
        }
        checks.add(tagCheck);

        // ② 周期规则：至少 1 个启用规则
        Long enabledRuleCount = periodRuleMapper.selectCount(
            Wrappers.<PeriodRule>lambdaQuery()
                .eq(PeriodRule::getStatus, "ENABLED"));
        TenantOverviewVo.ConfigCheckItem ruleCheck = new TenantOverviewVo.ConfigCheckItem();
        ruleCheck.setName("周期规则");
        ruleCheck.setDescription("至少配置 1 个启用的周期规则");
        if (enabledRuleCount != null && enabledRuleCount > 0) {
            ruleCheck.setStatus("GREEN");
        } else {
            ruleCheck.setStatus("RED");
            ruleCheck.setSuggestion("请前往「周期规则管理」添加并启用至少一个规则");
        }
        checks.add(ruleCheck);

        // ③ 语音合规规则：至少 1 个启用规则
        Long enabledComplianceCount = complianceRuleMapper.selectCount(
            Wrappers.<VoiceComplianceRule>lambdaQuery()
                .eq(VoiceComplianceRule::getEnabled, true));
        TenantOverviewVo.ConfigCheckItem complianceCheck = new TenantOverviewVo.ConfigCheckItem();
        complianceCheck.setName("语音合规配置");
        complianceCheck.setDescription("至少配置 1 个启用的合规规则");
        if (enabledComplianceCount != null && enabledComplianceCount > 0) {
            complianceCheck.setStatus("GREEN");
        } else {
            complianceCheck.setStatus("YELLOW");
            complianceCheck.setSuggestion("建议前往「语音与合规配置」启用至少一个合规规则");
        }
        checks.add(complianceCheck);

        // ④ License 有效期
        TenantOverviewVo.ConfigCheckItem licenseCheck = new TenantOverviewVo.ConfigCheckItem();
        licenseCheck.setName("License 授权");
        licenseCheck.setDescription("License 授权有效");
        if ("ACTIVE".equals(vo.getLicenseStatus())) {
            int remaining = vo.getLicenseRemainingDays() != null ? vo.getLicenseRemainingDays() : 0;
            if (remaining == -1) {
                licenseCheck.setStatus("GREEN"); // 永久
            } else if (remaining > 30) {
                licenseCheck.setStatus("GREEN");
            } else if (remaining > 0) {
                licenseCheck.setStatus("YELLOW");
                licenseCheck.setSuggestion("License 将在 " + remaining + " 天后到期，请及时续期");
            } else {
                licenseCheck.setStatus("RED");
                licenseCheck.setSuggestion("License 已到期，请尽快续期");
            }
        } else {
            licenseCheck.setStatus("RED");
            licenseCheck.setSuggestion("未检测到有效 License，请联系平台管理员");
        }
        checks.add(licenseCheck);

        vo.setConfigChecks(checks);

        // 整体健康度：有 RED → RED；有 YELLOW → YELLOW；全 GREEN → GREEN
        boolean hasRed = checks.stream().anyMatch(c -> "RED".equals(c.getStatus()));
        boolean hasYellow = checks.stream().anyMatch(c -> "YELLOW".equals(c.getStatus()));
        if (hasRed) {
            vo.setConfigHealth("RED");
        } else if (hasYellow) {
            vo.setConfigHealth("YELLOW");
        } else {
            vo.setConfigHealth("GREEN");
        }
    }
}
