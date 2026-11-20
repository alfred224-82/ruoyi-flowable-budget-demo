package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ComplianceAgreementVo;

import java.util.Map;

/**
 * 合规协议服务接口（T25.5，小微版 M25）
 * <p>租户 Admin 首次登录强制签署，协议版本更新仅新租户需重签。
 * <p>★协议正文存放于 OSS，agreement_content_url 存完整 OSS URL，
 *  当前版本号 + URL 通过 sys_config(compliance.agreement.current) 动态管理。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface IComplianceAgreementService {

    /**
     * 获取当前协议版本及签署状态
     *
     * @return 协议 VO（含 needSign / currentVersion / agreementContentUrl）
     */
    ComplianceAgreementVo getCurrent();

    /**
     * 签署协议（T25.5）
     *
     * @param agreementVersion 协议版本号
     * @param signIp           签署时IP
     */
    void sign(String agreementVersion, String signIp);

    /**
     * 检查当前租户是否已签署最新协议（登录守卫用）
     *
     * @return true=已签署或无需签署；false=需要签署
     */
    boolean checkSigned();

    // ===== 平台 Admin 协议管理（T25.5 OSS 方案） =====

    /**
     * 获取当前协议配置（平台 Admin 管理用）
     *
     * @return Map 含 version / contentUrl
     */
    Map<String, String> getAgreementConfig();

    /**
     * 更新协议配置（平台 Admin 上传 OSS 后调用）
     *
     * @param version    新版本号
     * @param contentUrl OSS 完整 URL
     */
    void updateAgreementConfig(String version, String contentUrl);
}
