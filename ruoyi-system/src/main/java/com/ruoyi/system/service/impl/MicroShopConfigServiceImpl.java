package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.MicroShopConfigBo;
import com.ruoyi.system.domain.vo.MicroShopConfigVo;
import com.ruoyi.system.domain.vo.TenantBizConfigVo;
import com.ruoyi.system.service.IMicroShopConfigService;
import com.ruoyi.system.service.ITenantBizConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小微版店铺管理配置服务实现（T15.24，老板 PC）
 * <p>以 {@code tenant_biz_config.module_overrides} JSON 为存储：读取→合并指定键→整体写回（{@link ITenantBizConfigService}
 * 负责 JSON 合法性校验与 upsert）；仅作用于当前登录租户（老板 PC web 上下文，多租户插件自动隔离）。
 * <p>★不臆造 schema：单店差异（营业时间/收款说明/通知方式）统一走单店覆盖表（设计文档 1.8.5「禁止 fork 业态包」）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroShopConfigServiceImpl implements IMicroShopConfigService {

    /** module_overrides JSON 键：营业时间 */
    private static final String KEY_BUSINESS_HOURS = "businessHours";
    /** module_overrides JSON 键：收款方式说明 */
    private static final String KEY_PAY_INFO = "payInfo";
    /** module_overrides JSON 键：通知方式设置 */
    private static final String KEY_NOTIFY_CONFIG = "notifyConfig";

    private final ITenantBizConfigService tenantBizConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public MicroShopConfigVo getConfig() {
        Map<String, Object> overrides = loadOverrides(requireTenantId());
        MicroShopConfigVo vo = new MicroShopConfigVo();
        vo.setBusinessHours(asString(overrides.get(KEY_BUSINESS_HOURS)));
        vo.setPayInfo(asString(overrides.get(KEY_PAY_INFO)));
        vo.setNotifyConfig(asString(overrides.get(KEY_NOTIFY_CONFIG)));
        return vo;
    }

    @Override
    public int saveConfig(MicroShopConfigBo bo) {
        Map<String, Object> patch = new LinkedHashMap<>();
        if (bo != null) {
            if (bo.getBusinessHours() != null) {
                patch.put(KEY_BUSINESS_HOURS, bo.getBusinessHours());
            }
            if (bo.getPayInfo() != null) {
                patch.put(KEY_PAY_INFO, bo.getPayInfo());
            }
            if (bo.getNotifyConfig() != null) {
                patch.put(KEY_NOTIFY_CONFIG, bo.getNotifyConfig());
            }
        }
        return merge(patch);
    }

    @Override
    public int saveBusinessHours(String businessHours) {
        return merge(Collections.singletonMap(KEY_BUSINESS_HOURS, businessHours));
    }

    @Override
    public int savePayInfo(String payInfo) {
        return merge(Collections.singletonMap(KEY_PAY_INFO, payInfo));
    }

    @Override
    public int saveNotifyConfig(String notifyConfig) {
        return merge(Collections.singletonMap(KEY_NOTIFY_CONFIG, notifyConfig));
    }

    /**
     * 读取现有覆盖 → 合并 patch（仅覆盖提供的键，保留其余业态模块开关/文案）→ 整体写回
     */
    private int merge(Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) {
            return 0;
        }
        Long tenantId = requireTenantId();
        Map<String, Object> overrides = loadOverrides(tenantId);
        overrides.putAll(patch);
        TenantBizConfigVo cfg = new TenantBizConfigVo();
        cfg.setTenantId(tenantId);
        cfg.setModuleOverrides(writeJson(overrides));
        return tenantBizConfigService.saveConfig(cfg);
    }

    /**
     * 加载当前租户 module_overrides 为可变 Map（无配置或解析失败按空处理，避免脏数据阻断）
     */
    private Map<String, Object> loadOverrides(Long tenantId) {
        TenantBizConfigVo cfg = tenantBizConfigService.getConfig(tenantId);
        if (cfg == null || StrUtil.isBlank(cfg.getModuleOverrides())) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(cfg.getModuleOverrides(),
                new TypeReference<Map<String, Object>>() {});
            return map != null ? new LinkedHashMap<>(map) : new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("[MicroShopConfig] 租户 {} module_overrides JSON 解析失败，按空覆盖处理", tenantId, e);
            return new LinkedHashMap<>();
        }
    }

    private Long requireTenantId() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("无法确定当前租户，店铺配置不可用");
        }
        return tenantId;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("[MicroShopConfig] 店铺配置序列化失败", e);
            throw new ServiceException("店铺配置序列化失败");
        }
    }

    /**
     * 覆盖值转文案：字符串直接返回；对象/数组序列化为 JSON 串（通知方式可能为结构化配置）
     */
    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return writeJson(value);
    }
}
