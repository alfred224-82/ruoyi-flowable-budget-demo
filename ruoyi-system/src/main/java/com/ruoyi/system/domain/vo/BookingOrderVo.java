package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约订单视图对象 booking_order（T15.5/T15.12）
 * <p>{@code customerName}/{@code staffName} 为服务层回填的用户昵称（来自 sys_user），便于列表展示，非表字段。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class BookingOrderVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单编号 */
    private String orderNo;

    /** 顾客用户ID */
    private Long customerUserId;

    /** 顾客昵称（回填，非表字段） */
    private String customerName;

    /** 顾客手机号 */
    private String customerPhone;

    /** 服务员工/助教用户ID */
    private Long staffUserId;

    /** 员工昵称（回填，非表字段） */
    private String staffName;

    /** 服务项目ID */
    private Long serviceItemId;

    /** 服务项目名称快照 */
    private String serviceName;

    /** 预约到店时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date bookingTime;

    /** 数量/时长 */
    private BigDecimal quantity;

    /** 订单金额 */
    private BigDecimal amount;

    /** 收款状态：PENDING_PAY(待到店付)/RECEIVED(已收款) */
    private String payStatus;

    /** 订单状态：PENDING_SERVICE/COMPLETED/CANCELLED */
    private String orderStatus;

    /** 老板标记已收款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 完成服务时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    /** 备注 */
    private String remark;

    /** 数据来源：BOOKING(顾客自主预约 booking_order)/ACTIVITY(boss 后台创建的活动 promotion_activity)，非表字段 */
    private String sourceType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
