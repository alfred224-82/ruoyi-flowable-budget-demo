package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 人员结算视图对象 staff_settlement（T15.5/T15.13）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class StaffSettlementVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 结算单号 */
    private String settleNo;

    /** 被结算员工/助教用户ID */
    private Long staffUserId;

    /** 员工姓名快照 */
    private String staffName;

    /** 结算周期起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodStart;

    /** 结算周期止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodEnd;

    /** 周期内完成单量 */
    private Integer totalOrders;

    /** 应发金额 */
    private BigDecimal totalAmount;

    /** 发放状态：PENDING(待发放)/PAID_OFFLINE(已线下发放) */
    private String payStatus;

    /** 标记人 */
    private String markedBy;

    /** 标记人用户ID */
    private Long markedById;

    /** 标记已线下发放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date markedTime;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
