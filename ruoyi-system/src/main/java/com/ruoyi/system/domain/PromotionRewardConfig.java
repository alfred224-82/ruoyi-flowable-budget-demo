package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 拉新奖励配置对象 promotion_reward_config（T16.2/T16.7，小微版 M16）
 * <p>老板后台配置；奖励内容唯一可变，trigger_rule 恒为 NEW_CUSTOMER（拉新）不可改。
 * <p>target_role：staff(员工/业务员,发现金) / customer(顾客,发优惠券)。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_reward_config")
public class PromotionRewardConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 奖励对象：staff/customer */
    private String targetRole;

    /** 奖励形式：cash(现金)/coupon(优惠券) */
    private String rewardType;

    /** 奖励内容(唯一可变项) */
    private BigDecimal rewardValue;

    /** 顾客奖励关联的优惠券ID */
    private Long couponId;

    /** 触发规则(固定不可改)：NEW_CUSTOMER */
    private String triggerRule;

    /** 触发条件：NEW_CUSTOMER_BIND(新客绑定)/NEW_CUSTOMER_FIRST_ORDER(新客首单) */
    private String triggerCondition;

    /** 奖励上限周期：NONE(不限)/DAILY(每日)/MONTHLY(每月) */
    private String rewardLimitType;

    /** 奖励上限次数(rewardLimitType!=NONE时必填，>=1) */
    private Integer rewardLimitValue;

    /** 状态：0启用 1停用 */
    private String status;

    private String remark;
}
