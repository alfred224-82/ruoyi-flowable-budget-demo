package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.TenantBizConfigVo;

import java.util.Map;

/**
 * 租户业态单店覆盖服务（T10.4）
 * <p>{@code module_overrides} JSON 覆盖模块开关/文案/字段可见性；合并优先级 = 租户覆盖 &gt; 业态包默认。
 * <b>禁止 fork 业态包</b>：单店差异一律走本表（设计文档 1.8.5 / 附录第 8 条）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ITenantBizConfigService {

    /**
     * 获取租户单店覆盖配置
     *
     * @param tenantId 租户ID
     * @return 覆盖配置 VO，无覆盖时返回 null
     */
    TenantBizConfigVo getConfig(Long tenantId);

    /**
     * 获取合并后的模块覆盖（业态包默认文案 ∪ 租户 module_overrides，租户覆盖优先）
     *
     * @param tenantId 租户ID
     * @param bizCode  业态编码（用于取业态包默认文案）
     * @return 合并后的键值映射，无覆盖时返回业态包默认文案
     */
    Map<String, Object> getMergedOverrides(Long tenantId, String bizCode);

    /**
     * 保存单店覆盖（JSON 结构校验 + 失效缓存）
     *
     * @param vo 覆盖配置（tenantId + moduleOverrides JSON）
     * @return 影响行数
     */
    int saveConfig(TenantBizConfigVo vo);

    /**
     * ★T10.4 v1.29：获取 module_overrides 的 JSON Schema 描述
     * <p>供前端管理页展示约束（白名单/类型/长度）与文档同步。
     *
     * @return Schema 描述 Map（version/type/allowedTopLevelKeys/moduleKeys/globalKeys/fieldKeys/maxModuleCodeLength）
     */
    Map<String, Object> describeSchema();

    /**
     * ★T10.6 v1.29：获取运行时渲染 Schema（Schema 页面引擎后端解析）
     * <p>合并优先级：业态包 schema_blocks 为基底 → 租户 module_overrides 叠加。
     * 解析规则：
     * <ol>
     *   <li>modules.&lt;code&gt;.enabled=false → 剔除对应区块（module 对齐 block.module）</li>
     *   <li>modules.&lt;code&gt;.copy.title → 覆盖区块标题；copy.&lt;fieldKey&gt; → 覆盖字段标签</li>
     *   <li>modules.&lt;code&gt;.fields.&lt;key&gt;.{visible,required,label} → 覆盖字段属性</li>
     *   <li>global.theme → 下发主题预设；global.copy → 区块标题兜底文案</li>
     * </ol>
     *
     * @param tenantId 租户ID（null=平台默认，不叠加单店覆盖）
     * @param bizCode  业态编码（null/空白回落默认业态包）
     * @return 渲染 Schema：{ bizCode, theme, blocks:[已解析区块] }
     */
    Map<String, Object> getRenderSchema(Long tenantId, String bizCode);
}
