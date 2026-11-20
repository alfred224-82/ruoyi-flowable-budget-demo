package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * 顾客券记录查询业务对象 coupon_record（T16.3，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class CouponRecordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 持券顾客用户ID */
    private Long userId;

    /** 优惠券ID */
    private Long couponId;

    /** 来源：promotion/activity */
    private String source;

    /** 状态：UNUSED/USED/EXPIRED */
    private String status;
}
