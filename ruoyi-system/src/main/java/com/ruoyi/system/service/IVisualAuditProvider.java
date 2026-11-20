package com.ruoyi.system.service;

import com.ruoyi.system.domain.compliance.VisualAuditResult;

import java.util.List;

/**
 * 视觉审核 Provider（M18 T18.3，可插拔，默认桩实现 VisualAuditStubProvider）
 * <p>未来接入腾讯云/阿里云视觉审核只需新增 @Primary Bean 覆盖桩实现。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IVisualAuditProvider {

    /**
     * 视觉审核。
     *
     * @param imageBytes   图片字节（非空）
     * @param enabledTypes 启用的违规类型：porn/violence/politics（非空）
     * @param threshold    置信度阈值（0.00~1.00）
     * @return 审核结果（PASS 或违规类型+置信度）
     */
    VisualAuditResult audit(byte[] imageBytes, List<String> enabledTypes, double threshold);
}
