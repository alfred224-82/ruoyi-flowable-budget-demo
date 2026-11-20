package com.ruoyi.system.domain.compliance;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词检测结果（M18 T18.2，非表映射 POJO）
 * <p>三路（text/voice_ocr/image_ocr）共用统一返回体。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class SensitiveWordCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结果：PASS/FAIL */
    private String result;

    /** 命中项列表（PASS 时为空） */
    private List<HitItem> hitWords = new ArrayList<>();

    /** 检测耗时（毫秒） */
    private long cost;

    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";

    public static SensitiveWordCheckResult pass(long cost) {
        SensitiveWordCheckResult r = new SensitiveWordCheckResult();
        r.setResult(PASS);
        r.setCost(cost);
        return r;
    }

    public static SensitiveWordCheckResult fail(List<HitItem> hitWords, long cost) {
        SensitiveWordCheckResult r = new SensitiveWordCheckResult();
        r.setResult(FAIL);
        r.setHitWords(hitWords != null ? hitWords : new ArrayList<>());
        r.setCost(cost);
        return r;
    }

    public boolean isPass() {
        return PASS.equals(result);
    }

    /** 命中详情项 */
    @Data
    public static class HitItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 命中词 */
        private String word;

        /** 命中分类 */
        private String category;

        /** 命中位置（原文字符偏移，-1=未知） */
        private int position;

        public HitItem() {
        }

        public HitItem(String word, String category, int position) {
            this.word = word;
            this.category = category;
            this.position = position;
        }
    }
}
