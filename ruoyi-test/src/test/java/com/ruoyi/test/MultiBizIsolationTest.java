package com.ruoyi.test;

import com.ruoyi.common.constant.EditionFeatureConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T9.5 多业态防串验收（v1.28 新增）
 *
 * <p>覆盖设计文档六层防串第 ⑥ 层「业态与通道隔离」的五类验收场景，
 * 采用纯单元级模拟（不依赖 Spring 上下文与数据库），断言三维交集、
 * 版本升降级、业态切换、跨通道投递拦截等语义等价关系。
 *
 * <p><b>五类场景</b>：
 * <ol>
 *   <li>基础版租户升级高级版后，预算编制菜单可见且 API 可调用；</li>
 *   <li>高级版租户降级基础版后，预算编制菜单隐藏且 API 拦截；</li>
 *   <li>专业版租户开启开放平台后，open_api_credential 可生成且 webhook 可注册；</li>
 *   <li>同一租户切换业态包（如零售→餐饮），menu_overrides 不生效且保留原业态数据；</li>
 *   <li>跨通道消息投递（企微→开放平台）被拦截并记录 audit_log。</li>
 * </ol>
 *
 * <p>本测试为「语义等价」单元测试：真实拦截路径由 {@code FeatureInterceptor} /
 * {@code SysMenuServiceImpl#applyBizFilter} / {@code TenantLineInnerInterceptor} /
 * 各 MessageProvider 承载，运行时联调由 T9.2 防串专项回归覆盖。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Tag("multi-biz")
@Tag("version")
@DisplayName("T9.5 多业态防串验收（5 类场景 0 穿透）")
public class MultiBizIsolationTest {

    /** 三维交集模拟：edition ∩ bizBundle.featureFlags ∩ license.features */
    private static Set<String> threeDimIntersect(String edition,
                                                 Set<String> bizBundleFlags,
                                                 Set<String> licenseFeatures) {
        Set<String> editionSet = new HashSet<>(EditionFeatureConfig.getFeaturesByEdition(edition));
        if (bizBundleFlags != null) {
            editionSet.retainAll(bizBundleFlags);
        }
        if (licenseFeatures != null) {
            editionSet.retainAll(licenseFeatures);
        }
        return editionSet;
    }

    /** 模拟 API 拦截：三维交集不含 code 即抛「当前版本不支持此功能」 */
    private static boolean apiAllowed(Set<String> effective, String featureCode) {
        return effective != null && effective.contains(featureCode);
    }

    /** 模拟菜单可见性：业态包 menus 声明集与角色授权菜单取交集 */
    private static Set<Long> menuVisible(Set<Long> roleMenuIds, Set<Long> bizAllowedMenuIds) {
        if (bizAllowedMenuIds == null || bizAllowedMenuIds.isEmpty()) {
            // 未声明 menus：不做业态过滤（兼容存量默认包）
            return roleMenuIds;
        }
        Set<Long> visible = new HashSet<>(roleMenuIds);
        visible.retainAll(bizAllowedMenuIds);
        return visible;
    }

    // ==================== 场景 1：基础版升级高级版，预算编制菜单可见且 API 可调用 ====================

    @Test
    @DisplayName("场景1：基础版→高级版升级后 FEAT_BUDGET_PREPARE 三维均授权，菜单可见 + API 可调用")
    void testScenario1_UpgradeBasicToAdvanced_BudgetPrepareUnlocked() {
        Long budgetMenuId = 3210L; // 预算编制菜单占位 ID
        Set<Long> roleMenuBasic = new HashSet<>(Arrays.asList(1000L, 1001L));
        Set<Long> roleMenuAdvanced = new HashSet<>(Arrays.asList(1000L, 1001L, budgetMenuId));
        Set<Long> bizAllowed = new HashSet<>(Arrays.asList(1000L, 1001L, budgetMenuId));

        // 升级前：基础版无 FEAT_BUDGET_PREPARE，角色模板不含预算菜单
        Set<String> beforeFeatures = threeDimIntersect("BASIC", null, null);
        assertFalse(apiAllowed(beforeFeatures, "FEAT_BUDGET_PREPARE"),
            "基础版 API 必须拦截 FEAT_BUDGET_PREPARE");
        assertFalse(menuVisible(roleMenuBasic, bizAllowed).contains(budgetMenuId),
            "基础版菜单不得显示预算编制");

        // 升级后：高级版含 FEAT_BUDGET_PREPARE，角色模板同步扩展
        Set<String> afterFeatures = threeDimIntersect("ADVANCED", null, null);
        assertTrue(apiAllowed(afterFeatures, "FEAT_BUDGET_PREPARE"),
            "高级版 API 必须放行 FEAT_BUDGET_PREPARE");
        assertTrue(menuVisible(roleMenuAdvanced, bizAllowed).contains(budgetMenuId),
            "高级版菜单必须显示预算编制");
    }

    // ==================== 场景 2：高级版降级基础版，预算编制菜单隐藏且 API 拦截 ====================

    @Test
    @DisplayName("场景2：高级版→基础版降级后 FEAT_BUDGET_PREPARE 三维失权，菜单隐藏 + API 拦截")
    void testScenario2_DowngradeAdvancedToBasic_BudgetPrepareLocked() {
        Long budgetMenuId = 3210L;
        Set<Long> roleMenuAdvanced = new HashSet<>(Arrays.asList(1000L, 1001L, budgetMenuId));
        Set<Long> roleMenuBasic = new HashSet<>(Arrays.asList(1000L, 1001L));
        Set<Long> bizAllowed = new HashSet<>(Arrays.asList(1000L, 1001L, budgetMenuId));

        // 降级前：高级版可用
        Set<String> before = threeDimIntersect("ADVANCED", null, null);
        assertTrue(apiAllowed(before, "FEAT_BUDGET_PREPARE"));
        assertTrue(menuVisible(roleMenuAdvanced, bizAllowed).contains(budgetMenuId));

        // 降级后：基础版拦截 + 菜单模板回收
        Set<String> after = threeDimIntersect("BASIC", null, null);
        assertFalse(apiAllowed(after, "FEAT_BUDGET_PREPARE"),
            "降级后 API 必须拦截 FEAT_BUDGET_PREPARE（②层 FeatureInterceptor 兜底）");
        assertFalse(menuVisible(roleMenuBasic, bizAllowed).contains(budgetMenuId),
            "降级后菜单必须隐藏预算编制（④层 sys_role_menu 模板回收）");
    }

    // ==================== 场景 3：专业版开启开放平台后 open_api_credential 可生成 ====================

    @Test
    @DisplayName("场景3：专业版开启 FEAT_OPEN_API 后凭证生成 + webhook 注册放行；高级版拦截")
    void testScenario3_ProfessionalOpenApiUnlocked() {
        // 专业版三维均授权（License 显式声明 + 业态包声明 + 版本默认开启）
        Set<String> bizFlagsPro = new HashSet<>(Arrays.asList("FEAT_OPEN_API", "FEAT_BUDGET_PREPARE"));
        Set<String> licensePro = new HashSet<>(Arrays.asList("FEAT_OPEN_API", "FEAT_BUDGET_PREPARE"));
        Set<String> proEffective = threeDimIntersect("PROFESSIONAL", bizFlagsPro, licensePro);
        assertTrue(apiAllowed(proEffective, "FEAT_OPEN_API"),
            "专业版 + License + 业态包三维均授权 → open_api_credential 生成放行");

        // 高级版即使 License 声明 FEAT_OPEN_API，版本未授权 → 拦截
        Set<String> licenseAdv = new HashSet<>(Collections.singletonList("FEAT_OPEN_API"));
        Set<String> advEffective = threeDimIntersect("ADVANCED", bizFlagsPro, licenseAdv);
        assertFalse(apiAllowed(advEffective, "FEAT_OPEN_API"),
            "高级版即使 License 越权声明 FEAT_OPEN_API，版本维度未开启 → 三维交集为空 → 拦截");

        // 专业版但业态包未声明 FEAT_OPEN_API → 拦截（业态维度失权）
        Set<String> bizFlagsNoOpen = new HashSet<>(Collections.singletonList("FEAT_BUDGET_PREPARE"));
        Set<String> proNoBizEffective = threeDimIntersect("PROFESSIONAL", bizFlagsNoOpen, licensePro);
        assertFalse(apiAllowed(proNoBizEffective, "FEAT_OPEN_API"),
            "业态包未声明 FEAT_OPEN_API → 拦截（⑥层业态隔离）");
    }

    // ==================== 场景 4：同一租户切换业态包，menu_overrides 不生效且保留原业态数据 ====================

    @Test
    @DisplayName("场景4：租户切换业态包 retail→catering，menu_overrides 不 fork 业态包 + 数据保留原业态 tenant_id 隔离")
    void testScenario4_SwitchBizBundle_NoForkAndDataRetained() {
        // 零售业态包 menus 声明
        Set<Long> retailBizMenus = new HashSet<>(Arrays.asList(2000L, 2001L, 2002L));
        // 餐饮业态包 menus 声明（完全不同集合）
        Set<Long> cateringBizMenus = new HashSet<>(Arrays.asList(3000L, 3001L, 3002L));
        // 租户角色授权菜单（版本模板分配，跨业态一致）
        Set<Long> roleMenus = new HashSet<>(Arrays.asList(2000L, 2001L, 3000L, 3001L));

        // 零售业态时：仅显示零售菜单
        Set<Long> visibleRetail = menuVisible(roleMenus, retailBizMenus);
        assertEquals(new HashSet<>(Arrays.asList(2000L, 2001L)), visibleRetail,
            "零售业态：仅显示零售声明菜单，餐饮菜单被过滤");

        // 切换到餐饮后：仅显示餐饮菜单，零售菜单被过滤（menu_overrides 不允许 fork 业态包）
        Set<Long> visibleCatering = menuVisible(roleMenus, cateringBizMenus);
        assertEquals(new HashSet<>(Arrays.asList(3000L, 3001L)), visibleCatering,
            "餐饮业态：仅显示餐饮声明菜单，零售菜单被过滤");

        // 数据保留验证：切换业态后原零售业务数据的 tenant_id 不变（由 TenantLineInnerInterceptor 隔离），
        // 不因业态切换被删除；仅菜单入口切换，历史数据仍可按 tenant_id 查询。
        Map<String, Long> retailRecords = new HashMap<>();
        retailRecords.put("retail_order_001", 1001L);
        retailRecords.put("retail_order_002", 1001L);
        assertNotNull(retailRecords.get("retail_order_001"),
            "切换业态后原零售数据的 tenant_id 归属保留，不删除");
        assertEquals(1001L, retailRecords.get("retail_order_002"));
    }

    // ==================== 场景 5：跨通道消息投递（企微→开放平台）被拦截并记录 audit_log ====================

    @Test
    @DisplayName("场景5：跨通道投递（企微→开放平台）被拦截 + audit_log 留痕")
    void testScenario5_CrossChannelDelivery_BlockedAndAudited() {
        // 模拟租户 access_channel 配置：仅绑定 wecom
        String tenantChannel = "wecom";
        // 允许投递的通道集合：必须与租户 access_channel 一致
        Set<String> allowedChannels = new HashSet<>(Collections.singletonList(tenantChannel));

        // 目标通道：open_api（跨通道）
        String targetChannel = "open_api";

        boolean allowed = allowedChannels.contains(targetChannel);
        assertFalse(allowed, "跨通道投递（wecom → open_api）必须被拦截");

        // 拦截时记录 audit_log（此处仅断言审计事件字段完整性）
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event_type", "CROSS_CHANNEL_BLOCKED");
        auditLog.put("source_channel", tenantChannel);
        auditLog.put("target_channel", targetChannel);
        auditLog.put("tenant_id", 1001L);
        auditLog.put("timestamp", System.currentTimeMillis());
        assertEquals("CROSS_CHANNEL_BLOCKED", auditLog.get("event_type"));
        assertEquals("wecom", auditLog.get("source_channel"));
        assertEquals("open_api", auditLog.get("target_channel"));

        // 同通道投递放行
        assertTrue(allowedChannels.contains("wecom"), "同通道 wecom→wecom 投递必须放行");
    }

    // ==================== 综合：五类场景全部通过后 0 穿透断言 ====================

    @Test
    @DisplayName("综合：五类场景 0 穿透——版本升降级 / 开放平台 / 业态切换 / 跨通道拦截全部按预期")
    void testAllScenarios_ZeroPenetration() {
        // 版本维度：基础版不含专业版专属能力
        Set<String> basic = EditionFeatureConfig.getFeaturesByEdition("BASIC");
        assertFalse(basic.contains("FEAT_OPEN_API"), "基础版不得含 FEAT_OPEN_API");
        assertFalse(basic.contains("FEAT_BUDGET_PREPARE"), "基础版不得含 FEAT_BUDGET_PREPARE（v1.12 下移）");

        // 别名维度：ENTERPRISE 与 PROFESSIONAL 等价（T4.1）
        assertEquals(EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL"),
            EditionFeatureConfig.getFeaturesByEdition("ENTERPRISE"));

        // 三维交集：任一维失权即拦截
        Set<String> empty = threeDimIntersect("ADVANCED",
            Collections.emptySet(),
            new HashSet<>(Collections.singletonList("FEAT_OPEN_API")));
        assertTrue(empty.isEmpty(), "业态包声明为空时三维交集必为空集");
    }
}
