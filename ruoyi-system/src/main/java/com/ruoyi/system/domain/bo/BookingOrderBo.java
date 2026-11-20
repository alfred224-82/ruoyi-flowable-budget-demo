package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约订单业务对象 booking_order（T15.12）
 * <p>老板 PC 帮顾客约/编辑；顾客小程序提交预约（生成「待到店付」订单）。
 * amount 为空时由服务层按 service_item.price × quantity 计算；serviceName 由服务层快照。
 * 兼作查询过滤：orderNo/payStatus/orderStatus/customerPhone/staffUserId/serviceItemId。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class BookingOrderBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "订单ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 订单编号（查询过滤用） */
    private String orderNo;

    /** 顾客用户ID（可空=游客预约） */
    private Long customerUserId;

    /** 顾客手机号 */
    private String customerPhone;

    /** 服务员工/助教用户ID */
    private Long staffUserId;

    /** 服务项目ID */
    @NotNull(message = "服务项目不能为空", groups = {AddGroup.class})
    private Long serviceItemId;

    /** 预约到店时间（小程序日期选择器传 yyyy-MM-dd） */
    @NotNull(message = "预约时间不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date bookingTime;

    /** 数量/时长（默认 1） */
    private BigDecimal quantity;

    /** 订单金额（为空则按服务项目单价×数量计算） */
    private BigDecimal amount;

    /** 收款状态（查询过滤用）：PENDING_PAY/RECEIVED */
    private String payStatus;

    /** 订单状态（查询过滤用）：PENDING_SERVICE/COMPLETED/CANCELLED */
    private String orderStatus;

    /** 备注 */
    private String remark;
}
