package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算对账单对象 sys_settlement_bill（T13.6/T14.3 按账期结算，金额可追溯到订单）
 * <p>表仅含 create_time/update_time，故不继承 BaseEntity；每租户每账期唯一（bill_no）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_settlement_bill")
public class SysSettlementBill implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 对账单号 */
    private String billNo;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 账期(yyyy-MM) */
    private String period;

    /** 订单笔数 */
    private Integer orderCount;

    /** 订单总额 */
    private BigDecimal grossAmount;

    /** 平台服务费合计 */
    private BigDecimal platformFee;

    /** 租户应得合计 */
    private BigDecimal tenantAmount;

    /** 状态：GENERATED/CONFIRMED */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
