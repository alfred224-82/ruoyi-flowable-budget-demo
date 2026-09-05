package com.ruoyi.web.controller.system;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.ITenantBizConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 版本能力下发Controller（防串第③层：前端动态渲染，T4.3/T8.5/T10.8）
 * <p>返回当前租户的三维能力：{ edition, bizCode, accessChannel, features, copy }——
 * features 已由 {@code LicenseServiceImpl#getTenantActiveFeatures} 按
 * {@code feature_flag ∩ biz_bundle ∩ License} 三维交集计算；copy 为业态包默认文案 ∪ 租户单店覆盖（T10.4）。
 * 前端（PC/小程序）按 features 隐藏未解锁入口、按 copy 渲染业态文案；后端 {@code @RequireFeature} + FeatureInterceptor 仍为最终防线。
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/edition")
public class SysEditionController {

    private final ILicenseService licenseService;
    private final ISysTenantService tenantService;
    private final ITenantBizConfigService tenantBizConfigService;

    /** 默认业态包编码（与 M1_06 存量回填一致） */
    private static final String DEFAULT_BIZ_CODE = "fund_control";
    /** 默认访问通道 */
    private static final String DEFAULT_ACCESS_CHANNEL = "wechat";

    /**
     * 获取当前租户三维能力（登录即可访问，小程序登录后立即调用）
     *
     * @return { edition, bizCode, accessChannel, features, copy }
     */
    @GetMapping("/features")
    public R<Map<String, Object>> features() {
        Map<String, Object> result = new HashMap<>(8);
        // admin 超管功能开关免检（与 FeatureInterceptor 豁免策略一致），按全量专业版 + 默认业态下发
        if (LoginHelper.isAdmin()) {
            result.put("edition", EditionFeatureConfig.EDITION_PROFESSIONAL);
            result.put("bizCode", DEFAULT_BIZ_CODE);
            result.put("accessChannel", DEFAULT_ACCESS_CHANNEL);
            result.put("features", EditionFeatureConfig.PROFESSIONAL_FEATURES);
            result.put("copy", tenantBizConfigService.getMergedOverrides(null, DEFAULT_BIZ_CODE));
            return R.ok(result);
        }
        Long tenantId = LoginHelper.getTenantId();
        // 读取租户业态包与访问通道（T10.1）
        String bizCode = DEFAULT_BIZ_CODE;
        String accessChannel = DEFAULT_ACCESS_CHANNEL;
        if (tenantId != null) {
            SysTenantVo tenant = tenantService.queryById(tenantId);
            if (tenant != null) {
                bizCode = StrUtil.isNotBlank(tenant.getBizCode()) ? tenant.getBizCode() : DEFAULT_BIZ_CODE;
                accessChannel = StrUtil.isNotBlank(tenant.getAccessChannel()) ? tenant.getAccessChannel() : DEFAULT_ACCESS_CHANNEL;
            }
        }
        result.put("edition", licenseService.getCurrentEdition());
        result.put("bizCode", bizCode);
        result.put("accessChannel", accessChannel);
        // 三维交集后的有效功能集（T10.8）
        Set<String> features = licenseService.getActiveFeatures();
        result.put("features", features);
        // 业态包默认文案 ∪ 租户单店覆盖（T10.4/T10.5）
        result.put("copy", tenantBizConfigService.getMergedOverrides(tenantId, bizCode));
        return R.ok(result);
    }
}
