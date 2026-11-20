package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * 拉新奖励发放记录查询业务对象 promotion_reward_record（T16.7，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionRewardRecordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推广人用户ID */
    private Long promoterUserId;

    /** 推广人角色 */
    private String promoterRoleCode;

    /** 被拉新顾客用户ID */
    private Long customerUserId;

    /** 发放状态：PENDING/PAID_OFFLINE/ISSUED */
    private String issueStatus;

    /** 奖励形式：cash/coupon */
    private String rewardType;
}
