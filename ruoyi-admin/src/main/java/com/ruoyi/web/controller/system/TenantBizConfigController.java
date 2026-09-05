package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.TenantBizConfigVo;
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
}
