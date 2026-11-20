package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.ComplianceAgreement;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.vo.ComplianceAgreementVo;
import com.ruoyi.system.mapper.ComplianceAgreementMapper;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.IComplianceAgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 合规协议服务实现（T25.5，小微版 M25）
 * <p>★协议正文存放于 OSS，agreement_content_url 存完整 OSS URL。
 * <p>★当前版本号 + OSS URL 通过 sys_config(compliance.agreement.current) 动态管理，
 *  平台 Admin 上传协议文件到 OSS 后调用 updateAgreementConfig 更新。
 * <p>★UNIQUE(tenant_id, agreement_version) 防重复签署。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceAgreementServiceImpl implements IComplianceAgreementService {

    private final ComplianceAgreementMapper agreementMapper;
    private final SysConfigMapper sysConfigMapper;

    /** sys_config 键名：存储当前协议版本 + OSS URL（JSON 格式） */
    private static final String CONFIG_KEY = "compliance.agreement.current";

    // ============================================================
    // 租户端：查看/签署/检查
    // ============================================================

    @Override
    public ComplianceAgreementVo getCurrent() {
        Long tenantId = LoginHelper.getTenantId();
        ComplianceAgreementVo vo = new ComplianceAgreementVo();

        // 从 sys_config 读取当前协议配置
        Map<String, String> config = readAgreementConfig();
        String currentVersion = config.get("version");
        String contentUrl = config.get("contentUrl");

        if (StrUtil.isBlank(currentVersion)) {
            // 平台尚未配置协议，无需签署
            vo.setNeedSign(false);
            vo.setCurrentVersion(null);
            return vo;
        }

        vo.setCurrentVersion(currentVersion);
        vo.setAgreementContentUrl(contentUrl);

        if (tenantId == null) {
            vo.setNeedSign(false);
            return vo;
        }

        // 查询当前租户是否已签署最新版本
        ComplianceAgreement signed = agreementMapper.selectOne(
            Wrappers.<ComplianceAgreement>lambdaQuery()
                .eq(ComplianceAgreement::getTenantId, tenantId)
                .eq(ComplianceAgreement::getAgreementVersion, currentVersion));
        if (signed != null) {
            BeanUtils.copyProperties(signed, vo);
            // 保留 currentVersion / contentUrl（不被 signed 覆盖）
            vo.setCurrentVersion(currentVersion);
            vo.setAgreementContentUrl(contentUrl);
            vo.setNeedSign(false);
        } else {
            vo.setTenantId(tenantId);
            vo.setNeedSign(true);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sign(String agreementVersion, String signIp) {
        if (StrUtil.isBlank(agreementVersion)) {
            throw new ServiceException("协议版本号不能为空");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户信息不存在");
        }
        // 从 sys_config 读取当前协议 contentUrl
        Map<String, String> config = readAgreementConfig();
        String contentUrl = config.get("contentUrl");

        // 幂等检查：是否已签署
        Long count = agreementMapper.selectCount(
            Wrappers.<ComplianceAgreement>lambdaQuery()
                .eq(ComplianceAgreement::getTenantId, tenantId)
                .eq(ComplianceAgreement::getAgreementVersion, agreementVersion));
        if (count != null && count > 0) {
            log.info("[agreement] 租户已签署协议，跳过：tenantId={}, version={}", tenantId, agreementVersion);
            return;
        }
        ComplianceAgreement entity = new ComplianceAgreement();
        entity.setTenantId(tenantId);
        entity.setAgreementVersion(agreementVersion);
        entity.setAgreementContentUrl(contentUrl);
        entity.setSignerUserId(LoginHelper.getUserId());
        entity.setSignerName(LoginHelper.getNickName());
        entity.setSignTime(new Date());
        entity.setSignIp(signIp);
        agreementMapper.insert(entity);
        log.info("[agreement] 协议签署成功：tenantId={}, version={}, signer={}",
            tenantId, agreementVersion, LoginHelper.getNickName());
    }

    @Override
    public boolean checkSigned() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return true;
        }
        // 从 sys_config 读取当前版本
        Map<String, String> config = readAgreementConfig();
        String currentVersion = config.get("version");
        if (StrUtil.isBlank(currentVersion)) {
            // 平台尚未配置协议，默认已签署（不拦截）
            return true;
        }
        Long count = agreementMapper.selectCount(
            Wrappers.<ComplianceAgreement>lambdaQuery()
                .eq(ComplianceAgreement::getTenantId, tenantId)
                .eq(ComplianceAgreement::getAgreementVersion, currentVersion));
        return count != null && count > 0;
    }

    // ============================================================
    // 平台 Admin：协议配置管理（OSS 方案）
    // ============================================================

    @Override
    public Map<String, String> getAgreementConfig() {
        return readAgreementConfig();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgreementConfig(String version, String contentUrl) {
        if (StrUtil.isBlank(version)) {
            throw new ServiceException("协议版本号不能为空");
        }
        if (StrUtil.isBlank(contentUrl)) {
            throw new ServiceException("协议内容URL不能为空");
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("version", version);
        data.put("contentUrl", contentUrl);
        String jsonValue = JsonUtils.toJsonString(data);

        // 幂等：存在则更新，不存在则插入
        SysConfig existing = sysConfigMapper.selectOne(
            Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, CONFIG_KEY));
        if (existing != null) {
            existing.setConfigValue(jsonValue);
            existing.setUpdateBy(LoginHelper.getNickName());
            existing.setUpdateTime(new Date());
            sysConfigMapper.updateById(existing);
        } else {
            SysConfig newConfig = new SysConfig();
            newConfig.setConfigName("合规协议当前版本配置");
            newConfig.setConfigKey(CONFIG_KEY);
            newConfig.setConfigValue(jsonValue);
            newConfig.setConfigType("Y");
            newConfig.setCreateBy(LoginHelper.getNickName());
            newConfig.setCreateTime(new Date());
            newConfig.setRemark("JSON格式：{\"version\":\"v1.0\",\"contentUrl\":\"https://...\"}");
            sysConfigMapper.insert(newConfig);
        }
        log.info("[agreement] 协议配置更新：version={}, contentUrl={}", version, contentUrl);
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    /**
     * 从 sys_config 读取当前协议配置（version + contentUrl）
     *
     * @return Map 含 version / contentUrl；若无配置则返回空 Map
     */
    private Map<String, String> readAgreementConfig() {
        SysConfig config = sysConfigMapper.selectOne(
            Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, CONFIG_KEY));
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = JsonUtils.parseMap(config.getConfigValue());
            Map<String, String> result = new LinkedHashMap<>();
            result.put("version", raw.getOrDefault("version", "").toString());
            result.put("contentUrl", raw.getOrDefault("contentUrl", "").toString());
            return result;
        } catch (Exception e) {
            log.warn("[agreement] sys_config 解析失败，key={}，value={}", CONFIG_KEY, config.getConfigValue(), e);
            return Collections.emptyMap();
        }
    }
}
