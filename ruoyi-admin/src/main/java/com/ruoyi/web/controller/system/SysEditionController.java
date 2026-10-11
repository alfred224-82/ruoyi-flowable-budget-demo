package com.ruoyi.web.controller.system;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.IndustryTemplateVo;
import com.ruoyi.system.domain.vo.RoleTemplateVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.IIndustryTemplateService;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.IRoleTemplateService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.ITenantBizConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final IIndustryTemplateService industryTemplateService;
    private final IRoleTemplateService roleTemplateService;

    /** 默认业态包编码（与 M1_06 存量回填一致） */
    private static final String DEFAULT_BIZ_CODE = "fund_control";
    /** 默认访问通道 */
    private static final String DEFAULT_ACCESS_CHANNEL = "wechat";
    /** 小微版产品线（tenant_type） */
    private static final String TENANT_TYPE_MICRO = "micro";
    /** 企业版产品线（tenant_type） */
    private static final String TENANT_TYPE_ENTERPRISE = "enterprise";
    /** 产品线→可选版本档位（value→label），级联顺序：选产品线后据此渲染版本档位下拉 */
    private static final Map<String, Map<String, String>> VERSION_OPTIONS;

    static {
        Map<String, Map<String, String>> options = new LinkedHashMap<>();
        Map<String, String> micro = new LinkedHashMap<>();
        micro.put("micro_lite", "小微版·推广档(免费)");
        micro.put("micro_pro", "小微版·经营档(收费)");
        options.put(TENANT_TYPE_MICRO, micro);
        Map<String, String> enterprise = new LinkedHashMap<>();
        enterprise.put("basic", "企业版·基础版");
        enterprise.put("advanced", "企业版·高级版");
        enterprise.put("pro", "企业版·专业版");
        options.put(TENANT_TYPE_ENTERPRISE, enterprise);
        VERSION_OPTIONS = Collections.unmodifiableMap(options);
    }

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

    /**
     * 级联①版本档位选项（T15.29，新建/编辑租户表单，perms 复用 system:tenant:list）
     * <p>按产品线(tenant_type)返回可选 version_type 列表：micro→[micro_lite,micro_pro]；enterprise→[basic,advanced,pro]。
     *
     * @param tenantType 产品线：micro(小微版)/enterprise(企业版)
     * @return [{value, label}] 有序列表
     */
    @GetMapping("/version-options")
    public R<List<Map<String, String>>> versionOptions(@RequestParam String tenantType) {
        Map<String, String> tier = VERSION_OPTIONS.get(StrUtil.trimToEmpty(tenantType));
        if (tier == null) {
            return R.ok(Collections.emptyList());
        }
        List<Map<String, String>> list = new ArrayList<>(tier.size());
        tier.forEach((value, label) -> {
            Map<String, String> item = new HashMap<>(4);
            item.put("value", value);
            item.put("label", label);
            list.add(item);
        });
        return R.ok(list);
    }

    /**
     * 级联②行业模板选项（T15.29，小微版专属）
     * <p>选定版本档位后返回可选行业模板（来自 industry_template，仅 status=0 启用项）。
     * 企业版无行业模板维度，非 micro 产品线直接返回空列表。
     *
     * @param tenantType 产品线，仅 micro 返回行业模板
     * @return [{value(industryCode), label(industryName), icon}]
     */
    @GetMapping("/industry-options")
    public R<List<Map<String, String>>> industryOptions(@RequestParam String tenantType) {
        if (!TENANT_TYPE_MICRO.equals(StrUtil.trimToEmpty(tenantType))) {
            return R.ok(Collections.emptyList());
        }
        List<IndustryTemplateVo> templates = industryTemplateService.listEnabled();
        List<Map<String, String>> list = new ArrayList<>(templates.size());
        for (IndustryTemplateVo vo : templates) {
            Map<String, String> item = new HashMap<>(4);
            item.put("value", vo.getIndustryCode());
            item.put("label", vo.getIndustryName());
            item.put("icon", vo.getIcon());
            list.add(item);
        }
        return R.ok(list);
    }

    /**
     * 级联③默认角色配方预览（T15.29，选定版本档位后展示该档位将预置的角色 + 菜单数）
     * <p>数据源 role_template（version_type × role_code）：返回角色编码 + menu_keys 数量，供 admin 建租户前预览。
     *
     * @param versionType 版本档位：micro_lite/micro_pro/basic/advanced/pro
     * @return [{roleCode, menuCount, blockCount}]
     */
    @GetMapping("/default-role-preview")
    public R<List<Map<String, Object>>> defaultRolePreview(@RequestParam String versionType) {
        List<RoleTemplateVo> templates = roleTemplateService.listByVersion(StrUtil.trimToEmpty(versionType));
        List<Map<String, Object>> list = new ArrayList<>(templates.size());
        for (RoleTemplateVo vo : templates) {
            Map<String, Object> item = new HashMap<>(4);
            item.put("roleCode", vo.getRoleCode());
            item.put("menuCount", countJsonArray(vo.getMenuKeys()));
            item.put("blockCount", countJsonArray(vo.getBlockKeys()));
            list.add(item);
        }
        return R.ok(list);
    }

    /** 统计 JSON 数组字符串的元素个数（menu_keys/block_keys 为 perms/block_key 数组） */
    private int countJsonArray(String json) {
        if (StrUtil.isBlank(json) || !cn.hutool.json.JSONUtil.isTypeJSONArray(json)) {
            return 0;
        }
        try {
            return cn.hutool.json.JSONUtil.parseArray(json).size();
        } catch (Exception e) {
            return 0;
        }
    }
}
