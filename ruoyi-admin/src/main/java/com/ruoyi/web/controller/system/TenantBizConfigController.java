package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.TenantBizConfigVo;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.ITenantBizConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 租户业态单店覆盖Controller（T10.4）
 * <p>module_overrides JSON 覆盖模块开关/文案/字段可见性；合并优先级 = 租户覆盖 &gt; 业态包默认。
 * 由平台超管按 tenantId 维护（admin 免租户过滤），禁止 fork 业态包。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/tenantBizConfig")
public class TenantBizConfigController extends BaseController {

    private final ITenantBizConfigService tenantBizConfigService;
    private final ILicenseService licenseService;

    /**
     * 查询租户单店覆盖配置
     */
    @SaCheckPermission("system:tenantBizConfig:query")
    @GetMapping("/{tenantId}")
    public R<TenantBizConfigVo> getConfig(@PathVariable Long tenantId) {
        return R.ok(tenantBizConfigService.getConfig(tenantId));
    }

    /**
     * 查询合并后的模块覆盖（业态包默认文案 ∪ 租户覆盖，租户优先）
     */
    @SaCheckPermission("system:tenantBizConfig:query")
    @GetMapping("/{tenantId}/merged")
    public R<Map<String, Object>> getMerged(@PathVariable Long tenantId,
                                            @RequestParam(required = false) String bizCode) {
        return R.ok(tenantBizConfigService.getMergedOverrides(tenantId, bizCode));
    }

    /**
     * 保存租户单店覆盖配置（JSON 结构校验）
     */
    @SaCheckPermission("system:tenantBizConfig:edit")
    @Log(title = "租户单店覆盖", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody TenantBizConfigVo vo) {
        return toAjax(tenantBizConfigService.saveConfig(vo));
    }

    /**
     * ★T10.4 v1.29：获取 module_overrides 的 JSON Schema 描述（供前端管理页展示约束）
     */
    @SaCheckPermission("system:tenantBizConfig:query")
    @GetMapping("/schema")
    public R<Map<String, Object>> schema() {
        return R.ok(tenantBizConfigService.describeSchema());
    }

    /**
     * ★T10.6 v1.29：获取当前登录租户的运行时渲染 Schema（Schema 页面引擎数据源）
     * <p>任意登录用户可访问（无特殊权限），后端自动解析当前租户上下文 + 业态包，
     * 前端 SchemaRenderer 组件消费 blocks 声明式渲染。
     */
    @GetMapping("/render")
    public R<Map<String, Object>> render() {
        Long tenantId = LoginHelper.isAdmin() ? null : LoginHelper.getTenantId();
        String bizCode = licenseService.getCurrentBizCode();
        return R.ok(tenantBizConfigService.getRenderSchema(tenantId, bizCode));
    }
}
