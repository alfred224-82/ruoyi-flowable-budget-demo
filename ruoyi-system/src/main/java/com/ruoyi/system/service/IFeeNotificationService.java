package com.ruoyi.system.service;

/**
 * 资金管控通知体系Service接口（T5.12）
 * 审批人合并通知（N→1）、老板每日≤1条简报、财务站内信；对接消息中心
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeNotificationService {

    /**
     * 按角色广播站内信（发给该角色全部正常用户）
     *
     * @param roleKey 角色标识（boss/finance 等）
     */
    void notifyRole(String roleKey, String title, String content, String messageType, String bizType, Long bizId);

    /**
     * 老板每日合并简报（N条待办 → 1条，限频每日≤1条）
     * 由定时任务调用（每日固定时间）
     *
     * @return 发送条数（0或1）
     */
    int sendBossDailyBriefing();
}
