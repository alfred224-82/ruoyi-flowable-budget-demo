package com.ruoyi.web.controller.portal;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公众号菜单配置生成器（T13.1，企业版零散缺口）
 * <p>将系统菜单树转换为微信公众号自定义菜单 JSON 格式，
 * 供运营人员复制到微信公众平台后台配置。
 * <p>约束：微信菜单最多 3 个一级菜单，每个一级菜单下最多 5 个子菜单；
 *   菜单名最长 8 个汉字 / 16 个英文字符。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/wechat/menu")
public class WechatMenuConfigController extends BaseController {

    private final ISysMenuService menuService;

    /** 微信一级菜单上限 */
    private static final int MAX_TOP_LEVEL = 3;
    /** 微信子菜单上限 */
    private static final int MAX_SUB_LEVEL = 5;

    /**
     * 生成微信公众号菜单配置 JSON
     * <p>从系统菜单树中选取 client_type=mp 的菜单，按 order_num 排序，
     * 截取前 3 个一级菜单，每个下面截取前 5 个子菜单。
     *
     * @param topMenuIds 指定一级菜单ID列表（为空则取全部 mp 菜单）
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/generate")
    public R<Map<String, Object>> generate(@RequestParam(required = false) List<Long> topMenuIds) {
        Long loginUserId = getLoginUserId();
        // ★fail-fast：未登录不允许生成菜单
        if (loginUserId == null) {
            return R.fail("用户未登录");
        }
        List<SysMenu> allMenus = menuService.selectMenuTreeByUserId(loginUserId, "mp");
        // 过滤 mp 端菜单（selectMenuTreeByUserId 已按 clientType 过滤，此处兜底）
        List<SysMenu> mpMenus = allMenus.stream()
            .filter(m -> "mp".equals(m.getClientType()) || "both".equals(m.getClientType()) || m.getClientType() == null)
            .sorted(Comparator.comparingInt(m -> m.getOrderNum() != null ? m.getOrderNum() : 0))
            .collect(Collectors.toList());
        // 构建树
        List<SysMenu> topMenus;
        if (topMenuIds != null && !topMenuIds.isEmpty()) {
            topMenus = mpMenus.stream()
                .filter(m -> m.getMenuId() != null && topMenuIds.contains(m.getMenuId()) && m.getParentId() == 0L)
                .limit(MAX_TOP_LEVEL)
                .collect(Collectors.toList());
        } else {
            topMenus = mpMenus.stream()
                .filter(m -> m.getParentId() == 0L)
                .limit(MAX_TOP_LEVEL)
                .collect(Collectors.toList());
        }
        // 转换为微信菜单 JSON
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (SysMenu top : topMenus) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("name", truncateForWechat(top.getMenuName()));
            List<SysMenu> children = mpMenus.stream()
                .filter(m -> top.getMenuId().equals(m.getParentId()))
                .sorted(Comparator.comparingInt(m -> m.getOrderNum() != null ? m.getOrderNum() : 0))
                .limit(MAX_SUB_LEVEL)
                .collect(Collectors.toList());
            if (children.isEmpty()) {
                // 无子菜单 → click 类型
                button.put("type", "view");
                button.put("url", resolveMenuUrl(top));
            } else {
                List<Map<String, Object>> subButtons = new ArrayList<>();
                for (SysMenu child : children) {
                    Map<String, Object> sub = new LinkedHashMap<>();
                    sub.put("type", "view");
                    sub.put("name", truncateForWechat(child.getMenuName()));
                    sub.put("url", resolveMenuUrl(child));
                    subButtons.add(sub);
                }
                button.put("sub_button", subButtons);
            }
            buttons.add(button);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("button", buttons);
        result.put("menu", menu);
        result.put("tip", "复制此 JSON 到微信公众平台 → 自定义菜单 → 粘贴配置");
        return R.ok(result);
    }

    /**
     * 预览纯文本格式（便于运营人员核对）
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/preview")
    public R<String> preview() {
        R<Map<String, Object>> jsonResult = generate(null);
        Map<String, Object> data = jsonResult.getData();
        if (data == null) return R.ok("无菜单数据");
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        Map<String, Object> menuObj = (Map<String, Object>) data.get("menu");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> buttons = (List<Map<String, Object>>) menuObj.get("button");
        for (int i = 0; i < buttons.size(); i++) {
            Map<String, Object> btn = buttons.get(i);
            sb.append("├─ ").append(btn.get("name")).append("\n");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subs = (List<Map<String, Object>>) btn.get("sub_button");
            if (subs != null) {
                for (Map<String, Object> sub : subs) {
                    sb.append("│  ├─ ").append(sub.get("name")).append(" → ").append(sub.get("url")).append("\n");
                }
            } else {
                sb.append("│  → ").append(btn.get("url")).append("\n");
            }
        }
        return R.ok(sb.toString());
    }

    /** 微信菜单名最长 8 汉字 */
    private String truncateForWechat(String name) {
        if (name == null) return "";
        return name.length() > 8 ? name.substring(0, 8) : name;
    }

    /** 解析菜单 URL：如果是外链直接用，否则拼 H5 域名 */
    private String resolveMenuUrl(SysMenu menu) {
        if (menu == null) {
            return "https://your-domain.com/h5#/";
        }
        String path = menu.getPath();
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            return path;
        }
        // 内部路由 → 拼 H5 域名（占位，实际部署时替换）
        return "https://your-domain.com/h5#" + (path != null ? path : "/");
    }

    private Long getLoginUserId() {
        try {
            return com.ruoyi.common.helper.LoginHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
