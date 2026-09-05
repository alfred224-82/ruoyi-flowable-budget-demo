package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.TenantBizConfig;
import com.ruoyi.system.domain.vo.TenantBizConfigVo;
import com.ruoyi.system.mapper.TenantBizConfigMapper;
import com.ruoyi.system.service.IBizBundleService;
import com.ruoyi.system.service.ITenantBizConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 租户业态单店覆盖服务实现（T10.4）
 * <p>合并优先级 = 租户 module_overrides &gt; 业态包默认 copy；禁止 fork 业态包。
 * 保存前做 JSON 结构校验（必须为合法 JSON 对象），非法 JSON 拒绝落库。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBizConfigServiceImpl implements ITenantBizConfigService {

    private final TenantBizConfigMapper tenantBizConfigMapper;
    private final IBizBundleService bizBundleService;
    private final ObjectMapper objectMapper;

    @Override
    public TenantBizConfigVo getConfig(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantBizConfigMapper.selectVoOne(
            Wrappers.<TenantBizConfig>lambdaQuery().eq(TenantBizConfig::getTenantId, tenantId));
    }

    @Override
    public Map<String, Object> getMergedOverrides(Long tenantId, String bizCode) {
        // 基底：业态包默认文案
        Map<String, Object> merged = new LinkedHashMap<>(bizBundleService.getCopy(bizCode));
        // 叠加：租户单店覆盖（优先级更高）
        TenantBizConfigVo config = getConfig(tenantId);
        if (config != null && StrUtil.isNotBlank(config.getModuleOverrides())) {
            Map<String, Object> overrides = parseOverrides(config.getModuleOverrides());
            merged.putAll(overrides);
        }
        return merged;
    }

    @Override
    public int saveConfig(TenantBizConfigVo vo) {
        if (vo == null || vo.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        // JSON 结构校验：module_overrides 非空时必须为合法 JSON 对象
        if (StrUtil.isNotBlank(vo.getModuleOverrides())) {
            parseOverrides(vo.getModuleOverrides());
        }
        TenantBizConfig exist = tenantBizConfigMapper.selectOne(
            Wrappers.<TenantBizConfig>lambdaQuery().eq(TenantBizConfig::getTenantId, vo.getTenantId()));
        int rows;
        if (exist == null) {
            TenantBizConfig entity = new TenantBizConfig();
            entity.setTenantId(vo.getTenantId());
            entity.setModuleOverrides(vo.getModuleOverrides());
            rows = tenantBizConfigMapper.insert(entity);
        } else {
            exist.setModuleOverrides(vo.getModuleOverrides());
            rows = tenantBizConfigMapper.updateById(exist);
        }
        return rows;
    }

    /**
     * 解析 module_overrides JSON 为 Map；非法 JSON 抛异常由调用方拦截（T10.4 Schema 校验）
     */
    private Map<String, Object> parseOverrides(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            log.warn("[TenantBizConfig] module_overrides JSON 非法: {}", json, e);
            throw new IllegalArgumentException("单店覆盖配置 JSON 格式非法，必须为合法 JSON 对象");
        }
    }
}
