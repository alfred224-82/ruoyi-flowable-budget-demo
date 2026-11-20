package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import com.ruoyi.system.domain.vo.SensitiveWordHitLogVo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;
import com.ruoyi.system.service.ISensitiveWordTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 租户敏感词管理 Controller（M18 T18.6，租户管理员）
 * <p>类级 {@code @RequireFeature("FEAT_SENSITIVE_WORD")}；数据隔离由 Service 层按当前租户过滤。
 * <p>自定义词 CRUD（micro:sensitiveWord:*）+ 全局底库只读（micro:sensitiveWord:query）
 *   + 命中记录只读（micro:sensitiveWord:hitLog）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/tenant/sensitiveWord")
@RequireFeature("FEAT_SENSITIVE_WORD")
public class TenantSensitiveWordController extends BaseController {

    private final ISensitiveWordTenantService tenantService;

    /** 租户自定义词列表（分页） */
    @SaCheckPermission("micro:sensitiveWord:list")
    @GetMapping("/list")
    public TableDataInfo<SensitiveWordVo> list(SensitiveWordBo bo, PageQuery pageQuery) {
        return tenantService.queryTenantWordPage(bo, pageQuery);
    }

    /** 全局底库只读列表（分页） */
    @SaCheckPermission("micro:sensitiveWord:query")
    @GetMapping("/global")
    public TableDataInfo<SensitiveWordVo> globalList(SensitiveWordBo bo, PageQuery pageQuery) {
        return tenantService.queryGlobalWordPage(bo, pageQuery);
    }

    /** 租户自定义词详情 */
    @SaCheckPermission("micro:sensitiveWord:list")
    @GetMapping("/{id}")
    public R<SensitiveWordVo> detail(@PathVariable Long id) {
        return R.ok(tenantService.queryWordById(id));
    }

    /** 新增租户自定义词 */
    @SaCheckPermission("micro:sensitiveWord:add")
    @Log(title = "租户敏感词", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SensitiveWordBo bo) {
        return R.ok(tenantService.insertWord(bo));
    }

    /** 修改租户自定义词 */
    @SaCheckPermission("micro:sensitiveWord:edit")
    @Log(title = "租户敏感词", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SensitiveWordBo bo) {
        return toAjax(tenantService.updateWord(bo));
    }

    /** 删除租户自定义词 */
    @SaCheckPermission("micro:sensitiveWord:remove")
    @Log(title = "租户敏感词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(tenantService.deleteWord(id));
    }

    /** 命中记录只读列表（分页，当前租户） */
    @SaCheckPermission("micro:sensitiveWord:hitLog")
    @GetMapping("/hitLog")
    public TableDataInfo<SensitiveWordHitLogVo> hitLog(SensitiveWordHitLogVo vo, PageQuery pageQuery) {
        return tenantService.queryHitLogPage(vo, pageQuery);
    }
}
