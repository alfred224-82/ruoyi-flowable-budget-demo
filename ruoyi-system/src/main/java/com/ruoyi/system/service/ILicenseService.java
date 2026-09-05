package com.ruoyi.system.service;

import java.util.Set;

/**
 * License授权Service接口
 * 核心职责：管理系统版本（BASIC/ADVANCED/ENTERPRISE）和功能开关
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface ILicenseService {

    /**
     * 系统启动时调用：加载License→校验→加载features到内存
     */
    void loadAndVerify();

    /**
     * 获取当前已授权功能列表
     */
    Set<String> getActiveFeatures();

    /**
     * 获取当前版本等级（BASIC/ADVANCED/ENTERPRISE）
     */
    String getCurrentEdition();

    /**
     * 获取授权行业列表
     */
    Set<String> getAuthorizedIndustries();

    /**
     * 校验某功能是否已授权
     */
    boolean hasFeature(String featureCode);

    /**
     * 校验行业是否已授权
     */
    boolean hasIndustry(String industryCode);

    /**
     * 获取试用剩余小时数（-1=非试用/已过期）
     */
    long getTrialRemainingHours();

    /**
     * 导出机器指纹
     */
    String exportFingerprint();

    /**
     * 替换License并重新加载（版本切换核心方法）
     * 支持通过修改edition字段实现 BASIC↔ADVANCED↔ENTERPRISE 切换
     *
     * @param licenseJson License内容JSON
     */
    void replaceLicense(String licenseJson);

    /**
     * 获取当前License状态信息（脱敏）
     */
    LicenseInfoVo getLicenseInfo();

    // ==================== 租户级 License 方法（T3.10/T3.12） ====================

    /**
     * 获取租户有效功能列表（License 授权功能 ∩ 版本功能，取交集）
     * 无 License 的租户降级为基础版默认能力
     *
     * @param tenantId 租户ID
     * @return 租户有效功能集合
     */
    Set<String> getTenantActiveFeatures(Long tenantId);

    /**
     * 获取租户当前版本等级（从租户级 License 读取，无 License 则返回 BASIC）
     *
     * @param tenantId 租户ID
     * @return 版本等级
     */
    String getTenantEdition(Long tenantId);

    /**
     * 校验租户某功能是否已授权
     *
     * @param tenantId    租户ID
     * @param featureCode 功能编码
     * @return true=已授权
     */
    boolean tenantHasFeature(Long tenantId, String featureCode);

    /**
     * 获取租户 License 信息（脱敏）
     *
     * @param tenantId 租户ID
     * @return License 信息，无 License 时返回基础版默认
     */
    LicenseInfoVo getTenantLicenseInfo(Long tenantId);

    /**
     * 为租户导入 License（T3.11）
     *
     * @param tenantId    目标租户ID
     * @param licenseJson License JSON 内容
     */
    void importLicenseForTenant(Long tenantId, String licenseJson);

    /**
     * 获取按版本分组的租户选择器数据（T3.17）
     *
     * @param name 企业名称模糊搜索（可为 null）
     * @return 按版本分组的租户列表 { BASIC: [...], ADVANCED: [...], PROFESSIONAL: [...] }
     */
    java.util.Map<String, java.util.List<java.util.Map<String, Object>>> getTenantSelector(String name);

    /**
     * License信息VO（内部类）
     */
    class LicenseInfoVo {
        private String licenseId;
        private String licenseType;
        private String customer;
        private String edition;
        private Set<String> features;
        private Set<String> industries;
        private String status;
        private String expiresAt;
        private long trialRemainingHours;
        private Integer trialDays;

        public String getLicenseId() { return licenseId; }
        public void setLicenseId(String licenseId) { this.licenseId = licenseId; }
        public String getLicenseType() { return licenseType; }
        public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        public String getEdition() { return edition; }
        public void setEdition(String edition) { this.edition = edition; }
        public Set<String> getFeatures() { return features; }
        public void setFeatures(Set<String> features) { this.features = features; }
        public Set<String> getIndustries() { return industries; }
        public void setIndustries(Set<String> industries) { this.industries = industries; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
        public long getTrialRemainingHours() { return trialRemainingHours; }
        public void setTrialRemainingHours(long trialRemainingHours) { this.trialRemainingHours = trialRemainingHours; }
        public Integer getTrialDays() { return trialDays; }
        public void setTrialDays(Integer trialDays) { this.trialDays = trialDays; }
    }
}
