package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.BizCopyVo;
import com.ruoyi.system.service.IBizCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业态文案映射 Controller（T10.5 v1.29 文案映射 i18n）
 * <p>供前端管理页维护多语言文案；供前端 $bizT(key) helper 拉取当前业态包 + 当前语言的文案映射。
 * <p>合并优先级由调用方按序尝试：租户 module_overrides.copy &gt; biz_copy &gt; biz_bundle.copy &gt; 前端硬编码兜底。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/bizCopy")
public class BizCopyController extends BaseController {

    private final IBizCopyService bizCopyService;

    /**
     * 查询指定业态包 + 语言的文案映射（前端 $bizT 一次性加载）
     * <p>★无需 system:bizCopy:query 权限：此端点为 i18n 文案读取接口，所有登录用户均需调用（登录时 GetInfo → bizCopy/switchBizCode 触发）；
     * 管理页 /list 仍保留 system:bizCopy:list 权限校验。
     */
    @GetMapping("/map")
    public R<Map<String, String>> getCopyMap(@RequestParam(required = false) String bizCode,
                                             @RequestParam(required = false) String lang) {
        return R.ok(bizCopyService.getCopy(bizCode, lang));
    }

    /**
     * 查询指定业态包的全部语言文案（前端多语言切换时一次性加载）
     * <p>★无需 system:bizCopy:query 权限：同上，i18n 文案读取接口，面向所有登录用户。
     */
    @GetMapping("/map-all")
    public R<Map<String, Map<String, String>>> getCopyMapAllLangs(@RequestParam(required = false) String bizCode) {
        return R.ok(bizCopyService.getCopyAllLangs(bizCode));
    }

    /**
     * 查询支持的语言列表（去重）
     * <p>★无需 system:bizCopy:query 权限：同上，i18n 辅助接口。
     */
    @GetMapping("/langs")
    public R<List<String>> listLangs() {
        return R.ok(bizCopyService.listLangs());
    }

    /**
     * 查询文案列表（管理页表格）
     */
    @SaCheckPermission("system:bizCopy:list")
    @GetMapping("/list")
    public R<List<BizCopyVo>> list(BizCopyVo query) {
        return R.ok(bizCopyService.listCopies(query));
    }

    /**
     * 新增或更新文案（按 (bizCode, i18nKey, lang) 唯一键幂等）
     */
    @SaCheckPermission("system:bizCopy:edit")
    @Log(title = "业态文案映射", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody BizCopyVo vo) {
        return toAjax(bizCopyService.saveCopy(vo));
    }

    /**
     * 批量新增/更新文案（前端表格批量保存）
     */
    @SaCheckPermission("system:bizCopy:edit")
    @Log(title = "业态文案映射", businessType = BusinessType.UPDATE)
    @PostMapping("/batch")
    public R<Void> batchSave(@RequestBody List<BizCopyVo> list) {
        return toAjax(bizCopyService.batchSave(list));
    }

    /**
     * 按 ID 删除文案
     */
    @SaCheckPermission("system:bizCopy:edit")
    @Log(title = "业态文案映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(bizCopyService.deleteById(id));
    }

    /**
     * 按 (bizCode, lang) 批量删除
     */
    @SaCheckPermission("system:bizCopy:edit")
    @Log(title = "业态文案映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public R<Void> removeBatch(@RequestParam String bizCode, @RequestParam String lang) {
        return toAjax(bizCopyService.deleteByBizAndLang(bizCode, lang));
    }

    /**
     * 手动失效缓存（bizCode/lang 为空时清全部）
     */
    @SaCheckPermission("system:bizCopy:edit")
    @Log(title = "业态文案映射", businessType = BusinessType.CLEAN)
    @DeleteMapping("/cache")
    public R<Void> evictCache(@RequestParam(required = false) String bizCode,
                              @RequestParam(required = false) String lang) {
        bizCopyService.evictCache(bizCode, lang);
        return R.ok();
    }
}
