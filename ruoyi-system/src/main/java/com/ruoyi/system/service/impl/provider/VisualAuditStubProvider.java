package com.ruoyi.system.service.impl.provider;

import com.ruoyi.system.domain.compliance.VisualAuditResult;
import com.ruoyi.system.service.IVisualAuditProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 视觉审核桩实现（M18 T18.3，默认 Provider）
 * <p>未接入真实厂商视觉审核 API 时占位：一律放行并记 WARN 日志。
 * <p>接入腾讯云/阿里云时新增 @Primary Bean 即可覆盖（见 ComplianceProviderConfig）。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
public class VisualAuditStubProvider implements IVisualAuditProvider {

    @Override
    public VisualAuditResult audit(byte[] imageBytes, List<String> enabledTypes, double threshold) {
        if (imageBytes == null || imageBytes.length == 0) {
            return VisualAuditResult.pass("空图片，桩实现放行");
        }
        log.warn("{\"event\":\"visual_audit_stub\",\"enabledTypes\":{},\"threshold\":{},\"msg\":\"未接入视觉审核 API，桩实现放行\"}",
            enabledTypes, threshold);
        return VisualAuditResult.pass("未接入视觉审核 API（桩实现放行）");
    }
}
