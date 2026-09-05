package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 计费订单视图对象 sys_billing_order（T14.1）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysBillingOrderVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单号 */
    private String orderNo;

    /** 套餐编码 */
    private String planCode;

    /** 套餐名称(冗余展示) */
    private String planName;

    /** 订单金额(元) */
    private BigDecimal amount;

    /** 授权类型：TRIAL/FORMAL */
    private String licenseType;

    /** 授予版本 */
    private String edition;

    /** 服务开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date periodStart;

    /** 服务到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date periodEnd;

    /** 支付渠道 */
    private String payChannel;

    /** 支付状态：PENDING/PAID/REFUNDED/CANCELLED */
    private String payStatus;

    /** 第三方支付流水号 */
    private String transactionId;

    /** 关联 License 编号 */
    private String licenseId;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 备注 */
    private String remark;
}
