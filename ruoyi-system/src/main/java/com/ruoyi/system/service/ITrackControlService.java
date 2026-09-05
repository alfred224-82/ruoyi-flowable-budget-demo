package com.ruoyi.system.service;

import java.math.BigDecimal;

/**
 * 三轨预算管控Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface ITrackControlService {

    /**
     * 三轨管控校验（报销提交时调用）
     *
     * @param budgetSubjectCode 预算科目编码
     * @param amount            报销金额
     * @param industryCode      行业编码
     * @return 校验结果（pass/blocked/warning）
     */
    TrackCheckResult checkTrack(String budgetSubjectCode, BigDecimal amount, String industryCode);

    /**
     * 查询科目所属轨道
     *
     * @param budgetSubjectCode 预算科目编码
     * @param industryCode      行业编码
     * @return 轨道类型（EXPENSE/CAPITAL/CASHFLOW）
     */
    String getTrackType(String budgetSubjectCode, String industryCode);

    /**
     * 三轨管控校验结果
     */
    class TrackCheckResult {
        private String result;  // pass / blocked / warning
        private String message;
        private String trackType;
        private String controlMode;

        public TrackCheckResult(String result, String message, String trackType, String controlMode) {
            this.result = result;
            this.message = message;
            this.trackType = trackType;
            this.controlMode = controlMode;
        }

        public static TrackCheckResult pass(String trackType) {
            return new TrackCheckResult("pass", "校验通过", trackType, null);
        }

        public static TrackCheckResult warning(String message, String trackType) {
            return new TrackCheckResult("warning", message, trackType, "SOFT");
        }

        public static TrackCheckResult blocked(String message, String trackType) {
            return new TrackCheckResult("blocked", message, trackType, "HARD");
        }

        public boolean isPassed() {
            return "pass".equals(result) || "warning".equals(result);
        }

        public boolean isBlocked() {
            return "blocked".equals(result);
        }

        public String getResult() { return result; }
        public String getMessage() { return message; }
        public String getTrackType() { return trackType; }
        public String getControlMode() { return controlMode; }
    }
}
