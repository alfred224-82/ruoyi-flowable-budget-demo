package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约订单对象 booking_order（T15.5/T15.12，小微版）
 * <p>顾客预约 → 到店付；租户级表（含 tenant_id），多租户插件自动隔离。
 * <p>★V1 零支付合规：pay_status 仅 PENDING_PAY(待到店付)/RECEIVED(已收款)，无在线支付态；
 * order_status：PENDING_SERVICE(待服务)/COMPLETED(已完成)/CANCELLED(已取消)。
 * 老板标记「已收款」时服务层自动写 cash_flow 收入流水。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("booking_order")
public class BookingOrder extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 顾客用户ID（可空=游客预约，记手机号）
     */
    private Long customerUserId;

    /**
     * 顾客手机号
     */
    private String customerPhone;

    /**
     * 服务员工/助教用户ID
     */
    private Long staffUserId;

    /**
     * 服务项目ID（FK→service_item）
     */
    private Long serviceItemId;

    /**
     * 服务项目名称快照
     */
    private String serviceName;

    /**
     * 预约到店时间
     */
    private Date bookingTime;

    /**
     * 预约过期时间（T15.31 防爽约）：booking_time + 15 分钟；
     * 过期仍 PENDING_SERVICE 且未主动取消 → noShowCheckJob 判定为爽约(NO_SHOW)。
     */
    private Date expireTime;

    /**
     * 数量/时长（按计价方式）
     */
    private BigDecimal quantity;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 收款状态：PENDING_PAY(待到店付)/RECEIVED(已收款)；★无在线支付态
     */
    private String payStatus;

    /**
     * 订单状态：PENDING_SERVICE(待服务)/COMPLETED(已完成)/CANCELLED(已取消)
     */
    private String orderStatus;

    /**
     * 老板标记已收款时间（线下收款）
     */
    private Date payTime;

    /**
     * 完成服务时间
     */
    private Date completeTime;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
