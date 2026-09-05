package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeExportTemplate;
import com.ruoyi.system.domain.vo.FeeExportTemplateVo;
import com.ruoyi.system.service.IFeeExportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义凭证模板Controller（T6.6 / FEAT_EXPORT_TEMPLATE_CUSTOM）
 * 内置7套只读，租户可新增/修改/删除自定义模板（列头/分隔符/借贷方向）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/exportTemplate")
public class FeeExportTemplateController extends BaseController {

    private final IFeeExportTemplateService exportTemplateService;

    /**
     * 模板列表（内置+本租户自定义）
     */
    @RequireFeature("FEAT_EXPORT_TEMPLATE_CUSTOM")
    @SaCheckPermission("fee:exportTemplate:list")
    @GetMapping("/list")
    public R<List<FeeExportTemplateVo>> list(@RequestParam(required = false) String templateName) {
        return R.ok(exportTemplateService.listTemplates(templateName));
    }

    /**
     * 模板详情
     */
    @RequireFeature("FEAT_EXPORT_TEMPLATE_CUSTOM")
    @SaCheckPermission("fee:exportTemplate:query")
    @GetMapping("/{id}")
    public R<FeeExportTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(exportTemplateService.getTemplate(id));
    }

    /**
     * 新增自定义模板
     */
    @RequireFeature("FEAT_EXPORT_TEMPLATE_CUSTOM")
    @SaCheckPermission("fee:exportTemplate:edit")
    @Log(title = "自定义凭证模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FeeExportTemplate template) {
        return toAjax(exportTemplateService.insertTemplate(template));
    }

    /**
     * 修改自定义模板（内置模板禁止修改）
     */
    @RequireFeature("FEAT_EXPORT_TEMPLATE_CUSTOM")
    @SaCheckPermission("fee:exportTemplate:edit")
    @Log(title = "自定义凭证模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FeeExportTemplate template) {
        return toAjax(exportTemplateService.updateTemplate(template));
    }

    /**
     * 删除自定义模板（内置模板禁止删除）
     */
    @RequireFeature("FEAT_EXPORT_TEMPLATE_CUSTOM")
    @SaCheckPermission("fee:exportTemplate:edit")
    @Log(title = "自定义凭证模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(exportTemplateService.deleteTemplate(id));
    }
}
