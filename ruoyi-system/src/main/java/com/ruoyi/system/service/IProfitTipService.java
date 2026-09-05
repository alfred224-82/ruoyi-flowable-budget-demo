package com.ruoyi.system.service;

import java.math.BigDecimal;

/**
 * 盈利提示Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IProfitTipService {

    /**
     * 根据科目类型获取盈利提示
     *
     * @param budgetSubjectCode 预算科目编码
     * @param industryCode      行业编码
     * @param amount            金额
     * @return 盈利提示结果
     */
    ProfitTipResult getTip(String budgetSubjectCode, String industryCode, BigDecimal amount);

    /**
     * 盈利提示结果
     */
    class ProfitTipResult {
        private String tipText;
        private String tipColor;
        private String tipType;
        private boolean forcePopup;

        public ProfitTipResult(String tipText, String tipColor, String tipType, boolean forcePopup) {
            this.tipText = tipText;
            this.tipColor = tipColor;
            this.tipType = tipType;
            this.forcePopup = forcePopup;
        }

        public String getTipText() { return tipText; }
        public String getTipColor() { return tipColor; }
        public String getTipType() { return tipType; }
        public boolean isForcePopup() { return forcePopup; }
    }
}
