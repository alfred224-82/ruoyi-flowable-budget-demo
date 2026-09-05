package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 分账记录视图对象 sys_profit_sharing_record（T14.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysProfitSharingRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联订单号 */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 平台服务费金额 */
    private BigDecimal platformAmount;

    /** 租户应得金额 */
    private BigDecimal tenantAmount;

    /** 分账时平台费率快照 */
    private BigDecimal platformRate;

    /** 状态：SUCCESS/FAILED/REVERSED */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
