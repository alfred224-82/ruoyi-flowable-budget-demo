package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 首单识别记录对象 first_order_record（T16.4/T16.8，小微版 M16）
 * <p>手机号唯一识别维度、按活动独立判断是否首次；UNIQUE(tenant_id, activity_id, customer_phone)。
 * <p>★首单判定（管优惠）与推广归属（管业绩）两条线独立互不干扰。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("first_order_record")
public class FirstOrderRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 活动ID(每个活动独立判断是否首次) */
    private Long activityId;

    /** ★顾客手机号(首单唯一识别维度) */
    private String customerPhone;

    /** 顾客用户ID(冗余,便于关联归因) */
    private Long customerUserId;

    /** 首次消费订单ID(booking_order.id) */
    private Long firstOrderId;

    /** 首次消费时间 */
    private Date consumeTime;
}
