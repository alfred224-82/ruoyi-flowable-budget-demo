package com.ruoyi.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.system.config.LicenseProperties;
import com.ruoyi.system.domain.SysLicense;
import com.ruoyi.system.mapper.SysLicenseMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysTenantVersionLogMapper;
import com.ruoyi.system.service.IBizBundleService;
import com.ruoyi.system.service.IFeatureFlagService;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.impl.LicenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * License授权体系单元测试
 * 对应验证文档：第二章 License授权体系验证（P0）
 *
 * 验收项覆盖：
 * - LIC-001: License加载
 * - LIC-004: 试用授权
 * - LIC-005: 试用到期
 * - LIC-006: 正式授权
 * - LIC-010: 功能开关
 * - LIC-011: 降级不阻断
 * - LIC-012: 替换License
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Tag("license")
@DisplayName("License授权体系验证")
@ExtendWith(MockitoExtension.class)
public class LicenseServiceTest {

    @Mock
    private SysLicenseMapper licenseMapper;

    /** T3.11/T10.8 依赖：LicenseServiceImpl 构造器需要以下 5 个额外依赖，未 mock 会注入 null 导致 NPE */
    @Mock
    private LicenseProperties licenseProperties;

    @Mock
    private SysTenantMapper tenantMapper;

    @Mock
    private SysTenantVersionLogMapper versionLogMapper;

    @Mock
    private IBizBundleService bizBundleService;

    @Mock
    private IFeatureFlagService featureFlagService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LicenseServiceImpl licenseService;

    private SysLicense validLicense;

    @BeforeEach
    void setUp() {
        validLicense = new SysLicense();
        validLicense.setId(1L);
        validLicense.setLicenseId("LIC-2026-000001");
        validLicense.setLicenseType("FORMAL");
        validLicense.setEdition("ENTERPRISE");
        validLicense.setStatus("ACTIVE");
        validLicense.setLicenseVersion(1);
        validLicense.setIssuedAt(new Date());
        validLicense.setFeatures("[\"BUDGET_AI_CHAT\",\"FEE_MULTI_CURRENCY\",\"FEE_TRACK_CONTROL\"]");
    }

    @Test
    @DisplayName("LIC-001: 系统启动时自动加载并验证License")
    void testLoadAndVerify_Success() {
        // Given
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);

        // When
        licenseService.loadAndVerify();

        // Then
        // ★T10.7 归一化：ENTERPRISE 归一化为标准编码 PROFESSIONAL
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL, licenseService.getCurrentEdition());
        assertTrue(licenseService.hasFeature("BUDGET_AI_CHAT"));
        assertTrue(licenseService.hasFeature("FEE_MULTI_CURRENCY"));
        verify(licenseMapper).updateById(any());
    }

    @Test
    @DisplayName("LIC-011: License不存在时降级为BASIC版本，不阻断系统")
    void testLoadAndVerify_NoLicense_DegradeToBasic() {
        // Given
        when(licenseMapper.selectOne(any())).thenReturn(null);

        // When
        licenseService.loadAndVerify();

        // Then
        assertEquals(EditionFeatureConfig.EDITION_BASIC, licenseService.getCurrentEdition());
        assertNotNull(licenseService.getActiveFeatures());
    }

    @Test
    @DisplayName("LIC-005: 试用到期自动降级为BASIC版本")
    void testLoadAndVerify_ExpiredLicense_DegradeToBasic() {
        // Given
        SysLicense expiredLicense = new SysLicense();
        expiredLicense.setId(1L);
        expiredLicense.setEdition("ADVANCED");
        expiredLicense.setStatus("ACTIVE");
        expiredLicense.setLicenseVersion(1);
        // 设置过期时间为昨天
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        expiredLicense.setExpiresAt(cal.getTime());

        when(licenseMapper.selectOne(any())).thenReturn(expiredLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);

        // When
        licenseService.loadAndVerify();

        // Then
        assertEquals(EditionFeatureConfig.EDITION_BASIC, licenseService.getCurrentEdition());
    }

    @Test
    @DisplayName("LIC-006: 正式授权永久使用（expireDate为null）")
    void testLoadAndVerify_FormalLicense_NoExpiry() {
        // Given
        validLicense.setLicenseType("FORMAL");
        validLicense.setExpiresAt(null); // 永久授权
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);

        // When
        licenseService.loadAndVerify();

        // Then
        // ★T10.7 归一化：ENTERPRISE 归一化为标准编码 PROFESSIONAL
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL, licenseService.getCurrentEdition());
    }

    @Test
    @DisplayName("LIC-010: 功能开关正确控制功能可见性")
    void testHasFeature_CorrectlyControlsAccess() {
        // Given
        // ★T3.11 并集策略：activeFeatures = 版本默认 ∪ features JSON。
        // PROFESSIONAL 默认集已含全部功能，无法观察“未授权”分支；
        // 本用例验证 features JSON 白名单裁剪效果，需以 BASIC（空默认集）为基线。
        validLicense.setEdition(EditionFeatureConfig.EDITION_BASIC);
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // Then - 已授权功能（features JSON 中声明）
        assertTrue(licenseService.hasFeature("BUDGET_AI_CHAT"));
        assertTrue(licenseService.hasFeature("FEE_MULTI_CURRENCY"));
        assertTrue(licenseService.hasFeature("FEE_TRACK_CONTROL"));

        // Then - 未授权功能（不在 BASIC 默认集且未在 features JSON 中声明）
        assertFalse(licenseService.hasFeature("FEE_ACCRUAL_ENGINE"));
        assertFalse(licenseService.hasFeature("NON_EXISTENT_FEATURE"));
    }

    @Test
    @DisplayName("LIC-004: 试用授权在限定天数内正常使用")
    void testTrialRemainingHours_WithinTrialPeriod() {
        // Given
        SysLicense trialLicense = new SysLicense();
        trialLicense.setLicenseType("TRIAL");
        trialLicense.setTrialDays(30);
        trialLicense.setActivatedAt(new Date()); // 刚激活
        trialLicense.setEdition("ADVANCED");
        trialLicense.setStatus("ACTIVE");
        trialLicense.setLicenseVersion(1);

        when(licenseMapper.selectOne(any())).thenReturn(trialLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // When
        long remainingHours = licenseService.getTrialRemainingHours();

        // Then - 应该还有接近30天（720小时）
        assertTrue(remainingHours > 700);
        assertTrue(remainingHours <= 720);
    }

    @Test
    @DisplayName("LIC-004b: 非试用类型返回-1")
    void testTrialRemainingHours_FormalLicense_ReturnsNegative() {
        // Given
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // When
        long remainingHours = licenseService.getTrialRemainingHours();

        // Then
        assertEquals(-1, remainingHours);
    }

    @Test
    @DisplayName("LIC-012: 替换License后功能即时解锁")
    void testReplaceLicense_UpdatesFeatures() {
        // Given
        // ★T3.11 并集策略：初始设为 BASIC 且 features 为空，才能观察替换后“解锁”的因果关系。
        // 若初始为 ENTERPRISE/PROFESSIONAL，默认集已含 FEE_ACCRUAL_ENGINE，assertFalse 不成立。
        validLicense.setEdition(EditionFeatureConfig.EDITION_BASIC);
        validLicense.setFeatures("[]");
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // 初始状态：BASIC + 空 features → 无 FEE_ACCRUAL_ENGINE
        assertFalse(licenseService.hasFeature("FEE_ACCRUAL_ENGINE"));

        // When - 替换为包含新功能的License（ENTERPRISE 归一化为 PROFESSIONAL）
        String newLicenseJson = "{\"edition\":\"ENTERPRISE\",\"features\":\"[\\\"FEE_ACCRUAL_ENGINE\\\",\\\"FEE_LOAN_LEDGER\\\"]\"}";
        licenseService.replaceLicense(newLicenseJson);

        // Then - PROFESSIONAL 默认集 ∪ features JSON 均含下列开关
        assertTrue(licenseService.hasFeature("FEE_ACCRUAL_ENGINE"));
        assertTrue(licenseService.hasFeature("FEE_LOAN_LEDGER"));
    }

    @Test
    @DisplayName("LIC-A02: 导出机器指纹")
    void testExportFingerprint() {
        // When
        String fingerprint = licenseService.exportFingerprint();

        // Then
        assertNotNull(fingerprint);
        assertTrue(fingerprint.startsWith("FP-"));
    }

    @Test
    @DisplayName("LIC-A01: 获取当前授权信息")
    void testGetLicenseInfo() {
        // Given
        when(licenseMapper.selectOne(any())).thenReturn(validLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // When
        ILicenseService.LicenseInfoVo info = licenseService.getLicenseInfo();

        // Then
        assertNotNull(info);
        // ★T10.7 归一化：ENTERPRISE 归一化为标准编码 PROFESSIONAL
        assertEquals(EditionFeatureConfig.EDITION_PROFESSIONAL, info.getEdition());
        assertEquals("LIC-2026-000001", info.getLicenseId());
        assertEquals("FORMAL", info.getLicenseType());
        assertNotNull(info.getFeatures());
    }

    @Test
    @DisplayName("LIC-V01: BASIC→ADVANCED版本切换")
    void testVersionSwitch_BasicToAdvanced() {
        // Given - 初始为BASIC
        when(licenseMapper.selectOne(any())).thenReturn(null);
        licenseService.loadAndVerify();
        assertEquals(EditionFeatureConfig.EDITION_BASIC, licenseService.getCurrentEdition());

        // When - 切换到ADVANCED
        SysLicense advancedLicense = new SysLicense();
        advancedLicense.setEdition("ADVANCED");
        advancedLicense.setStatus("ACTIVE");
        advancedLicense.setLicenseVersion(2);
        advancedLicense.setFeatures("[\"BUDGET_AI_CHAT\",\"FEE_MULTI_CURRENCY\"]");
        when(licenseMapper.selectOne(any())).thenReturn(advancedLicense);
        when(licenseMapper.updateById(any())).thenReturn(1);
        licenseService.loadAndVerify();

        // Then
        assertEquals("ADVANCED", licenseService.getCurrentEdition());
    }
}
