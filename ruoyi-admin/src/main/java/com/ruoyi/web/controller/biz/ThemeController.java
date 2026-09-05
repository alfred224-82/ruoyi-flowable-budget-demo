package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ThemePreviewVo;
import com.ruoyi.system.domain.vo.ThemePresetVo;
import com.ruoyi.system.domain.vo.TenantThemeVo;
import com.ruoyi.system.service.IThemeDecorationService;
import com.ruoyi.system.service.IThemePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 主题装修Controller（T12.2 Design Token / T12.3 素材管线 / T12.5 三层融合）
 * <p>四套主题预设以 CSS 变量下发，PC 与小程序共用；装修保存统一编排内容安全审核与衍生图管线。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/theme")
public class ThemeController extends BaseController {

    private final IThemePresetService themePresetService;
    private final IThemeDecorationService themeDecorationService;

    /**
     * 主题预设列表（四套 Design Token）
     */
    @SaCheckPermission("biz:theme:list")
    @GetMapping("/presets")
    public R<List<ThemePresetVo>> presets() {
        return R.ok(themePresetService.listPresets());
    }

    /**
     * 按 key 取预设 Design Token（CSS 变量键值）
     */
    @SaCheckPermission("biz:theme:query")
    @GetMapping("/tokens/{presetKey}")
    public R<Map<String, String>> tokens(@PathVariable String presetKey) {
        return R.ok(themePresetService.getTokens(presetKey));
    }

    /**
     * 三层融合预览（活动级优先，缺失回落租户级）
     */
    @SaCheckPermission("biz:theme:query")
    @GetMapping("/preview")
    public R<ThemePreviewVo> preview(@RequestParam(required = false) Long activityId) {
        return R.ok(themeDecorationService.preview(activityId));
    }

    /**
     * 装修保存（内容安全审核 → 落库 → 衍生图管线 → 回写成品URL）
     */
    @SaCheckPermission("biz:theme:edit")
    @Log(title = "主题装修", businessType = BusinessType.UPDATE)
    @PostMapping("/decorate")
    public R<Long> decorate(@RequestBody TenantThemeVo vo) {
        return R.ok(themeDecorationService.decorate(vo));
    }
}
