package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 计提记录对象 fee_accrual_record
 * 设计文档 Phase 6 Task 6.3
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_accrual_record")
public class FeeAccrualRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 计提期间（YYYY-MM） */
    private String accrualPeriod;

    /** 来源类型（ASSET_DEPRECIATION/CONTRACT_AMORTIZE/LOAN_INTEREST） */
    private String sourceType;

    /** 来源ID（资产卡片/合同/借款ID） */
    private Long sourceId;

    /** 来源编号 */
    private String sourceCode;

    /** 归属部门 */
    private Long deptId;

    /** 预算科目 */
    private String budgetSubjectCode;

    /** 轨道类型（计提全部归费用轨） */
    private String trackType;

    /** 计提金额 */
    private BigDecimal amount;

    /** 凭证状态（DRAFT/CONFIRMED/VOIDED） */
    private String voucherStatus;

    /** 财务复核人 */
    private Long confirmedBy;

    /** 复核时间 */
    private Date confirmedTime;

    /** 创建时间 */
    private Date createTime;
}
