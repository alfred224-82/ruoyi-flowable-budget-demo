package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约策略配置对象 booking_policy（T18.10，小微版 M18）
 * <p>每租户一条策略（UNIQUE tenant_id），upsert 模式。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("booking_policy")
public class BookingPolicy extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 是否允许顾客自助预约：1=允许 0=不允许 */
    private Integer customerSelfBooking;

    /** 提前预约天数 */
    private Integer advanceBookingDays;

    /** 取消政策：FREE/FEE/FORBID */
    private String cancelPolicy;

    /** 免费取消截止小时数 */
    private Integer cancelFreeHours;

    /** 爽约次数阈值 */
    private Integer noShowThreshold;

    /** 是否自动确认预约：1=自动 0=需确认 */
    private Integer autoConfirm;

    /** 每日最大预约数（NULL=不限） */
    private Integer maxDailyBookings;

    /** 项目资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer projectBookingEnabled;

    /** 项目资源是否必填：1=必填 0=可选（锁死行业强制1，REQ-01） */
    private Integer projectBookingRequired;

    /** 场地资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer roomBookingEnabled;

    /** 场地资源是否必填：1=必填 0=可选（REQ-01） */
    private Integer roomBookingRequired;

    /** 员工资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer staffBookingEnabled;

    /** 员工资源是否必填：1=必填 0=可选（REQ-01） */
    private Integer staffBookingRequired;

    private String remark;
}
