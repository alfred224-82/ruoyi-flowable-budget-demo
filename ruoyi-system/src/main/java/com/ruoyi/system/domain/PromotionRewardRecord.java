package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 拉新奖励发放记录对象 promotion_reward_record（T16.2/T16.7，小微版 M16）
 * <p>首次消费触发，仅一次；UNIQUE(tenant_id, customer_user_id) 保证同一被拉新顾客只触发一次。
 * <p>★V1 零支付：issue_status 仅 PENDING/PAID_OFFLINE/ISSUED，无在线支付态。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_reward_record")
public class PromotionRewardRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 推广人用户ID(受奖人) */
    private Long promoterUserId;

    /** 推广人角色：salesman/customer/boss */
    private String promoterRoleCode;

    /** 被拉新顾客用户ID */
    private Long customerUserId;

    /** 触发拉新的首次消费订单ID */
    private Long firstOrderId;

    /** 奖励形式：cash/coupon(快照自配置) */
    private String rewardType;

    /** 现金奖励金额(reward_type=cash) */
    private BigDecimal rewardAmount;

    /** 优惠券发放记录ID(reward_type=coupon) */
    private Long couponRecordId;

    /** ★发放状态(V1零支付)：PENDING/PAID_OFFLINE/ISSUED */
    private String issueStatus;

    /** 发放时间 */
    private Date issuedTime;

    /** 是否已回传通知推广人小程序：0否 1是 */
    private Integer notified;

    private String remark;
}
