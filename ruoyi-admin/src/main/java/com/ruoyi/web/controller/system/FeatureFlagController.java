package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeatureFlagVo;
import com.ruoyi.system.service.IFeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 功能开关管理Controller（T10.3 表驱动开关）
 * <p>feature_flag 为系统级共享表（无 tenant_id），仅平台超管维护；可启停开关（status），
 * 【不可】改版本归属（basic/advanced/pro_enabled）以免串版本。启停即时生效（缓存刷新）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/featureFlag")
public class FeatureFlagController extends BaseController {

    private final IFeatureFlagService featureFlagService;

    /**
     * 开关列表（33 项，按分组/编码排序）
     */
    @SaCheckPermission("system:featureFlag:list")
    @GetMapping("/list")
    public R<List<FeatureFlagVo>> list() {
        return R.ok(featureFlagService.listFlags());
    }

    /**
     * 查询指定版本的默认开启开关集（表驱动 FEAT_* ∪ 常量兜底 legacy）
     */
    @SaCheckPermission("system:featureFlag:list")
    @GetMapping("/edition/{edition}")
    public R<Set<String>> editionFeatures(@PathVariable String edition) {
        return R.ok(featureFlagService.getEditionFeatureSet(edition));
    }

    /**
     * 启停开关（仅改 status，不改版本归属）
     */
    @SaCheckPermission("system:featureFlag:edit")
    @Log(title = "功能开关管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return toAjax(featureFlagService.toggleStatus(id, status));
    }

    /**
     * 手动失效开关缓存
     */
    @SaCheckPermission("system:featureFlag:edit")
    @Log(title = "功能开关管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/cache")
    public R<Void> evictCache() {
        featureFlagService.evictCache();
        return R.ok();
    }
}
