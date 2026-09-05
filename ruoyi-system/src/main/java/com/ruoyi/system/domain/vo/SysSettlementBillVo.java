package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算对账单视图对象 sys_settlement_bill（T13.6/T14.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysSettlementBillVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 对账单号 */
    private String billNo;

    /** 租户ID */
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
