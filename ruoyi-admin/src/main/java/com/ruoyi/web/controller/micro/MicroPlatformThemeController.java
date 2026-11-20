package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * platform_admin 小程序主题管理Controller（T15.36，租户管理员四件套之四）
 * <p>platform_admin 在 PC 端选择小程序全局主题（默认 4 套），选定后写入 sys_tenant.app_theme_key，
 * 小程序端下次冷启动读取最新主题配置。
 * <p>perms：micro:platform:theme（M15_17 种子菜单）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/theme")
public class MicroPlatformThemeController extends BaseController {

    private final SysTenantMapper tenantMapper;

    /**
     * 预设主题色板（4 套，M27 T27.1 命名对齐 V3.9 §四）：
     * theme_fresh 清新简约风 / theme_vivid 活力渐变风 / theme_dark 暗黑科技风 / theme_handdrawn 温暖手绘风。
     * 每套含主色 + 页面底色 + 渐变起止色 + 图标风格 + 圆角 + 深色标记 + 文字色（暗黑科技风反色）。
     * 小程序端欢迎区/封面占位按渐变渲染，链接与数字按主色渲染。
     */
    private static final List<Map<String, String>> PRESET_THEMES = new ArrayList<>();
    /** 默认主题 key（T27.1：旧 theme_default 已迁移为 theme_fresh 清新简约风） */
    private static final String DEFAULT_THEME_KEY = "theme_fresh";
    static {
        // 清新简约风（浅色圆角 + 线性图标）
        Map<String, String> freshTheme = new LinkedHashMap<>();
        freshTheme.put("key", "theme_fresh");
        freshTheme.put("name", "清新简约风");
        freshTheme.put("primaryColor", "#4A90E2");
        freshTheme.put("bgColor", "#F5F9FF");
        freshTheme.put("gradientStart", "#E3F2FD");
        freshTheme.put("gradientEnd", "#FFFFFF");
        freshTheme.put("iconStyle", "linear");
        freshTheme.put("radius", "8px");
        freshTheme.put("isDark", "false");
        freshTheme.put("textPrimary", "#1A1A1A");
        freshTheme.put("textSecondary", "#666666");
        PRESET_THEMES.add(freshTheme);

        // 活力渐变风（渐变背景 + 圆润填充图标）
        Map<String, String> vividTheme = new LinkedHashMap<>();
        vividTheme.put("key", "theme_vivid");
        vividTheme.put("name", "活力渐变风");
        vividTheme.put("primaryColor", "#FF6B6B");
        vividTheme.put("bgColor", "#FFF3EE");
        vividTheme.put("gradientStart", "#FF9A56");
        vividTheme.put("gradientEnd", "#FF6B6B");
        vividTheme.put("iconStyle", "rounded");
        vividTheme.put("radius", "16px");
        vividTheme.put("isDark", "false");
        vividTheme.put("textPrimary", "#2D2D2D");
        vividTheme.put("textSecondary", "#777777");
        PRESET_THEMES.add(vividTheme);

        // 暗黑科技风（深色底 + 霓虹边；文字反色）
        Map<String, String> darkTheme = new LinkedHashMap<>();
        darkTheme.put("key", "theme_dark");
        darkTheme.put("name", "暗黑科技风");
        darkTheme.put("primaryColor", "#00E5FF");
        darkTheme.put("bgColor", "#0F0F1A");
        darkTheme.put("gradientStart", "#1A1A2E");
        darkTheme.put("gradientEnd", "#16213E");
        darkTheme.put("iconStyle", "neon");
        darkTheme.put("radius", "4px");
        darkTheme.put("isDark", "true");
        darkTheme.put("textPrimary", "#EAEAEA");
        darkTheme.put("textSecondary", "#A0A0B0");
        PRESET_THEMES.add(darkTheme);

        // 温暖手绘风（暖色底 + 手绘插画）
        Map<String, String> handdrawnTheme = new LinkedHashMap<>();
        handdrawnTheme.put("key", "theme_handdrawn");
        handdrawnTheme.put("name", "温暖手绘风");
        handdrawnTheme.put("primaryColor", "#F4A261");
        handdrawnTheme.put("bgColor", "#FFFBF2");
        handdrawnTheme.put("gradientStart", "#FFF8E7");
        handdrawnTheme.put("gradientEnd", "#FFE8CC");
        handdrawnTheme.put("iconStyle", "handdrawn");
        handdrawnTheme.put("radius", "12px");
        handdrawnTheme.put("isDark", "false");
        handdrawnTheme.put("textPrimary", "#3D3D3D");
        handdrawnTheme.put("textSecondary", "#8A7A6A");
        PRESET_THEMES.add(handdrawnTheme);
    }

    /**
     * 当前租户主题配置（app_theme_key + 4 套预设主题色板）
     */
    @SaCheckPermission("micro:platform:theme")
    @GetMapping("/config")
    public R<Map<String, Object>> config() {
        // ★修复：TenantHelper 仅读 ThreadLocal 上下文（仅 @TenantJob 切面设置，Web 请求线程恒为 null），
        //   误用导致 selectById(null) 恒回退默认主题；改用 LoginHelper（回退登录态租户）。
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return R.fail("无法获取当前登录租户，请重新登录");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentThemeKey", tenant != null && tenant.getAppThemeKey() != null
            ? tenant.getAppThemeKey() : DEFAULT_THEME_KEY);
        result.put("themes", PRESET_THEMES);
        return R.ok(result);
    }

    /**
     * 预设主题列表（不含当前选中状态）
     */
    @SaCheckPermission("micro:platform:theme")
    @GetMapping("/themes")
    public R<List<Map<String, String>>> themes() {
        return R.ok(PRESET_THEMES);
    }

    /**
     * ★当前租户小程序主题（v1.43，登录可读、不校验权限）：小程序端首页欢迎区/活动情况/浏览页按租户主题渲染。
     * <p>返回当前租户 app_theme_key 对应完整色板（key/name/primaryColor/bgColor/gradientStart/gradientEnd 等）；
     * 未配置时回退 theme_fresh（清新简约风），保证小程序端任何登录角色都能拿到可渲染色板。
     */
    @GetMapping("/current")
    public R<Map<String, String>> current() {
        // ★修复：同 /config，Web 请求线程 TenantHelper 上下文恒为 null，改用 LoginHelper 取登录态租户
        Long tenantId = LoginHelper.getTenantId();
        SysTenant tenant = tenantId != null ? tenantMapper.selectById(tenantId) : null;
        String key = tenant != null && tenant.getAppThemeKey() != null ? tenant.getAppThemeKey() : DEFAULT_THEME_KEY;
        Map<String, String> theme = PRESET_THEMES.stream()
            .filter(t -> t.get("key").equals(key))
            .findFirst()
            .orElse(PRESET_THEMES.get(0));
        return R.ok(theme);
    }

    /**
     * ★匿名主题接口（T29.5）：小程序登录前按 tenantId 获取主题，用于登录页/门店选择页主题渲染。
     * <p>不校验登录态，通过 URL 参数 tenantId 查询租户主题配置。
     * <p>返回格式与 /current 一致（key/name/primaryColor/bgColor 等完整色板）。
     */
    @SaIgnore
    @GetMapping("/currentByTenant")
    public R<Map<String, String>> currentByTenant(@RequestParam("tenantId") Long tenantId) {
        if (tenantId == null) {
            return R.fail("tenantId 不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        String key = tenant != null && tenant.getAppThemeKey() != null ? tenant.getAppThemeKey() : DEFAULT_THEME_KEY;
        Map<String, String> theme = PRESET_THEMES.stream()
            .filter(t -> t.get("key").equals(key))
            .findFirst()
            .orElse(PRESET_THEMES.get(0));
        return R.ok(theme);
    }

    /**
     * ★保存选定主题（更新 sys_tenant.app_theme_key）
     *
     * @param themeKey 主题key（theme_fresh/theme_vivid/theme_dark/theme_handdrawn）
     */
    @SaCheckPermission("micro:platform:theme")
    @Log(title = "小程序主题", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public R<Void> save(@RequestBody String themeKey) {
        // ★修复：原用 TenantHelper.getTenantId() 在 Web 线程恒为 null → updateById(id=null) 更新 0 行，
        //   保存静默空操作却返回成功（PC 刷新后主题回退默认值的根因）；改用 LoginHelper 并 fail-fast。
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return R.fail("无法获取当前登录租户，请重新登录");
        }
        // ★修复：前端 JSON 序列化 String 时带双引号（"theme_handdrawn"），
        //   @RequestBody String 原样接收导致校验失败、更新未执行。
        //   此处剥离首尾引号 + 空白，保证与 PRESET_THEMES key 精确匹配。
        String key = themeKey.replace("\"", "").trim();
        // 校验 themeKey 合法性
        boolean valid = PRESET_THEMES.stream().anyMatch(t -> t.get("key").equals(key));
        if (!valid) {
            return R.fail("无效的主题key：" + key);
        }
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setAppThemeKey(key);
        tenantMapper.updateById(update);
        return R.ok();
    }
}
