package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 优惠券定义对象 coupon（T16.3，小微版 M16）
 * <p>站内券模板；顾客拉新奖励 / 活动优惠的券定义；按 tenant_id 隔离。
 * <p>type：discount(折扣)/gift(赠品)/cash(代金)；valid_days 领取后有效天数。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon")
public class Coupon extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 券类型：discount(折扣)/gift(赠品)/cash(代金) */
    private String type;

    /** 券值：discount=折扣(如8.0折)/cash=面额(元) */
    private BigDecimal value;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 发行总量(NULL=不限量) */
    private Integer total;

    /** 已发放数量 */
    private Integer issuedCount;

    /** 状态：0启用 1停用 */
    private String status;

    @TableLogic
    private String delFlag;

    private String remark;
}
