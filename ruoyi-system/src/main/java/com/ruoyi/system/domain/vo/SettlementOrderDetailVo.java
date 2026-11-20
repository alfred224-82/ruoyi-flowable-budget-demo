package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算单关联订单明细视图（T15.13，供结算明细弹窗展示）
 * <p>包含顾客、服务活动、时长、开始/结束时间，方便查证。
 *
 * @author ruoyi
 * @date 2026-10-11
 */
@Data
public class SettlementOrderDetailVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 顾客昵称 */
    private String customerName;

    /** 顾客手机号 */
    private String customerPhone;

    /** 服务活动/项目名称 */
    private String serviceName;

    /** 服务时长（小时） */
    private BigDecimal hours;

    /** 助教费用（从 costDetails 提取 type='助教' 的 subtotal 之和） */
    private BigDecimal staffFee;

    /** 开始时间（预约到店时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date startTime;

    /** 结束时间（bookingTime + hours 小时） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date endTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date completeTime;
}
