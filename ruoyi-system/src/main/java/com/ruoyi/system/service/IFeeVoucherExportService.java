package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeeExportTemplateVo;

import java.util.List;

/**
 * 凭证导出Service接口（T5.11 / ADR-017/020）
 * 选月 + 选模板（内置7套）+ 选范围 → 借贷两行 → Excel
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeVoucherExportService {

    /**
     * 模板列表（内置7套 + 租户自定义）
     */
    List<FeeExportTemplateVo> listTemplates();

    /**
     * 生成凭证导出文件（Excel字节流）
     *
     * @param month      导出月份 yyyy-MM（必选）
     * @param templateId 模板ID（必选）
     * @param projectId  项目范围（可空=全部，0=公司级）
     */
    byte[] exportVouchers(String month, Long templateId, Long projectId);

    /**
     * 二维导出（T7.3 / FEAT_EXPORT_2D）：凭证导出附加第二张 Sheet（项目×科目矩阵）
     *
     * @param month      导出月份 yyyy-MM（必选）
     * @param templateId 模板ID（必选）
     * @param projectId  项目范围（可空=全部，0=公司级）
     */
    byte[] exportVouchers2d(String month, Long templateId, Long projectId);
}
