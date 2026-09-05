package com.ruoyi.system.service;

import java.math.BigDecimal;

/**
 * 补助管理Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface ISubsidyService {

    /**
     * 自动核算补助金额
     *
     * @param subsidyType  补助类型（TRAVEL/MEAL/TRANSPORT）
     * @param days         天数
     * @param industryCode 行业编码
     * @return 补助核算结果
     */
    SubsidyCalcResult calculate(String subsidyType, int days, String industryCode);

    /**
     * 补助核算结果
     */
    class SubsidyCalcResult {
        private BigDecimal amount;
        private String subjectCode;
        private String subjectName;
        private String subsidyName;
        private boolean exceededMaxDays;
        private Integer maxDays;

        public SubsidyCalcResult(BigDecimal amount, String subjectCode, String subjectName,
                                  String subsidyName, boolean exceededMaxDays, Integer maxDays) {
            this.amount = amount;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.subsidyName = subsidyName;
            this.exceededMaxDays = exceededMaxDays;
            this.maxDays = maxDays;
        }

        public BigDecimal getAmount() { return amount; }
        public String getSubjectCode() { return subjectCode; }
        public String getSubjectName() { return subjectName; }
        public String getSubsidyName() { return subsidyName; }
        public boolean isExceededMaxDays() { return exceededMaxDays; }
        public Integer getMaxDays() { return maxDays; }
    }
}
