package com.ruoyi.system.service;

import com.ruoyi.system.domain.FeeExportTemplate;
import com.ruoyi.system.domain.vo.FeeExportTemplateVo;

import java.util.List;

/**
 * 自定义凭证模板Service接口（T6.6 / FEAT_EXPORT_TEMPLATE_CUSTOM）
 * 内置7套只读，租户可新增/修改/删除自定义模板（列头/分隔符/借贷方向）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeExportTemplateService {

    /**
     * 模板列表（内置+本租户自定义）
     *
     * @param templateName 模板名称（模糊，可空）
     */
    List<FeeExportTemplateVo> listTemplates(String templateName);

    /**
     * 模板详情
     */
    FeeExportTemplateVo getTemplate(Long id);

    /**
     * 新增自定义模板（强制 isBuiltin=0、tenantId=当前租户）
     */
    Boolean insertTemplate(FeeExportTemplate template);

    /**
     * 修改模板（仅本租户自定义模板，内置模板禁止修改）
     */
    Boolean updateTemplate(FeeExportTemplate template);

    /**
     * 删除模板（仅本租户自定义模板，内置模板禁止删除）
     */
    Boolean deleteTemplate(Long id);
}
