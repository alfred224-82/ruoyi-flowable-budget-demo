package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.config.LicenseProperties;
import com.ruoyi.system.domain.SysLicense;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.SysTenantVersionLog;
import com.ruoyi.system.mapper.SysLicenseMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysTenantVersionLogMapper;
import com.ruoyi.system.service.IBizBundleService;
import com.ruoyi.system.service.IFeatureFlagService;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.utils.LicenseSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * License授权Service业务层处理
 * 核心机制：通过数据库中的edition字段和features JSON控制版本功能
 *
 * 双模式支持（license.enforce-mode配置）：
 * - soft模式（开发/测试）：仅读取数据库配置，不校验签名和指纹，支持API明文切换版本
 * - hard模式（生产环境）：强制RSA数字签名验证 + 机器指纹绑定 + 防篡改检测
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements ILicenseService, FeatureChecker {

    private final SysLicenseMapper licenseMapper;
    private final ObjectMapper objectMapper;
    private final LicenseProperties licenseProperties;
    private final SysTenantMapper tenantMapper;
    private final SysTenantVersionLogMapper versionLogMapper;
    /** 业态包服务（T10.8 三维能力计算第③维：biz_bundle.feature_flags 过滤） */
    private final IBizBundleService bizBundleService;
    /** 功能开关服务（T10.8 三维能力计算第②维：feature_flag 表驱动版本默认开启集） */
    private final IFeatureFlagService featureFlagService;

    /** 内存缓存：当前激活的功能列表（系统级，admin 超管使用） */
    private volatile Set<String> activeFeatures = Collections.emptySet();
    /** 内存缓存：当前版本等级 */
    private volatile String currentEdition = EditionFeatureConfig.EDITION_BASIC;
    /** 内存缓存：授权行业列表 */
    private volatile Set<String> authorizedIndustries = Collections.emptySet();
    /** 内存缓存：当前License实体 */
    private volatile SysLicense currentLicense;

    /** 内存缓存：租户级 License（tenantId → SysLicense），T3.10 */
    private final ConcurrentHashMap<Long, SysLicense> tenantLicenseCache = new ConcurrentHashMap<>();
    /** 内存缓存：租户级有效功能（tenantId → features），T3.10 */
    private final ConcurrentHashMap<Long, Set<String>> tenantFeaturesCache = new ConcurrentHashMap<>();
    /** 内存缓存：租户级版本等级（tenantId → edition），T3.10 */
    private final ConcurrentHashMap<Long, String> tenantEditionCache = new ConcurrentHashMap<>();

    /**
     * 系统启动时自动加载License
     * soft模式：仅读取数据库，不校验签名/指纹（开发测试用）
     * hard模式：强制校验RSA签名 + 机器指纹绑定（生产环境）
     */
    @PostConstruct
    @Override
    public void loadAndVerify() {
        try {
            SysLicense license = licenseMapper.selectOne(
                Wrappers.<SysLicense>lambdaQuery()
                    .eq(SysLicense::getStatus, "ACTIVE")
                    .orderByDesc(SysLicense::getLicenseVersion)
                    .last("LIMIT 1")
            );
            if (license == null) {
                log.warn("[License] 未找到有效License，系统以BASIC版本运行");
                currentEdition = EditionFeatureConfig.EDITION_BASIC;
                activeFeatures = EditionFeatureConfig.BASIC_FEATURES;
                authorizedIndustries = Collections.emptySet();
                return;
            }

            // === hard模式：数字签名验证 + 机器指纹校验 ===
            if (licenseProperties.isHardMode()) {
                if (!verifySignature(license)) {
                    log.error("[License] 签名验证失败！License可能被篡改，降级为BASIC版本");
                    markAsTampered(license);
                    return;
                }
                if (!verifyFingerprint(license)) {
                    log.error("[License] 机器指纹不匹配！License不属于当前机器，降级为BASIC版本");
                    markAsTampered(license);
                    return;
                }
                log.info("[License] hard模式校验通过（签名+指纹）");
            }

            currentLicense = license;
            // 加载版本等级
            currentEdition = StrUtil.isNotBlank(license.getEdition()) ? license.getEdition() : EditionFeatureConfig.EDITION_BASIC;
            // 加载功能列表：优先从features JSON解析，若为空则按edition默认配置
            activeFeatures = parseFeatures(license.getFeatures(), currentEdition);
            // 加载授权行业
            authorizedIndustries = parseJsonSet(license.getIndustries());
            // 校验有效期
            if (license.getExpiresAt() != null && license.getExpiresAt().before(new Date())) {
                log.warn("[License] 授权已过期，降级为BASIC版本");
                currentEdition = EditionFeatureConfig.EDITION_BASIC;
                activeFeatures = EditionFeatureConfig.BASIC_FEATURES;
                license.setStatus("EXPIRED");
                licenseMapper.updateById(license);
                return;
            }
            // 更新最后校验时间
            license.setLastCheckTime(new Date());
            licenseMapper.updateById(license);
            log.info("[License] 加载成功 | 模式={} | 版本={} | 功能数={} | 行业={}",
                licenseProperties.getEnforceMode(), currentEdition, activeFeatures.size(), authorizedIndustries);
        } catch (Exception e) {
            log.error("[License] 加载失败，降级为BASIC版本", e);
            currentEdition = EditionFeatureConfig.EDITION_BASIC;
            activeFeatures = EditionFeatureConfig.BASIC_FEATURES;
        }
    }

    /**
     * 获取当前登录用户的租户ID（T3.12 租户上下文感知辅助方法）
     * admin 超管或未登录时返回 null
     */
    private Long getCurrentTenantId() {
        try {
            if (LoginHelper.isAdmin()) {
                return null;
            }
            return LoginHelper.getTenantId();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== hard模式安全校验方法 ====================

    /**
     * hard模式：验证RSA数字签名
     * 将License核心字段拼接后用公钥验签，任何字段被篡改都会失败
     */
    private boolean verifySignature(SysLicense license) {
        String publicKey = licenseProperties.getPublicKey();
        if (StrUtil.isBlank(publicKey)) {
            log.error("[License] hard模式未配置公钥(license.public-key)，无法验签");
            return false;
        }
        if (StrUtil.isBlank(license.getSignature()) || "LOCAL-SWITCH".equals(license.getSignature())) {
            log.error("[License] 签名无效（缺失或为本地切换标记）");
            return false;
        }
        String expiresAtStr = license.getExpiresAt() != null
            ? new SimpleDateFormat("yyyy-MM-dd").format(license.getExpiresAt()) : "";
        String signContent = LicenseSignUtil.buildSignContent(
            license.getLicenseId(), license.getEdition(), license.getLicenseType(),
            license.getCustomer(), license.getFeatures(), license.getIndustries(),
            expiresAtStr, license.getMachineFingerprint(), license.getLicenseVersion()
        );
        return LicenseSignUtil.verify(signContent, license.getSignature(), publicKey);
    }

    /**
     * hard模式：验证机器指纹是否匹配当前服务器
     */
    private boolean verifyFingerprint(SysLicense license) {
        if (StrUtil.isBlank(license.getMachineFingerprint())) {
            log.warn("[License] License未绑定机器指纹，跳过指纹校验");
            return true; // 未绑定指纹的License（如永久授权）不校验
        }
        String currentFp = exportFingerprint();
        boolean match = license.getMachineFingerprint().equals(currentFp);
        if (!match) {
            log.error("[License] 指纹不匹配 | 期望={} | 实际={}", license.getMachineFingerprint(), currentFp);
        }
        return match;
    }

    /**
     * 验证传入的License JSON签名（hard模式replaceLicense时使用）
     */
    private boolean verifyIncomingSignature(JsonNode node, String signature) {
        String publicKey = licenseProperties.getPublicKey();
        if (StrUtil.isBlank(publicKey)) {
            return false;
        }
        String licenseId = getJsonText(node, "licenseId");
        String edition = getJsonText(node, "edition");
        String licenseType = getJsonText(node, "licenseType");
        String customer = getJsonText(node, "customer");
        String features = getJsonText(node, "features");
        String industries = getJsonText(node, "industries");
        String expiresAt = getJsonText(node, "expiresAt");
        String machineFingerprint = getJsonText(node, "machineFingerprint");
        Integer licenseVersion = (node.has("licenseVersion") && !node.get("licenseVersion").isNull())
            ? node.get("licenseVersion").asInt() : null;
        String signContent = LicenseSignUtil.buildSignContent(
            licenseId, edition, licenseType, customer, features, industries,
            expiresAt != null ? expiresAt : "", machineFingerprint, licenseVersion
        );
        return LicenseSignUtil.verify(signContent, signature, publicKey);
    }

    /**
     * 标记License为已篡改，降级为BASIC
     */
    private void markAsTampered(SysLicense license) {
        license.setStatus("TAMPERED");
        licenseMapper.updateById(license);
        currentEdition = EditionFeatureConfig.EDITION_BASIC;
        activeFeatures = EditionFeatureConfig.BASIC_FEATURES;
        authorizedIndustries = Collections.emptySet();
        currentLicense = null;
    }

    // ==================== 公共接口实现 ====================

    @Override
    public Set<String> getActiveFeatures() {
        // T3.12：租户上下文感知
        Long tenantId = getCurrentTenantId();
        if (tenantId != null) {
            return getTenantActiveFeatures(tenantId);
        }
        return activeFeatures;
    }

    @Override
    public String getCurrentEdition() {
        // T3.12：租户上下文感知
        Long tenantId = getCurrentTenantId();
        if (tenantId != null) {
            return getTenantEdition(tenantId);
        }
        return currentEdition;
    }

    @Override
    public Set<String> getAuthorizedIndustries() {
        return authorizedIndustries;
    }

    @Override
    public boolean hasFeature(String featureCode) {
        // T3.12：租户上下文感知——租户用户从租户级 License 读取
        Long tenantId = getCurrentTenantId();
        if (tenantId != null) {
            return getTenantActiveFeatures(tenantId).contains(featureCode);
        }
        return activeFeatures.contains(featureCode);
    }

    @Override
    public boolean isFeatureEnabled(String featureCode) {
        return hasFeature(featureCode);
    }

    @Override
    public boolean hasIndustry(String industryCode) {
        if (authorizedIndustries.isEmpty()) {
            return true; // 未配置行业限制时默认全部可用
        }
        return authorizedIndustries.contains(industryCode);
    }

    @Override
    public long getTrialRemainingHours() {
        if (currentLicense == null || !"TRIAL".equals(currentLicense.getLicenseType())) {
            return -1;
        }
        if (currentLicense.getActivatedAt() == null) {
            return -1;
        }
        long elapsedMs = System.currentTimeMillis() - currentLicense.getActivatedAt().getTime();
        long totalMs = TimeUnit.DAYS.toMillis(currentLicense.getTrialDays() != null ? currentLicense.getTrialDays() : 0);
        long remainingMs = totalMs - elapsedMs;
        return remainingMs > 0 ? TimeUnit.MILLISECONDS.toHours(remainingMs) : 0;
    }

    @Override
    public String exportFingerprint() {
        StringBuilder sb = new StringBuilder("FP-");
        // 操作系统信息
        String os = System.getProperty("os.name", "UNKNOWN").replaceAll("\\s+", "");
        sb.append(os).append("-");
        // 主机名+IP
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sb.append(addr.getHostName()).append("-").append(addr.getHostAddress());
        } catch (Exception e) {
            sb.append("HOST-NA");
        }
        // MAC地址
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp() || ni.getHardwareAddress() == null) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                StringBuilder macSb = new StringBuilder();
                for (byte b : mac) {
                    macSb.append(String.format("%02X", b));
                }
                sb.append("-").append(macSb);
                break; // 只取第一个有效网卡
            }
        } catch (Exception ignored) {
        }
        String fingerprint = sb.toString();
        // 兜底：确保永不为空
        if (fingerprint.length() <= 4) {
            fingerprint = "FP-" + os + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        return fingerprint;
    }

    /**
     * 替换License（版本切换核心方法）
     * soft模式：支持明文JSON切换（开发测试用）
     * hard模式：仅接受带有效RSA签名的License文件，禁止明文切换
     *
     * 支持JSON字段：
     * - edition: 版本等级（BASIC/ADVANCED/ENTERPRISE）
     * - licenseType: 授权类型（TRIAL=试用/FORMAL=正式）
     * - durationDays: 授权期限天数（7=7天试用, 90=3个月, 365=1年, null=永久）
     * - customer: 客户名称
     * - features/industries/status: 其他可选字段
     * - signature: RSA签名（hard模式必须）
     */
    @Override
    public void replaceLicense(String licenseJson) {
        try {
            JsonNode node = objectMapper.readTree(licenseJson);

            // === hard模式：必须携带有效签名，禁止明文切换 ===
            if (licenseProperties.isHardMode()) {
                String signature = getJsonText(node, "signature");
                if (StrUtil.isBlank(signature)) {
                    throw new ServiceException("生产模式(hard)禁止明文切换版本，请使用厂商签发的License文件");
                }
                if (!verifyIncomingSignature(node, signature)) {
                    throw new ServiceException("License签名验证失败，请确认文件未被篡改");
                }
            }

            String edition = getJsonText(node, "edition");
            String licenseType = getJsonText(node, "licenseType");
            String customer = getJsonText(node, "customer");
            String features = getJsonText(node, "features");
            String industries = getJsonText(node, "industries");
            String status = getJsonText(node, "status");
            Integer durationDays = (node.has("durationDays") && !node.get("durationDays").isNull())
                ? node.get("durationDays").asInt() : null;

            // 查找现有License（含ACTIVE和EXPIRED，避免过期后无法切换）
            SysLicense existing = licenseMapper.selectOne(
                Wrappers.<SysLicense>lambdaQuery()
                    .in(SysLicense::getStatus, "ACTIVE", "EXPIRED", "TAMPERED")
                    .orderByDesc(SysLicense::getLicenseVersion)
                    .last("LIMIT 1")
            );

            if (existing != null) {
                // === 更新现有License ===
                if (StrUtil.isNotBlank(edition)) {
                    existing.setEdition(edition);
                }
                if (StrUtil.isNotBlank(licenseType)) {
                    existing.setLicenseType(licenseType);
                }
                if (StrUtil.isNotBlank(customer)) {
                    existing.setCustomer(customer);
                }
                if (StrUtil.isNotBlank(industries)) {
                    existing.setIndustries(industries);
                }
                if (StrUtil.isNotBlank(status)) {
                    existing.setStatus(status);
                } else {
                    existing.setStatus("ACTIVE");
                }
                // 处理授权期限
                applyDuration(existing, licenseType, durationDays);
                // 功能列表：由版本等级决定全量功能
                String targetEdition = StrUtil.isNotBlank(edition) ? edition : existing.getEdition();
                if (StrUtil.isNotBlank(features)) {
                    existing.setFeatures(features);
                } else {
                    existing.setFeatures(objectMapper.writeValueAsString(EditionFeatureConfig.getFeaturesByEdition(targetEdition)));
                }
                // hard模式：保存签名和指纹
                if (licenseProperties.isHardMode()) {
                    existing.setSignature(getJsonText(node, "signature"));
                    String fp = getJsonText(node, "machineFingerprint");
                    if (StrUtil.isNotBlank(fp)) {
                        existing.setMachineFingerprint(fp);
                    }
                }
                existing.setLicenseVersion(existing.getLicenseVersion() != null ? existing.getLicenseVersion() + 1 : 1);
                licenseMapper.updateById(existing);
            } else {
                // === 新增License ===
                SysLicense newLicense = new SysLicense();
                newLicense.setLicenseId("LIC-" + new SimpleDateFormat("yyyy").format(new Date()) + "-" + String.format("%06d", new Random().nextInt(999999)));
                newLicense.setEdition(StrUtil.isNotBlank(edition) ? edition : EditionFeatureConfig.EDITION_BASIC);
                newLicense.setLicenseType(StrUtil.isNotBlank(licenseType) ? licenseType : "FORMAL");
                newLicense.setCustomer(StrUtil.isNotBlank(customer) ? customer : "未命名客户");
                newLicense.setMachineFingerprint(exportFingerprint());
                newLicense.setIssuedAt(new Date());
                newLicense.setLicenseVersion(1);
                newLicense.setStatus("ACTIVE");
                newLicense.setIndustries(StrUtil.isNotBlank(industries) ? industries : null);
                // hard模式：使用传入的签名；soft模式：标记为LOCAL-SWITCH
                if (licenseProperties.isHardMode()) {
                    newLicense.setSignature(getJsonText(node, "signature"));
                    String fp = getJsonText(node, "machineFingerprint");
                    if (StrUtil.isNotBlank(fp)) {
                        newLicense.setMachineFingerprint(fp);
                    }
                } else {
                    newLicense.setSignature("LOCAL-SWITCH");
                }
                // 处理授权期限
                applyDuration(newLicense, newLicense.getLicenseType(), durationDays);
                // 功能列表：由版本等级决定全量功能
                if (StrUtil.isNotBlank(features)) {
                    newLicense.setFeatures(features);
                } else {
                    newLicense.setFeatures(objectMapper.writeValueAsString(EditionFeatureConfig.getFeaturesByEdition(newLicense.getEdition())));
                }
                licenseMapper.insert(newLicense);
            }
            // 重新加载
            loadAndVerify();
            log.info("[License] 版本切换成功 → {} | 类型={} | 客户={}", currentEdition, licenseType, customer);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("License替换失败: " + e.getMessage());
        }
    }

    /**
     * 处理授权期限：
     * - TRIAL + durationDays(默认7天)：设置trialDays、activatedAt、expiresAt
     * - FORMAL + durationDays(90/365)：设置expiresAt = 当前 + 天数
     * - FORMAL + durationDays=null：永久授权（expiresAt=null）
     */
    private void applyDuration(SysLicense license, String licenseType, Integer durationDays) {
        if ("TRIAL".equalsIgnoreCase(licenseType)) {
            int days = (durationDays != null && durationDays > 0) ? durationDays : 7;
            license.setTrialDays(days);
            license.setActivatedAt(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, days);
            license.setExpiresAt(cal.getTime());
        } else if ("FORMAL".equalsIgnoreCase(licenseType)) {
            license.setTrialDays(0);
            if (durationDays != null && durationDays > 0) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, durationDays);
                license.setExpiresAt(cal.getTime());
            } else {
                license.setExpiresAt(null); // 永久授权
            }
        }
    }

    /**
     * 安全获取JSON文本字段
     */
    private String getJsonText(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            String val = node.get(field).asText();
            return StrUtil.isNotBlank(val) ? val : null;
        }
        return null;
    }

    @Override
    public LicenseInfoVo getLicenseInfo() {
        LicenseInfoVo vo = new LicenseInfoVo();
        if (currentLicense != null) {
            vo.setLicenseId(currentLicense.getLicenseId());
            vo.setLicenseType(currentLicense.getLicenseType());
            vo.setCustomer(currentLicense.getCustomer());
            vo.setStatus(currentLicense.getStatus());
            vo.setTrialDays(currentLicense.getTrialDays());
            if (currentLicense.getExpiresAt() != null) {
                vo.setExpiresAt(new SimpleDateFormat("yyyy-MM-dd").format(currentLicense.getExpiresAt()));
            } else {
                vo.setExpiresAt("永久");
            }
        }
        vo.setEdition(currentEdition);
        vo.setFeatures(activeFeatures);
        vo.setIndustries(authorizedIndustries);
        vo.setTrialRemainingHours(getTrialRemainingHours());
        return vo;
    }

    /**
     * 解析功能列表：版本等级决定功能全集
     * 企业版 = 全部功能开放；数据库features JSON仅作为自定义裁剪的参考，
     * 取「版本默认功能 ∪ 数据库存储功能」的并集，确保版本升级后功能完整
     */
    private Set<String> parseFeatures(String featuresJson, String edition) {
        Set<String> editionFeatures = new HashSet<>(EditionFeatureConfig.getFeaturesByEdition(edition));
        if (StrUtil.isNotBlank(featuresJson)) {
            try {
                Set<String> stored = objectMapper.readValue(featuresJson, new TypeReference<Set<String>>() {});
                if (stored != null && !stored.isEmpty()) {
                    editionFeatures.addAll(stored);
                }
            } catch (Exception e) {
                log.warn("[License] features JSON解析失败，使用版本默认配置", e);
            }
        }
        return editionFeatures;
    }

    /**
     * 解析JSON数组为Set
     */
    private Set<String> parseJsonSet(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptySet();
        }
        try {
            Set<String> set = objectMapper.readValue(json, new TypeReference<Set<String>>() {});
            return set != null ? set : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    // ==================== 租户级 License 方法（T3.10/T3.11/T3.12） ====================

    /**
     * 加载并缓存租户级 License
     * 从数据库按 tenant_id 加载 ACTIVE 状态的 License，校验有效期，
     * 过期则标记 EXPIRED 并降级为基础版
     */
    private SysLicense loadTenantLicense(Long tenantId) {
        SysLicense cached = tenantLicenseCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        SysLicense license = licenseMapper.selectOne(
            Wrappers.<SysLicense>lambdaQuery()
                .eq(SysLicense::getTenantId, tenantId)
                .eq(SysLicense::getStatus, "ACTIVE")
                .orderByDesc(SysLicense::getLicenseVersion)
                .last("LIMIT 1")
        );
        if (license != null) {
            // 校验有效期
            if (license.getExpiresAt() != null && license.getExpiresAt().before(new Date())) {
                log.warn("[License] 租户 {} License 已过期，降级为基础版", tenantId);
                license.setStatus("EXPIRED");
                licenseMapper.updateById(license);
                // 写入版本变更日志（T3.16）
                logTenantVersionChange(tenantId, license.getEdition(), EditionFeatureConfig.EDITION_BASIC,
                    "LICENSE_EXPIRE", "License到期自动降级为基础版");
                tenantLicenseCache.put(tenantId, null);
                return null;
            }
            tenantLicenseCache.put(tenantId, license);
        }
        return license;
    }

    @Override
    public Set<String> getTenantActiveFeatures(Long tenantId) {
        if (tenantId == null) {
            return activeFeatures;
        }
        Set<String> cached = tenantFeaturesCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        // 业态维度：读取租户 biz_code（T10.8 第③维）
        String bizCode = loadTenantBizCode(tenantId);
        SysLicense license = loadTenantLicense(tenantId);
        if (license == null) {
            // 无 License 的租户降级为基础版默认能力，仍受业态包过滤（三维一致）
            Set<String> basic = applyBizFilter(EditionFeatureConfig.BASIC_FEATURES, bizCode);
            Set<String> basicImmutable = Collections.unmodifiableSet(basic);
            tenantFeaturesCache.put(tenantId, basicImmutable);
            tenantEditionCache.put(tenantId, EditionFeatureConfig.EDITION_BASIC);
            return basicImmutable;
        }
        // 租户版本等级
        String edition = StrUtil.isNotBlank(license.getEdition()) ? license.getEdition() : EditionFeatureConfig.EDITION_BASIC;
        tenantEditionCache.put(tenantId, edition);
        // 维度①：License 授权功能（并集策略：版本默认 ∪ 数据库存储）
        Set<String> licenseFeatures = parseFeatures(license.getFeatures(), edition);
        // 维度②：版本开关（feature_flag 表驱动，兜底常量，T10.3）
        Set<String> editionFeatures = featureFlagService.getEditionFeatureSet(edition);
        // ①∩②：License 授权 ∩ 版本功能（T3.12 防"License 有但版本未分配"串功能）
        Set<String> intersection = new HashSet<>(licenseFeatures);
        intersection.retainAll(editionFeatures);
        // 维度③：业态包过滤（仅 FEAT_* 参与交集，legacy 透传；业态包缺失则跳过，T10.2/T10.8）
        Set<String> result = Collections.unmodifiableSet(applyBizFilter(intersection, bizCode));
        tenantFeaturesCache.put(tenantId, result);
        return result;
    }

    /**
     * 读取租户业态编码（T10.8）；缺省回落默认业态包 fund_control
     */
    private String loadTenantBizCode(Long tenantId) {
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getBizCode())) {
                return tenant.getBizCode();
            }
        } catch (Exception e) {
            log.warn("[License] 读取租户 {} 业态编码失败，回落默认业态包", tenantId, e);
        }
        return BizBundleServiceImpl.DEFAULT_BIZ_CODE;
    }

    /**
     * 业态包维度过滤（T10.8 三维能力计算第③维）
     * <p>仅对 FEAT_* 新开关取交集（业态包未声明即不开放）；legacy FEE_* / BUDGET_* 透传，
     * 避免历史能力被业态包误伤。业态包缺失或未声明开关时跳过过滤（保留全部，getBundle 已告警）。
     */
    private Set<String> applyBizFilter(Set<String> features, String bizCode) {
        Set<String> bizFlags = bizBundleService.getFeatureFlags(bizCode);
        if (bizFlags == null || bizFlags.isEmpty()) {
            return new HashSet<>(features);
        }
        Set<String> result = new HashSet<>();
        for (String code : features) {
            if (code != null && code.startsWith("FEAT_")) {
                if (bizFlags.contains(code)) {
                    result.add(code);
                }
            } else {
                result.add(code);
            }
        }
        return result;
    }

    @Override
    public String getTenantEdition(Long tenantId) {
        if (tenantId == null) {
            return currentEdition;
        }
        String cached = tenantEditionCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        // 触发加载
        getTenantActiveFeatures(tenantId);
        return tenantEditionCache.getOrDefault(tenantId, EditionFeatureConfig.EDITION_BASIC);
    }

    @Override
    public boolean tenantHasFeature(Long tenantId, String featureCode) {
        return getTenantActiveFeatures(tenantId).contains(featureCode);
    }

    @Override
    public LicenseInfoVo getTenantLicenseInfo(Long tenantId) {
        if (tenantId == null) {
            return getLicenseInfo();
        }
        SysLicense license = loadTenantLicense(tenantId);
        LicenseInfoVo vo = new LicenseInfoVo();
        if (license != null) {
            vo.setLicenseId(license.getLicenseId());
            vo.setLicenseType(license.getLicenseType());
            vo.setCustomer(license.getCustomer());
            vo.setStatus(license.getStatus());
            vo.setTrialDays(license.getTrialDays());
            if (license.getExpiresAt() != null) {
                vo.setExpiresAt(new SimpleDateFormat("yyyy-MM-dd").format(license.getExpiresAt()));
            } else {
                vo.setExpiresAt("永久");
            }
            String edition = StrUtil.isNotBlank(license.getEdition()) ? license.getEdition() : EditionFeatureConfig.EDITION_BASIC;
            vo.setEdition(edition);
            vo.setFeatures(getTenantActiveFeatures(tenantId));
            vo.setIndustries(parseJsonSet(license.getIndustries()));
        } else {
            // 无 License，降级为基础版默认
            vo.setLicenseId("未授权");
            vo.setEdition(EditionFeatureConfig.EDITION_BASIC);
            vo.setFeatures(EditionFeatureConfig.BASIC_FEATURES);
            vo.setIndustries(Collections.emptySet());
            vo.setStatus("NONE");
            vo.setExpiresAt("-");
        }
        return vo;
    }

    /**
     * 为租户导入 License（T3.11）
     * 校验签名（hard 模式）→ 校验有效期 → 写入 sys_license →
     * 同步更新租户 expire_date 和 edition → 刷新缓存 → 写版本日志
     */
    @Override
    public void importLicenseForTenant(Long tenantId, String licenseJson) {
        try {
            JsonNode node = objectMapper.readTree(licenseJson);

            // === hard模式：强制签名校验 ===
            if (licenseProperties.isHardMode()) {
                String signature = getJsonText(node, "signature");
                if (StrUtil.isBlank(signature)) {
                    throw new ServiceException("生产模式(hard)必须提供RSA签名的License文件");
                }
                if (!verifyIncomingSignature(node, signature)) {
                    throw new ServiceException("License签名验证失败，文件可能被篡改");
                }
            }

            String edition = getJsonText(node, "edition");
            String licenseType = getJsonText(node, "licenseType");
            String customer = getJsonText(node, "customer");
            String features = getJsonText(node, "features");
            String industries = getJsonText(node, "industries");
            String expiresAtStr = getJsonText(node, "expiresAt");
            String machineFingerprint = getJsonText(node, "machineFingerprint");
            Integer licenseVersion = (node.has("licenseVersion") && !node.get("licenseVersion").isNull())
                ? node.get("licenseVersion").asInt() : 1;

            // 解析到期日期
            Date expiresAt = null;
            if (StrUtil.isNotBlank(expiresAtStr)) {
                expiresAt = new SimpleDateFormat("yyyy-MM-dd").parse(expiresAtStr);
            }

            // 将旧 License 标记为 EXPIRED（如果存在）
            SysLicense existing = licenseMapper.selectOne(
                Wrappers.<SysLicense>lambdaQuery()
                    .eq(SysLicense::getTenantId, tenantId)
                    .eq(SysLicense::getStatus, "ACTIVE")
                    .orderByDesc(SysLicense::getLicenseVersion)
                    .last("LIMIT 1")
            );
            String fromEdition = null;
            if (existing != null) {
                fromEdition = existing.getEdition();
                existing.setStatus("REPLACED");
                licenseMapper.updateById(existing);
            }

            // 查询租户当前版本（用于版本日志）
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                throw new ServiceException("租户不存在");
            }
            if (fromEdition == null) {
                fromEdition = tenant.getEdition();
            }

            // 创建新 License 记录
            SysLicense newLicense = new SysLicense();
            newLicense.setTenantId(tenantId);
            newLicense.setLicenseId(getJsonText(node, "licenseId"));
            if (StrUtil.isBlank(newLicense.getLicenseId())) {
                newLicense.setLicenseId("LIC-" + new SimpleDateFormat("yyyy").format(new Date())
                    + "-" + String.format("%06d", new Random().nextInt(999999)));
            }
            newLicense.setEdition(StrUtil.isNotBlank(edition) ? edition : EditionFeatureConfig.EDITION_BASIC);
            newLicense.setLicenseType(StrUtil.isNotBlank(licenseType) ? licenseType : "FORMAL");
            newLicense.setCustomer(StrUtil.isNotBlank(customer) ? customer : tenant.getCompanyName());
            newLicense.setIssuedAt(new Date());
            newLicense.setExpiresAt(expiresAt);
            newLicense.setLicenseVersion(licenseVersion);
            newLicense.setStatus("ACTIVE");
            newLicense.setIndustries(StrUtil.isNotBlank(industries) ? industries : null);
            newLicense.setMachineFingerprint(StrUtil.isNotBlank(machineFingerprint) ? machineFingerprint : null);
            // 功能列表
            if (StrUtil.isNotBlank(features)) {
                newLicense.setFeatures(features);
            } else {
                newLicense.setFeatures(objectMapper.writeValueAsString(
                    EditionFeatureConfig.getFeaturesByEdition(newLicense.getEdition())));
            }
            // hard模式：保存签名
            if (licenseProperties.isHardMode()) {
                newLicense.setSignature(getJsonText(node, "signature"));
            } else {
                newLicense.setSignature("LOCAL-IMPORT");
            }
            // 试用类型处理
            if ("TRIAL".equalsIgnoreCase(newLicense.getLicenseType())) {
                Integer trialDays = (node.has("trialDays") && !node.get("trialDays").isNull())
                    ? node.get("trialDays").asInt() : 7;
                newLicense.setTrialDays(trialDays);
                newLicense.setActivatedAt(new Date());
            }
            licenseMapper.insert(newLicense);

            // 同步更新租户 expire_date 和 edition（T3.11）
            SysTenant tenantUpdate = new SysTenant();
            tenantUpdate.setId(tenantId);
            tenantUpdate.setEdition(newLicense.getEdition());
            tenantUpdate.setExpireDate(expiresAt);
            tenantUpdate.setLicenseType(newLicense.getLicenseType());
            tenantMapper.updateById(tenantUpdate);

            // 清除租户缓存，触发重新加载
            evictTenantCache(tenantId);

            // 写版本变更日志（T3.16）
            String toEdition = newLicense.getEdition();
            String changeType = (existing == null) ? "LICENSE_IMPORT" : "LICENSE_REPLACE";
            String remark = (existing == null) ? "导入License" : "替换License";
            logTenantVersionChange(tenantId, fromEdition, toEdition, changeType, remark);

            log.info("[License] 租户 {} License 导入成功 | 版本={} | 到期={}",
                tenantId, newLicense.getEdition(), expiresAt);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("License导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取按版本分组的租户选择器数据（T3.17）
     */
    @Override
    public Map<String, List<Map<String, Object>>> getTenantSelector(String name) {
        List<SysTenant> tenants = tenantMapper.selectList(
            Wrappers.<SysTenant>lambdaQuery()
                .like(StrUtil.isNotBlank(name), SysTenant::getCompanyName, name)
                .ne(SysTenant::getStatus, "DELETED")
                .orderByAsc(SysTenant::getCompanyName)
        );
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("BASIC", new ArrayList<>());
        result.put("ADVANCED", new ArrayList<>());
        result.put("PROFESSIONAL", new ArrayList<>());
        for (SysTenant t : tenants) {
            String edition = StrUtil.isNotBlank(t.getEdition()) ? t.getEdition() : EditionFeatureConfig.EDITION_BASIC;
            // 兼容 ENTERPRISE
            if ("ENTERPRISE".equalsIgnoreCase(edition)) {
                edition = "PROFESSIONAL";
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("companyName", t.getCompanyName());
            item.put("tenantCode", t.getTenantCode());
            item.put("edition", edition);
            item.put("status", t.getStatus());
            // 检查是否有租户级 License
            SysLicense lic = licenseMapper.selectOne(
                Wrappers.<SysLicense>lambdaQuery()
                    .eq(SysLicense::getTenantId, t.getId())
                    .eq(SysLicense::getStatus, "ACTIVE")
                    .orderByDesc(SysLicense::getLicenseVersion)
                    .last("LIMIT 1")
            );
            item.put("hasLicense", lic != null);
            item.put("licenseId", lic != null ? lic.getLicenseId() : null);
            result.computeIfAbsent(edition, k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 清除租户 License 缓存（导入/替换/过期时调用）
     */
    public void evictTenantCache(Long tenantId) {
        tenantLicenseCache.remove(tenantId);
        tenantFeaturesCache.remove(tenantId);
        tenantEditionCache.remove(tenantId);
    }

    /**
     * 写入租户版本变更日志（T3.16）
     */
    private void logTenantVersionChange(Long tenantId, String fromEdition, String toEdition,
                                        String changeType, String remark) {
        try {
            SysTenantVersionLog log = new SysTenantVersionLog();
            log.setTenantId(tenantId);
            log.setFromEdition(fromEdition);
            log.setToEdition(toEdition);
            log.setChangeType(changeType);
            try {
                log.setOperator(LoginHelper.getUsername());
            } catch (Exception e) {
                log.setOperator("system");
            }
            log.setRemark(remark);
            versionLogMapper.insert(log);
        } catch (Exception e) {
            log.error("[License] 写入版本变更日志失败", e);
        }
    }
}
