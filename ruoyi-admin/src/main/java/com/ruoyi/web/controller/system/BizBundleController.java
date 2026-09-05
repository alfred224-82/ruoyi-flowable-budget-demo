package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.BizBundle;
import com.ruoyi.system.domain.vo.BizBundleVo;
import com.ruoyi.system.service.IBizBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业态包管理Controller（T10.2 多业态平台底座）
 * <p>biz_bundle 为系统级共享表（无 tenant_id），仅平台超管维护；新增业态包只需插数据 + 补文案，
 * 不动核心表结构、不改核心代码。admin 超管豁免权限校验（RuoYi 超管返回 *:*:*）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/bizBundle")
public class BizBundleController extends BaseController {

    private final IBizBundleService bizBundleService;

    /**
     * 业态包列表
     */
    @SaCheckPermission("system:bizBundle:list")
    @GetMapping("/list")
    public R<List<BizBundleVo>> list() {
        return R.ok(bizBundleService.listBundles());
    }

    /**
     * 业态包详情（含解析后的开关集与文案映射，便于管理页预览）
     */
    @SaCheckPermission("system:bizBundle:query")
    @GetMapping("/{bizCode}")
    public R<Map<String, Object>> getInfo(@PathVariable String bizCode) {
        BizBundle bundle = bizBundleService.getBundle(bizCode);
        Set<String> flags = bizBundleService.getFeatureFlags(bizCode);
        Map<String, String> copy = bizBundleService.getCopy(bizCode);
        return R.ok(Map.of("bundle", bundle == null ? Map.of() : bundle, "featureFlags", flags, "copy", copy));
    }

    /**
     * 新增/修改业态包（保存后失效缓存）
     */
    @SaCheckPermission("system:bizBundle:edit")
    @Log(title = "业态包管理", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody BizBundleVo vo) {
        return toAjax(bizBundleService.saveBundle(vo));
    }

    /**
     * 手动失效业态包缓存（bizCode 为空时清空全部）
     */
    @SaCheckPermission("system:bizBundle:edit")
    @Log(title = "业态包管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/cache")
    public R<Void> evictCache(@RequestParam(required = false) String bizCode) {
        bizBundleService.evictCache(bizCode);
        return R.ok();
    }
}
