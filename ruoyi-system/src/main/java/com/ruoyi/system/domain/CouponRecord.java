package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 顾客券记录对象 coupon_record（T16.3，小微版 M16）
 * <p>发放到顾客账户；source 区分 promotion(拉新奖励系统发放) / activity(活动领取)。
 * <p>status：UNUSED(未使用)/USED(已使用)/EXPIRED(已过期)。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_record")
public class CouponRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 持券顾客用户ID */
    private Long userId;

    /** 优惠券ID(指向 coupon.id) */
    private Long couponId;

    /** 来源：promotion(拉新奖励系统发放)/activity(活动领取) */
    private String source;

    /** 状态：UNUSED/USED/EXPIRED */
    private String status;

    /** 到账/领取时间 */
    private Date receiveTime;

    /** 过期时间(=receive_time+coupon.valid_days) */
    private Date expireTime;

    /** 使用时间 */
    private Date usedTime;

    private String remark;
}
