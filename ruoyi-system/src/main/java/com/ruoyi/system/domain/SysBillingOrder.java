package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 计费订单对象 sys_billing_order（T14.1；与 License 一一对应）
 * <p>下单写 license_type + period_end（回写 sys_tenant.expire_date）；【严禁自动扣款】。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_billing_order")
public class SysBillingOrder extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 套餐编码(sys_billing_plan.plan_code) */
    private String planCode;

    /** 订单金额(元) */
    private BigDecimal amount;

    /** 授权类型：TRIAL/FORMAL */
    private String licenseType;

    /** 授予版本 */
    private String edition;

    /** 服务开始时间 */
    private Date periodStart;

    /** 服务到期时间(写入 sys_tenant.expire_date) */
    private Date periodEnd;

    /** 支付渠道：wechat/alipay/offline/manual */
    private String payChannel;

    /** 支付状态：PENDING/PAID/REFUNDED/CANCELLED */
    private String payStatus;

    /** 第三方支付流水号 */
    private String transactionId;

    /** 关联签发的 License 编号(sys_license.license_id) */
    private String licenseId;

    /** 支付时间 */
    private Date payTime;

    /** 备注 */
    private String remark;
}
