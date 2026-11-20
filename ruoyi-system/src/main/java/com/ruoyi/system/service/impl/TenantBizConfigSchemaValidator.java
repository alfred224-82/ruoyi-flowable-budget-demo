package com.ruoyi.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户单店覆盖 module_overrides JSON Schema 校验器（T10.4 v1.29 缺口封闭）
 *
 * <p><b>Schema 规范（v1.29）</b>：{@code module_overrides} 必须为 JSON Object，顶层允许以下键：
 * <pre>
 * {
 *   "modules": {                       // 可选：模块级覆盖（key = 模块编码，与 biz_bundle.copy 或功能开关 code 对齐）
 *     "&lt;module_code&gt;": {
 *       "enabled": true|false,         // 可选：模块启停（false=隐藏入口 + 接口拦截）
 *       "copy": {                      // 可选：模块内文案覆盖（key→value 均为字符串）
 *         "&lt;i18n_key&gt;": "&lt;文案&gt;"
 *       },
 *       "fields": {                    // 可选：字段可见性/必填覆盖
 *         "&lt;field_name&gt;": {
 *           "visible": true|false,
 *           "required": true|false,
 *           "label": "&lt;字段标签&gt;"      // 可选：字段标签文案覆盖
 *         }
 *       }
 *     }
 *   },
 *   "global": {                        // 可选：全局覆盖（非模块级）
 *     "copy": { "&lt;i18n_key&gt;": "&lt;文案&gt;" },
 *     "theme": "&lt;preset_key&gt;"          // 可选：主题预设（与 tenant_theme.preset_key 对齐）
 *   }
 * }
 * </pre>
 *
 * <p><b>硬约束</b>：
 * <ol>
 *   <li>顶层必须为 Object，禁止 Array/String/Number/Boolean/null</li>
 *   <li>顶层键必须在 {@link #ALLOWED_TOP_LEVEL_KEYS} 白名单内，禁止未知键（防止 fork 业态包）</li>
 *   <li>{@code modules} / {@code global} 若存在必须为 Object</li>
 *   <li>{@code enabled} / {@code visible} / {@code required} 若存在必须为 Boolean</li>
 *   <li>{@code copy} 若存在必须为 Object，且 value 必须为 String（防止结构化数据混入文案）</li>
 *   <li>{@code fields} 若存在必须为 Object，且每项 value 必须为 Object</li>
 *   <li>{@code theme} 若存在必须为 String</li>
 *   <li>模块编码长度限制 1~60，禁止空白字符</li>
 * </ol>
 *
 * <p><b>校验失败</b>：抛 {@link IllegalArgumentException}，错误信息包含全部违规路径（一次暴露所有问题，便于运维定位）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantBizConfigSchemaValidator {

    /** 顶层允许的键（白名单，禁止未知键防止 fork 业态包） */
    public static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("modules", "global")));

    /** modules.&lt;code&gt; 下允许的键 */
    public static final Set<String> ALLOWED_MODULE_KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("enabled", "copy", "fields")));

    /** global 下允许的键 */
    public static final Set<String> ALLOWED_GLOBAL_KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("copy", "theme")));

    /** modules.&lt;code&gt;.fields.&lt;name&gt; 下允许的键 */
    public static final Set<String> ALLOWED_FIELD_KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("visible", "required", "label")));

    /** 模块编码最大长度 */
    private static final int MAX_MODULE_CODE_LENGTH = 60;

    private final ObjectMapper objectMapper;

    /**
     * 校验 module_overrides JSON 是否符合 Schema。
     *
     * @param json 待校验 JSON 字符串（null/空白视为无覆盖，直接通过）
     * @throws IllegalArgumentException JSON 非法或违反 Schema 约束
     */
    public void validate(String json) {
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("module_overrides JSON 解析失败: " + e.getMessage());
        }
        if (root == null || root.isNull()) {
            return;
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("module_overrides 顶层必须为 JSON Object，实际为 " + root.getNodeType());
        }
        List<String> errors = new ArrayList<>();
        validateTopLevelKeys(root, errors);
        validateModules(root.get("modules"), errors);
        validateGlobal(root.get("global"), errors);
        if (!errors.isEmpty()) {
            String msg = "module_overrides Schema 校验失败（共 " + errors.size() + " 处违规）:\n  - "
                + String.join("\n  - ", errors);
            log.warn("[TenantBizConfigSchema] {}", msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * 返回 Schema 描述（供前端展示与文档同步）
     */
    public Map<String, Object> describeSchema() {
        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("version", "v1.29");
        schema.put("type", "object");
        schema.put("allowedTopLevelKeys", ALLOWED_TOP_LEVEL_KEYS);
        schema.put("moduleKeys", ALLOWED_MODULE_KEYS);
        schema.put("globalKeys", ALLOWED_GLOBAL_KEYS);
        schema.put("fieldKeys", ALLOWED_FIELD_KEYS);
        schema.put("maxModuleCodeLength", MAX_MODULE_CODE_LENGTH);
        return schema;
    }

    // ==================== 内部校验方法 ====================

    private void validateTopLevelKeys(JsonNode root, List<String> errors) {
        Iterator<String> it = root.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            if (!ALLOWED_TOP_LEVEL_KEYS.contains(key)) {
                errors.add("顶层键 '" + key + "' 不在白名单内（允许: " + ALLOWED_TOP_LEVEL_KEYS + "）");
            }
        }
    }

    private void validateModules(JsonNode modules, List<String> errors) {
        if (modules == null || modules.isNull()) {
            return;
        }
        if (!modules.isObject()) {
            errors.add("modules 必须为 Object，实际为 " + modules.getNodeType());
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = modules.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String code = entry.getKey();
            JsonNode body = entry.getValue();
            String path = "modules." + code;
            if (code == null || code.trim().isEmpty()) {
                errors.add(path + ": 模块编码不能为空");
                continue;
            }
            if (code.length() > MAX_MODULE_CODE_LENGTH) {
                errors.add(path + ": 模块编码长度超限（>" + MAX_MODULE_CODE_LENGTH + "）");
            }
            if (code.contains(" ") || code.contains("\t") || code.contains("\n")) {
                errors.add(path + ": 模块编码禁止包含空白字符");
            }
            if (!body.isObject()) {
                errors.add(path + ": 模块体必须为 Object，实际为 " + body.getNodeType());
                continue;
            }
            validateModuleBody(path, body, errors);
        }
    }

    private void validateModuleBody(String path, JsonNode body, List<String> errors) {
        Iterator<String> it = body.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            if (!ALLOWED_MODULE_KEYS.contains(key)) {
                errors.add(path + "." + key + ": 键不在白名单内（允许: " + ALLOWED_MODULE_KEYS + "）");
            }
        }
        JsonNode enabled = body.get("enabled");
        if (enabled != null && !enabled.isNull() && !enabled.isBoolean()) {
            errors.add(path + ".enabled: 必须为 Boolean，实际为 " + enabled.getNodeType());
        }
        validateCopyMap(path + ".copy", body.get("copy"), errors);
        validateFields(path + ".fields", body.get("fields"), errors);
    }

    private void validateFields(String path, JsonNode fields, List<String> errors) {
        if (fields == null || fields.isNull()) {
            return;
        }
        if (!fields.isObject()) {
            errors.add(path + ": 必须为 Object，实际为 " + fields.getNodeType());
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = fields.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            JsonNode spec = entry.getValue();
            String fp = path + "." + name;
            if (!spec.isObject()) {
                errors.add(fp + ": 字段配置必须为 Object，实际为 " + spec.getNodeType());
                continue;
            }
            Iterator<String> keys = spec.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!ALLOWED_FIELD_KEYS.contains(key)) {
                    errors.add(fp + "." + key + ": 键不在白名单内（允许: " + ALLOWED_FIELD_KEYS + "）");
                }
            }
            JsonNode visible = spec.get("visible");
            if (visible != null && !visible.isNull() && !visible.isBoolean()) {
                errors.add(fp + ".visible: 必须为 Boolean");
            }
            JsonNode required = spec.get("required");
            if (required != null && !required.isNull() && !required.isBoolean()) {
                errors.add(fp + ".required: 必须为 Boolean");
            }
            JsonNode label = spec.get("label");
            if (label != null && !label.isNull() && !label.isTextual()) {
                errors.add(fp + ".label: 必须为 String");
            }
        }
    }

    private void validateGlobal(JsonNode global, List<String> errors) {
        if (global == null || global.isNull()) {
            return;
        }
        if (!global.isObject()) {
            errors.add("global 必须为 Object，实际为 " + global.getNodeType());
            return;
        }
        Iterator<String> it = global.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            if (!ALLOWED_GLOBAL_KEYS.contains(key)) {
                errors.add("global." + key + ": 键不在白名单内（允许: " + ALLOWED_GLOBAL_KEYS + "）");
            }
        }
        validateCopyMap("global.copy", global.get("copy"), errors);
        JsonNode theme = global.get("theme");
        if (theme != null && !theme.isNull() && !theme.isTextual()) {
            errors.add("global.theme: 必须为 String");
        }
    }

    private void validateCopyMap(String path, JsonNode copy, List<String> errors) {
        if (copy == null || copy.isNull()) {
            return;
        }
        if (!copy.isObject()) {
            errors.add(path + ": 必须为 Object，实际为 " + copy.getNodeType());
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = copy.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode val = entry.getValue();
            if (key == null || key.trim().isEmpty()) {
                errors.add(path + ": 文案 key 不能为空");
                continue;
            }
            if (val == null || val.isNull()) {
                continue; // 允许显式 null（表示删除覆盖，回落业态包默认）
            }
            if (!val.isTextual()) {
                errors.add(path + "." + key + ": 文案 value 必须为 String，实际为 " + val.getNodeType());
            }
        }
    }
}
