package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.PromotionRewardConfigBo;
import com.ruoyi.system.domain.bo.PromotionRewardRecordBo;
import com.ruoyi.system.domain.vo.PromotionRewardConfigVo;
import com.ruoyi.system.domain.vo.PromotionRewardRecordVo;

/**
 * 推广奖励服务接口（T16.2/T16.7/T16.8/T16.9，小微版 M16）
 * <p>奖励配置管理 / 拉新奖励结算 / 首单识别 / 消费回传通知。
 * <p>★V1 零支付：员工现金奖励走「标记已线下发放」记账口径，不调任何支付接口。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
public interface IPromotionRewardService {

    // ===== T16.2 奖励配置 =====

    /**
     * 奖励配置列表
     */
    TableDataInfo<PromotionRewardConfigVo> queryConfigPage(PromotionRewardConfigBo bo, PageQuery pageQuery);

    /**
     * 更新奖励配置（★trigger_rule 恒 NEW_CUSTOMER 不可改；仅 reward_value/coupon_id/status 可变）
     */
    int updateConfig(PromotionRewardConfigBo bo);

    // ===== T16.7 拉新奖励结算 =====

    /**
     * ★拉新奖励结算（被推广顾客首次消费触发，仅一次）
     * <p>查 promotion_attribution 取当时推广人 → 按 promoter_role_code 取 promotion_reward_config：
     * ├ salesman/boss(员工) → reward_type=cash → 写 promotion_reward_record(issue_status=PENDING)
     * └ customer(顾客) → reward_type=coupon → 写 coupon_record(source=promotion) + promotion_reward_record(ISSUED)
     * → 回传通知推广人小程序(T16.9)
     *
     * @param customerUserId 被拉新顾客用户ID
     * @param firstOrderId 触发拉新的首次消费订单ID
     */
    void settleNewCustomerReward(Long customerUserId, Long firstOrderId);

    /**
     * 奖励发放记录列表（老板 PC）
     */
    TableDataInfo<PromotionRewardRecordVo> queryRewardRecordPage(PromotionRewardRecordBo bo, PageQuery pageQuery);

    /**
     * ★老板标记现金奖励已线下发放（issue_status=PENDING→PAID_OFFLINE）
     */
    int markPaidOffline(Long id);

    // ===== T16.8 首单识别 =====

    /**
     * ★首单判定（手机号+活动独立判断是否首次）
     * <p>无记录→首客→享受首次活动优惠+写 first_order_record；有记录→老客→仅常规优惠。
     * <p>★与推广归属解耦：判定只看手机号+活动，不看扫了谁的码。
     *
     * @param activityId 活动ID
     * @param customerPhone 顾客手机号
     * @param customerUserId 顾客用户ID
     * @param orderId 消费订单ID
     * @return true=首单(可享优惠) false=老客
     */
    boolean checkFirstOrder(Long activityId, String customerPhone, Long customerUserId, Long orderId);

    /**
     * 首单活动配置列表（老板 PC，复用 promotion_activity 但按首单维度）
     */
    TableDataInfo<com.ruoyi.system.domain.vo.FirstOrderRecordVo> queryFirstOrderPage(
        com.ruoyi.system.domain.bo.FirstOrderRecordBo bo, PageQuery pageQuery);
}
