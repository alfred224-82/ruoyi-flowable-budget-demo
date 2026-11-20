package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.SensitiveWordBo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;
import com.ruoyi.system.service.ISensitiveWordAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 全局敏感词库管理 Controller（M18 T18.3，platform_admin 专属）
 * <p>类级 {@code @SaCheckRole("platform_admin")} + {@code @RequireFeature("FEAT_SENSITIVE_WORD")}；
 *   非 platform_admin 访问返回 403；开关关闭时 FeatureInterceptor 拦截。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/admin/sensitiveWord")
@SaCheckRole("platform_admin")
@RequireFeature("FEAT_SENSITIVE_WORD")
public class AdminSensitiveWordController extends BaseController {

    private final ISensitiveWordAdminService adminService;

    /** 全局敏感词列表（分页，tenant_id=0） */
    @SaCheckPermission("compliance:sensitiveWord:list")
    @GetMapping("/list")
    public TableDataInfo<SensitiveWordVo> list(SensitiveWordBo bo, PageQuery pageQuery) {
        return adminService.queryWordPage(bo, pageQuery);
    }

    /** 全局敏感词详情 */
    @SaCheckPermission("compliance:sensitiveWord:list")
    @GetMapping("/{id}")
    public R<SensitiveWordVo> detail(@PathVariable Long id) {
        return R.ok(adminService.queryWordById(id));
    }

    /** 新增全局敏感词 */
    @SaCheckPermission("compliance:sensitiveWord:add")
    @Log(title = "全局敏感词", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SensitiveWordBo bo) {
        return R.ok(adminService.insertWord(bo));
    }

    /** 修改全局敏感词 */
    @SaCheckPermission("compliance:sensitiveWord:edit")
    @Log(title = "全局敏感词", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SensitiveWordBo bo) {
        return toAjax(adminService.updateWord(bo));
    }

    /** 删除全局敏感词 */
    @SaCheckPermission("compliance:sensitiveWord:remove")
    @Log(title = "全局敏感词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(adminService.deleteWord(id));
    }

    /** 批量导入全局敏感词 */
    @SaCheckPermission("compliance:sensitiveWord:import")
    @Log(title = "全局敏感词导入", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public R<Integer> importWords(@RequestParam List<String> words,
                                  @RequestParam(required = false, defaultValue = "CUSTOM") String category,
                                  @RequestParam(required = false, defaultValue = "exact") String matchMode) {
        return R.ok(adminService.importWords(words, category, matchMode));
    }

    /** 敏感词总开关（0启用/1禁用全局底库） */
    @SaCheckPermission("compliance:sensitiveWord:toggle")
    @Log(title = "敏感词总开关", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle")
    public R<Void> toggle(@RequestParam Integer status) {
        return toAjax(adminService.toggleSensitiveWord(status));
    }
}
