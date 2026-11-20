package com.ruoyi.system.domain.compliance;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片合规检测结果（M18 T18.3，非表映射 POJO）
 * <p>OCR 文字路 + 视觉审核路两路并行，任一 FAIL 即整体 FAIL。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class ImageComplianceResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结果：PASS/FAIL */
    private String result;

    /** 失败原因（PASS 时为 null） */
    private String failReason;

    /** OCR 命中敏感词列表 */
    private List<SensitiveWordCheckResult.HitItem> ocrHits = new ArrayList<>();

    /** 视觉命中违规类型列表 */
    private List<VisualHit> visualHits = new ArrayList<>();

    /** 检测耗时（毫秒） */
    private long cost;

    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";

    public static ImageComplianceResult pass(long cost) {
        ImageComplianceResult r = new ImageComplianceResult();
        r.setResult(PASS);
        r.setCost(cost);
        return r;
    }

    public boolean isPass() {
        return PASS.equals(result);
    }

    /** 视觉违规命中项 */
    @Data
    public static class VisualHit implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 违规类型：porn/violence/politics */
        private String violationType;

        /** 置信度（0.00~1.00） */
        private double confidence;

        public VisualHit() {
        }

        public VisualHit(String violationType, double confidence) {
            this.violationType = violationType;
            this.confidence = confidence;
        }
    }
}
