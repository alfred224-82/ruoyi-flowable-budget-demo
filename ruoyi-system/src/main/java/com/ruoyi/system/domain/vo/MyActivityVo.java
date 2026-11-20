package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 顾客/助教「我的活动」统一视图对象（M22）
 * <p>合并两类来源：自助预约订单（BOOKING）+ 活动报名（ACTIVITY_SIGNUP），
 * 前端统一渲染为活动卡片列表。
 *
 * @author ruoyi
 * @date 2026-10-11
 */
@Data
public class MyActivityVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源类型：BOOKING（自助预约）/ ACTIVITY_SIGNUP（活动报名） */
    private String sourceType;

    /** 关联ID（booking_order.id 或 promotion_activity.id） */
    private Long bizId;

    /** 活动/服务标题 */
    private String title;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 封面图URL */
    private String coverUrl;

    /** 状态（booking_order.order_status 或 promotion_activity.status） */
    private String status;

    /** 服务项目名称 */
    private String serviceName;

    /** 助教名称 */
    private String staffName;

    // ===== M22 费用字段 =====

    /** 总金额 */
    private BigDecimal amount;

    /** 人均费用（活动报名时有值） */
    private BigDecimal perPersonPrice;

    /** 参与人数（活动报名时为 Boss 设定人数） */
    private Integer participantCount;

    /** 支付状态（活动报名：PENDING_PAY/PAID/REMIND_SENT） */
    private String payStatus;

    /** 活动是否已锁定 */
    private Boolean isLocked;

    /** 角色标签（租户角色映射动态显示名，如司机/导游/助教；多主持人去重保序顿号拼接；null 时前端回退「主持」） */
    private String roleLabel;

    /** 主持人名称拼接（如 "黄导游、刘司机"） */
    private String hostNames;

    /** 主持人头像 URL 列表（与 hostNames 按顿号拆分后一一对应） */
    private java.util.List<String> hostAvatars;

    /** 当前已选座位数 */
    private Integer currentSeats;

    /** 总座位数 */
    private Integer totalSeats;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
