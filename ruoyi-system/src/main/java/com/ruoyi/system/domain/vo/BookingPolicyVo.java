package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 预约策略配置 VO（T18.10，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class BookingPolicyVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer customerSelfBooking;
    private Integer advanceBookingDays;
    private String cancelPolicy;
    private Integer cancelFreeHours;
    private Integer noShowThreshold;
    private Integer autoConfirm;
    private Integer maxDailyBookings;
    /** 项目资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer projectBookingEnabled;
    /** 项目资源是否必填：1=必填 0=可选（REQ-01） */
    private Integer projectBookingRequired;
    /** 场地资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer roomBookingEnabled;
    /** 场地资源是否必填：1=必填 0=可选（REQ-01） */
    private Integer roomBookingRequired;
    /** 员工资源是否对客户开放：1=开放 0=隐藏（REQ-01） */
    private Integer staffBookingEnabled;
    /** 员工资源是否必填：1=必填 0=可选（REQ-01） */
    private Integer staffBookingRequired;
    /** 当前租户行业编码（锁死行业前端置灰提示用，REQ-01） */
    private String industryCode;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
