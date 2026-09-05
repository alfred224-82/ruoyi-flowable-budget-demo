package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 提现申请视图对象 sys_withdrawal_request（T14.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysWithdrawalRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 提现单号 */
    private String withdrawNo;

    /** 租户ID */
    private Long tenantId;

    /** 提现金额 */
    private BigDecimal amount;

    /** 收款账户信息(脱敏) */
    private String accountInfo;

    /** 状态：PENDING/APPROVED/REJECTED/PAID */
    private String status;

    /** 审核人 */
    private String auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 申请人 */
    private String createBy;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
