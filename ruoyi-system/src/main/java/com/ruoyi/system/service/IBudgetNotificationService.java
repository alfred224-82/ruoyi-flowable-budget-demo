package com.ruoyi.system.service;

import java.util.Date;

/**
 * 预算邮件通知服务接口
 *
 * @author ruoyi
 */
public interface IBudgetNotificationService {

    /**
     * N1: 发送编制期开始提醒（每月26日触发）
     *
     * @param budgetPeriod 预算期间（如 "2026年7月"）
     * @param startDate    编制开始日期
     * @param endDate      编制截止日期
     */
    void sendPeriodStartNotify(String budgetPeriod, Date startDate, Date endDate);

    /**
     * N2: 发送待审批通知（提交审核时触发）
     *
     * @param sheetNo        预算单号
     * @param deptName       编制部门
     * @param createByName   编制人姓名
     * @param approvalLevel  审批级别（Dept/Branch/HQ）
     */
    void sendApprovalNotify(String sheetNo, String deptName, String createByName, String approvalLevel);

    /**
     * N3: 发送驳回通知（驳回时触发）
     *
     * @param sheetNo      预算单号
     * @param rejectLevel  驳回级别
     * @param rejectReason 驳回理由
     * @param deadline     截止时间
     * @param sheetId      预算单ID
     */
    void sendRejectNotify(String sheetNo, String rejectLevel, String rejectReason, Date deadline, Long sheetId);

    /**
     * N4: 发送审批通过通知（三级审批全部通过时触发）
     *
     * @param sheetNo      预算单号
     * @param budgetPeriod 预算期间
     */
    void sendApprovedNotify(String sheetNo, String budgetPeriod);

    /**
     * N5/N6: 发送即将超时提醒（定时任务触发）
     *
     * @param sheetNo        预算单号
     * @param remainingHours 剩余小时数
     * @param deadline       截止时间
     * @param sheetId        预算单ID
     */
    void sendTimeoutRemind(String sheetNo, int remainingHours, Date deadline, Long sheetId);

    /**
     * N7: 发送超时告警（定时任务触发）
     *
     * @param sheetNo              预算单号
     * @param timeoutDurationHours 超时时长（小时）
     * @param originalDeadline     原截止时间
     * @param sheetId              预算单ID
     */
    void sendTimeoutAlert(String sheetNo, int timeoutDurationHours, Date originalDeadline, Long sheetId);
}
