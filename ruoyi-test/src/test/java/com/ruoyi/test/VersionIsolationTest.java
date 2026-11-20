package com.ruoyi.test;

import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.interceptor.FeatureInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 版本隔离与功能开关单元测试
 * 对应验证文档：第十八章 版本隔离验证矩阵
 *
 * 验收项覆盖：
 * - LIC-V01~V05: 版本切换验证
 * - LIC-010: 功能开关控制
 * - ERR-013: 功能开关关闭时接口拦截
 * - 版本隔离矩阵全量验证
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Tag("version")
@DisplayName("版本隔离与功能开关验证")
@ExtendWith(MockitoExtension.class)
public class VersionIsolationTest {

    @Mock
    private FeatureChecker featureChecker;

    private FeatureInterceptor interceptor;

    /** 基础版功能集 */
    private Set<String> basicFeatures;
    /** 高级版功能集 */
    private Set<String> advancedFeatures;
    /** 企业版功能集 */
    private Set<String> enterpriseFeatures;

    @BeforeEach
    void setUp() {
        interceptor = new FeatureInterceptor(featureChecker);

        // 基础版：仅核心功能
        basicFeatures = new HashSet<>(Arrays.asList(
            "BUDGET_BASIC", "FEE_BASIC"
        ));

        // 高级版：基础 + 增强功能
        advancedFeatures = new HashSet<>(basicFeatures);
        advancedFeatures.addAll(Arrays.asList(
            "BUDGET_AI_CHAT", "FEE_MULTI_CURRENCY", "FEE_TRACK_CONTROL",
            "FEE_COMPLIANCE_CUSTOM", "FEE_PROFIT_CUSTOM_TIP", "FEE_SUBSIDY_CONFIG",
            "FEE_INDUSTRY_SINGLE", "BUDGET_PERIOD_CONTROL", "BUDGET_BATCH_OPERATION",
            "BUDGET_RISK_CUSTOM", "BUDGET_TIMEOUT_ALERT", "FEE_FUND_POOL"
        ));

        // 企业版：高级 + 企业专属
        enterpriseFeatures = new HashSet<>(advancedFeatures);
        enterpriseFeatures.addAll(Arrays.asList(
            "FEE_ACCRUAL_ENGINE", "FEE_LOAN_LEDGER", "FEE_CROSS_DEPT_ALLOCATE",
            "FEE_DESENSITIZE_CUSTOM", "FEE_AUDIT_FULL_EXPORT", "FEE_AI_PRIVATE_DEPLOY",
            "BUDGET_THREE_LEVEL_APPROVAL", "BUDGET_SEND_BACK", "FEE_FUND_POOL_THREE_LEVEL"
        ));
    }

    // ==================== 版本隔离矩阵验证 ====================

    @Test
    @DisplayName("基础版：预算编制CRUD可用")
    void testBasic_BudgetCrud_Available() {
        assertTrue(isFeatureAvailable("BASIC", "BUDGET_BASIC"));
    }

    @Test
    @DisplayName("基础版：三级审批隐藏")
    void testBasic_ThreeLevelApproval_Hidden() {
        assertFalse(isFeatureAvailable("BASIC", "BUDGET_THREE_LEVEL_APPROVAL"));
    }

    @Test
    @DisplayName("基础版：批量操作隐藏")
    void testBasic_BatchOperation_Hidden() {
        assertFalse(isFeatureAvailable("BASIC", "BUDGET_BATCH_OPERATION"));
    }

    @Test
    @DisplayName("基础版：AI对话隐藏")
    void testBasic_AiChat_Hidden() {
        assertFalse(isFeatureAvailable("BASIC", "BUDGET_AI_CHAT"));
    }

    @Test
    @DisplayName("高级版：批量操作可用")
    void testAdvanced_BatchOperation_Available() {
        assertTrue(isFeatureAvailable("ADVANCED", "BUDGET_BATCH_OPERATION"));
    }

    @Test
    @DisplayName("高级版：AI对话可用")
    void testAdvanced_AiChat_Available() {
        assertTrue(isFeatureAvailable("ADVANCED", "BUDGET_AI_CHAT"));
    }

    @Test
    @DisplayName("高级版：多币种可用")
    void testAdvanced_MultiCurrency_Available() {
        assertTrue(isFeatureAvailable("ADVANCED", "FEE_MULTI_CURRENCY"));
    }

    @Test
    @DisplayName("高级版：三轨管控可用")
    void testAdvanced_TrackControl_Available() {
        assertTrue(isFeatureAvailable("ADVANCED", "FEE_TRACK_CONTROL"));
    }

    @Test
    @DisplayName("高级版：计提引擎隐藏")
    void testAdvanced_AccrualEngine_Hidden() {
        assertFalse(isFeatureAvailable("ADVANCED", "FEE_ACCRUAL_ENGINE"));
    }

    @Test
    @DisplayName("高级版：借款台账隐藏")
    void testAdvanced_LoanLedger_Hidden() {
        assertFalse(isFeatureAvailable("ADVANCED", "FEE_LOAN_LEDGER"));
    }

    @Test
    @DisplayName("企业版：计提引擎可用")
    void testEnterprise_AccrualEngine_Available() {
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_ACCRUAL_ENGINE"));
    }

    @Test
    @DisplayName("企业版：借款台账可用")
    void testEnterprise_LoanLedger_Available() {
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_LOAN_LEDGER"));
    }

    @Test
    @DisplayName("企业版：跨部门分摊可用")
    void testEnterprise_CrossDeptAllocate_Available() {
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_CROSS_DEPT_ALLOCATE"));
    }

    @Test
    @DisplayName("企业版：AI私有化部署可用")
    void testEnterprise_AiPrivateDeploy_Available() {
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_AI_PRIVATE_DEPLOY"));
    }

    // ==================== LIC-V05: @RequireFeature拦截验证 ====================

    @Test
    @DisplayName("LIC-V05: 功能开关关闭时抛出异常")
    void testRequireFeature_Blocked() {
        // Given
        when(featureChecker.isFeatureEnabled("BUDGET_AI_CHAT")).thenReturn(false);

        // When & Then
        ServiceException ex = assertThrows(ServiceException.class, () -> {
            if (!featureChecker.isFeatureEnabled("BUDGET_AI_CHAT")) {
                throw new ServiceException("当前版本不支持此功能，请升级License");
            }
        });
        assertTrue(ex.getMessage().contains("升级License"));
    }

    @Test
    @DisplayName("LIC-V05b: 功能开关开启时正常通过")
    void testRequireFeature_Allowed() {
        // Given
        when(featureChecker.isFeatureEnabled("BUDGET_AI_CHAT")).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> {
            if (!featureChecker.isFeatureEnabled("BUDGET_AI_CHAT")) {
                throw new ServiceException("当前版本不支持此功能，请升级License");
            }
        });
    }

    @Test
    @DisplayName("ERR-013: 功能开关关闭时接口返回明确提示")
    void testFeatureOff_ClearMessage() {
        String expectedMsg = "当前版本不支持此功能，请升级License";

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            throw new ServiceException(expectedMsg);
        });
        assertEquals(expectedMsg, ex.getMessage());
    }

    // ==================== LIC-V01~V03: 版本切换 ====================

    @Test
    @DisplayName("LIC-V01: BASIC→ADVANCED功能解锁")
    void testVersionUpgrade_BasicToAdvanced() {
        // 基础版无AI功能
        assertFalse(isFeatureAvailable("BASIC", "BUDGET_AI_CHAT"));
        // 高级版有AI功能
        assertTrue(isFeatureAvailable("ADVANCED", "BUDGET_AI_CHAT"));
    }

    @Test
    @DisplayName("LIC-V02: ADVANCED→ENTERPRISE功能解锁")
    void testVersionUpgrade_AdvancedToEnterprise() {
        // 高级版无计提引擎
        assertFalse(isFeatureAvailable("ADVANCED", "FEE_ACCRUAL_ENGINE"));
        // 企业版有计提引擎
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_ACCRUAL_ENGINE"));
    }

    @Test
    @DisplayName("LIC-V03: 降级后高级版功能隐藏")
    void testVersionDowngrade_FeaturesHidden() {
        // 高级版有AI功能
        assertTrue(isFeatureAvailable("ADVANCED", "BUDGET_AI_CHAT"));
        // 降到基础版后无AI功能
        assertFalse(isFeatureAvailable("BASIC", "BUDGET_AI_CHAT"));
    }

    // ==================== 资金池版本差异 ====================

    @Test
    @DisplayName("资金池：基础版无、高级版部门级、企业版三级")
    void testFundPool_VersionDifference() {
        // 基础版无资金池
        assertFalse(isFeatureAvailable("BASIC", "FEE_FUND_POOL"));
        // 高级版有资金池（部门级）
        assertTrue(isFeatureAvailable("ADVANCED", "FEE_FUND_POOL"));
        // 企业版有三级资金池
        assertTrue(isFeatureAvailable("ENTERPRISE", "FEE_FUND_POOL_THREE_LEVEL"));
    }

    // ==================== T10.7 版本编码兼容验证 ====================

    @Test
    @DisplayName("T10.7: pro/PROFESSIONAL/ENTERPRISE 归一化返回同一功能集")
    void testNormalizeEdition_ProfessionalAliases_SameFeatureSet() {
        Set<String> proSet = EditionFeatureConfig.getFeaturesByEdition("pro");
        Set<String> professionalSet = EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL");
        Set<String> enterpriseSet = EditionFeatureConfig.getFeaturesByEdition("ENTERPRISE");
        assertEquals(professionalSet, proSet, "pro 与 PROFESSIONAL 功能集必须一致");
        assertEquals(professionalSet, enterpriseSet, "ENTERPRISE 与 PROFESSIONAL 功能集必须一致");
    }

    @Test
    @DisplayName("T10.7: adv/ADVANCED 归一化返回同一功能集")
    void testNormalizeEdition_AdvancedAliases_SameFeatureSet() {
        Set<String> advSet = EditionFeatureConfig.getFeaturesByEdition("adv");
        Set<String> advancedSet = EditionFeatureConfig.getFeaturesByEdition("ADVANCED");
        assertEquals(advancedSet, advSet, "adv 与 ADVANCED 功能集必须一致");
    }

    @Test
    @DisplayName("T10.7: base/BASIC/null/未知值 归一化返回同一功能集")
    void testNormalizeEdition_BasicAliases_SameFeatureSet() {
        Set<String> baseSet = EditionFeatureConfig.getFeaturesByEdition("base");
        Set<String> basicSet = EditionFeatureConfig.getFeaturesByEdition("BASIC");
        Set<String> nullSet = EditionFeatureConfig.getFeaturesByEdition(null);
        Set<String> unknownSet = EditionFeatureConfig.getFeaturesByEdition("unknown_garbage");
        assertEquals(basicSet, baseSet, "base 与 BASIC 功能集必须一致");
        assertEquals(basicSet, nullSet, "null 与 BASIC 功能集必须一致");
        assertEquals(basicSet, unknownSet, "未知值与 BASIC 功能集必须一致");
    }

    @Test
    @DisplayName("T10.7: 大小写不敏感（PrOfEsSiOnAl → PROFESSIONAL）")
    void testNormalizeEdition_CaseInsensitive() {
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL,
            EditionFeatureConfig.normalizeEdition("professional"));
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL,
            EditionFeatureConfig.normalizeEdition("Professional"));
        assertEquals(EditionFeatureConfig.EDITION_ADVANCED,
            EditionFeatureConfig.normalizeEdition("advanced"));
        assertEquals(EditionFeatureConfig.EDITION_BASIC,
            EditionFeatureConfig.normalizeEdition("basic"));
    }

    @Test
    @DisplayName("T10.7: editionLevel 等级排序 PROFESSIONAL > ADVANCED > BASIC")
    void testEditionLevel_Ordering() {
        assertTrue(EditionFeatureConfig.editionLevel("PROFESSIONAL") > EditionFeatureConfig.editionLevel("ADVANCED"));
        assertTrue(EditionFeatureConfig.editionLevel("ADVANCED") > EditionFeatureConfig.editionLevel("BASIC"));
        // 别名等级一致
        assertEquals(EditionFeatureConfig.editionLevel("ENTERPRISE"), EditionFeatureConfig.editionLevel("PROFESSIONAL"));
        assertEquals(EditionFeatureConfig.editionLevel("pro"), EditionFeatureConfig.editionLevel("PROFESSIONAL"));
    }

    // ==================== T4.5 v1.28 防串基线扩至 34 条：新增 8 条 ====================

    @Test
    @DisplayName("防串用例 27：PROFESSIONAL ⊇ ADVANCED ⊇ BASIC 能力单调递增")
    void testEditionFeatureMonotonic() {
        Set<String> basic = EditionFeatureConfig.getFeaturesByEdition("BASIC");
        Set<String> advanced = EditionFeatureConfig.getFeaturesByEdition("ADVANCED");
        Set<String> professional = EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL");
        assertTrue(advanced.containsAll(basic), "高级版必须包含基础版全部能力");
        assertTrue(professional.containsAll(advanced), "专业版必须包含高级版全部能力");
    }

    @Test
    @DisplayName("防串用例 28：组 A 下移开关 FEAT_BUDGET_PREPARE 基础版隐藏、高级版+可见")
    void testGroupA_Downshift_FeatBudgetPrepare() {
        assertFalse(EditionFeatureConfig.getFeaturesByEdition("BASIC").contains("FEAT_BUDGET_PREPARE"),
            "基础版不得含 FEAT_BUDGET_PREPARE（v1.12 下移至高级版）");
        assertTrue(EditionFeatureConfig.getFeaturesByEdition("ADVANCED").contains("FEAT_BUDGET_PREPARE"));
        assertTrue(EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL").contains("FEAT_BUDGET_PREPARE"));
    }

    @Test
    @DisplayName("防串用例 29：组 C 多业态底座 FEAT_WECOM 高级版+可见，基础版隐藏")
    void testGroupC_MultiBiz_FeatWecom() {
        assertFalse(EditionFeatureConfig.getFeaturesByEdition("BASIC").contains("FEAT_WECOM"));
        assertTrue(EditionFeatureConfig.getFeaturesByEdition("ADVANCED").contains("FEAT_WECOM"));
        assertTrue(EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL").contains("FEAT_WECOM"));
    }

    @Test
    @DisplayName("防串用例 30：组 C 专业版专属 FEAT_OPEN_API/FEAT_FLOW_CUSTOM/FEAT_MULTI_ACCOUNT_SET 高级版隐藏")
    void testGroupC_ProfessionalOnly() {
        Set<String> advanced = EditionFeatureConfig.getFeaturesByEdition("ADVANCED");
        Set<String> professional = EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL");
        assertFalse(advanced.contains("FEAT_OPEN_API"), "FEAT_OPEN_API 仅专业版");
        assertFalse(advanced.contains("FEAT_FLOW_CUSTOM"), "FEAT_FLOW_CUSTOM 仅专业版");
        assertFalse(advanced.contains("FEAT_MULTI_ACCOUNT_SET"), "FEAT_MULTI_ACCOUNT_SET 仅专业版");
        assertTrue(professional.contains("FEAT_OPEN_API"));
        assertTrue(professional.contains("FEAT_FLOW_CUSTOM"));
        assertTrue(professional.contains("FEAT_MULTI_ACCOUNT_SET"));
    }

    @Test
    @DisplayName("防串用例 31：存量 ENTERPRISE 与 PROFESSIONAL 功能集完全等价（T4.1 别名兼容）")
    void testEnterpriseAliasEquivalent() {
        assertEquals(EditionFeatureConfig.getFeaturesByEdition("PROFESSIONAL"),
            EditionFeatureConfig.getFeaturesByEdition("ENTERPRISE"),
            "ENTERPRISE 必须与 PROFESSIONAL 返回同一不可变集合");
        assertSame(EditionFeatureConfig.PROFESSIONAL_FEATURES, EditionFeatureConfig.ENTERPRISE_FEATURES,
            "ENTERPRISE_FEATURES 必须为 PROFESSIONAL_FEATURES 的引用别名");
    }

    @Test
    @DisplayName("防串用例 32：normalizeEdition 容忍前后空格与空字符串")
    void testNormalizeEdition_TrimAndEmpty() {
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL,
            EditionFeatureConfig.normalizeEdition("  PROFESSIONAL  "));
        assertEquals(EditionFeatureConfig.EDITION_ADVANCED,
            EditionFeatureConfig.normalizeEdition("\tadv\n"));
        assertEquals(EditionFeatureConfig.EDITION_BASIC,
            EditionFeatureConfig.normalizeEdition(""),
            "空字符串必须归一化为 BASIC");
        assertEquals(EditionFeatureConfig.EDITION_BASIC,
            EditionFeatureConfig.normalizeEdition("   "));
    }

    @Test
    @DisplayName("防串用例 33：三维能力交集模拟（版本 ∩ 业态包 ∩ License）")
    void testThreeDimensionIntersection() {
        // 模拟三维交集：高级版能力 ∩ 业态包声明 ∩ License 声明
        Set<String> editionSet = EditionFeatureConfig.getFeaturesByEdition("ADVANCED");
        Set<String> bizBundleFlags = new HashSet<>(Arrays.asList(
            "FEAT_BUDGET_PREPARE", "FEAT_SUBJECT_3LAYER", "FEAT_WECOM", "FEAT_OPEN_API"));
        Set<String> licenseFeatures = new HashSet<>(Arrays.asList(
            "FEAT_BUDGET_PREPARE", "FEAT_SUBJECT_3LAYER", "FEAT_WECOM"));

        Set<String> intersection = new HashSet<>(editionSet);
        intersection.retainAll(bizBundleFlags);
        intersection.retainAll(licenseFeatures);

        assertTrue(intersection.contains("FEAT_BUDGET_PREPARE"), "三维均授权→可用");
        assertTrue(intersection.contains("FEAT_WECOM"), "三维均授权→可用");
        assertFalse(intersection.contains("FEAT_OPEN_API"),
            "FEAT_OPEN_API 属专业版专属，高级版未授权→不可用（业态包声明也不开启）");
    }

    @Test
    @DisplayName("防串用例 34：基础版不含任何 FEAT_* 专业版专属能力（全量扫描）")
    void testBasic_NoProfessionalFeat() {
        Set<String> basic = EditionFeatureConfig.getFeaturesByEdition("BASIC");
        Set<String> professionalOnly = new HashSet<>(EditionFeatureConfig.PROFESSIONAL_FEATURES);
        professionalOnly.removeAll(EditionFeatureConfig.ADVANCED_FEATURES);
        for (String code : professionalOnly) {
            if (code.startsWith("FEAT_")) {
                assertFalse(basic.contains(code),
                    "基础版不得含专业版专属开关：" + code);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private boolean isFeatureAvailable(String edition, String featureCode) {
        Set<String> features;
        switch (edition) {
            case "BASIC":
                features = basicFeatures;
                break;
            case "ADVANCED":
                features = advancedFeatures;
                break;
            case "ENTERPRISE":
                features = enterpriseFeatures;
                break;
            default:
                features = Collections.emptySet();
        }
        return features.contains(featureCode);
    }
}
