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
 * 分账记录对象 sys_profit_sharing_record（T14.3 每笔收款正确入账）
 * <p>表仅含 create_time，故不继承 BaseEntity；platform_amount + tenant_amount = order_amount（分账合计=订单金额）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_profit_sharing_record")
public class SysProfitSharingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 关联订单号 */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 平台服务费金额 */
    private BigDecimal platformAmount;

    /** 租户应得金额(入账虚拟账户) */
    private BigDecimal tenantAmount;

    /** 分账时平台费率快照 */
    private BigDecimal platformRate;

    /** 状态：SUCCESS/FAILED/REVERSED */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
