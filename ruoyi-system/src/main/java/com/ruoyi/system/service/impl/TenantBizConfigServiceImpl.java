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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TenantBizConfigSchemaValidator schemaValidator;

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
        // ★T10.4 v1.29 缺口封闭：JSON Schema 校验（结构+白名单+类型）
        if (StrUtil.isNotBlank(vo.getModuleOverrides())) {
            schemaValidator.validate(vo.getModuleOverrides());
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
     * ★T10.4 v1.29：对外暴露 Schema 描述（供前端管理页展示约束与文档同步）
     */
    public Map<String, Object> describeSchema() {
        return schemaValidator.describeSchema();
    }

    /**
     * ★T10.6 v1.29：运行时渲染 Schema（Schema 页面引擎后端解析）
     * <p>业态包 schema_blocks 为基底，叠加租户 module_overrides（enabled/copy/fields）与 global（theme/copy）。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRenderSchema(Long tenantId, String bizCode) {
        // 基底：业态包声明的区块列表
        List<Map<String, Object>> rawBlocks = bizBundleService.getSchemaBlocks(bizCode);
        // 叠加：租户单店覆盖
        Map<String, Object> modules = new HashMap<>();
        Map<String, Object> global = new HashMap<>();
        if (tenantId != null) {
            TenantBizConfigVo config = getConfig(tenantId);
            if (config != null && StrUtil.isNotBlank(config.getModuleOverrides())) {
                Map<String, Object> overrides = parseOverrides(config.getModuleOverrides());
                Object m = overrides.get("modules");
                if (m instanceof Map) {
                    modules = (Map<String, Object>) m;
                }
                Object g = overrides.get("global");
                if (g instanceof Map) {
                    global = (Map<String, Object>) g;
                }
            }
        }
        Map<String, Object> globalCopy = asStringMap(global.get("copy"));
        String theme = global.get("theme") instanceof String ? (String) global.get("theme") : "";

        List<Map<String, Object>> resolved = new ArrayList<>();
        for (Map<String, Object> block : rawBlocks) {
            if (block == null) {
                continue;
            }
            String module = block.get("module") instanceof String ? (String) block.get("module") : null;
            Map<String, Object> mo = module != null ? asMap(modules.get(module)) : new HashMap<>();
            // 规则①：enabled=false 剔除区块
            Object enabled = mo.get("enabled");
            if (enabled instanceof Boolean && !((Boolean) enabled)) {
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>(block);
            Map<String, Object> moduleCopy = asStringMap(mo.get("copy"));
            // 规则②：标题覆盖（模块 copy.title > global copy.<code> > 原值）
            Object title = out.get("title");
            String code = block.get("code") instanceof String ? (String) block.get("code") : null;
            if (moduleCopy.get("title") != null) {
                out.put("title", moduleCopy.get("title"));
            } else if (code != null && globalCopy.get(code) != null) {
                out.put("title", globalCopy.get(code));
            } else if (title == null && code != null && moduleCopy.get(code) != null) {
                out.put("title", moduleCopy.get(code));
            }
            // 规则③：字段属性覆盖（fields + copy 标签）
            Map<String, Object> fieldOverrides = asMap(mo.get("fields"));
            out.put("fields", resolveFields(block.get("fields"), fieldOverrides, moduleCopy));
            out.put("columns", resolveFields(block.get("columns"), fieldOverrides, moduleCopy));
            resolved.add(out);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bizCode", StrUtil.isNotBlank(bizCode) ? bizCode : "fund_control");
        result.put("theme", theme);
        result.put("blocks", resolved);
        return result;
    }

    /**
     * 解析字段/列列表，叠加 fields 覆盖（visible/required/label）与 copy 标签覆盖。
     * visible=false 的字段直接剔除；label 优先取 fields.label，次取 copy.<key>。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveFields(Object rawFields, Map<String, Object> fieldOverrides,
                                                    Map<String, Object> moduleCopy) {
        if (!(rawFields instanceof List)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : (List<Object>) rawFields) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> field = new LinkedHashMap<>((Map<String, Object>) o);
            String key = field.get("key") instanceof String ? (String) field.get("key") : null;
            Map<String, Object> fo = key != null ? asMap(fieldOverrides.get(key)) : new HashMap<>();
            Object visible = fo.get("visible");
            if (visible instanceof Boolean && !((Boolean) visible)) {
                continue; // 字段隐藏
            }
            if (fo.get("required") instanceof Boolean) {
                field.put("required", fo.get("required"));
            }
            if (fo.get("label") instanceof String) {
                field.put("label", fo.get("label"));
            } else if (key != null && moduleCopy.get(key) instanceof String) {
                field.put("label", moduleCopy.get(key));
            }
            out.add(field);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
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
