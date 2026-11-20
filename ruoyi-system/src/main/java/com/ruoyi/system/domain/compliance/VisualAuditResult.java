package com.ruoyi.system.domain.compliance;

import lombok.Data;

import java.io.Serializable;

/**
 * 视觉审核结果（M18 T18.3，IVisualAuditProvider 返回体，非表映射 POJO）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class VisualAuditResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过 */
    private boolean pass;

    /** 违规类型：porn/violence/politics（PASS 时为 null） */
    private String violationType;

    /** 置信度（0.00~1.00） */
    private double confidence;

    /** 说明信息 */
    private String message;

    public static VisualAuditResult pass(String message) {
        VisualAuditResult r = new VisualAuditResult();
        r.setPass(true);
        r.setMessage(message);
        return r;
    }

    public static VisualAuditResult reject(String violationType, double confidence, String message) {
        VisualAuditResult r = new VisualAuditResult();
        r.setPass(false);
        r.setViolationType(violationType);
        r.setConfidence(confidence);
        r.setMessage(message);
        return r;
    }
}
