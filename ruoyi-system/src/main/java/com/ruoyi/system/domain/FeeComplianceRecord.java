package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 发票合规校验记录对象 fee_compliance_record
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_compliance_record")
public class FeeComplianceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 报销单ID
     */
    private Long billId;

    /**
     * 报销单号
     */
    private String billNo;

    /**
     * 识别票据类型（FORMAL_INVOICE/RECEIPT/OTHER）
     */
    private String invoiceType;

    /**
     * 要求合规级别
     */
    private String requiredLevel;

    /**
     * 实际满足级别
     */
    private String actualLevel;

    /**
     * 校验结果（PASS/FAIL/PENDING_SEAL）
     */
    private String checkResult;

    /**
     * 不合规原因
     */
    private String failReason;

    /**
     * 财务盖章确认（0未确认1已确认）
     */
    private Integer sealConfirmed;

    /**
     * 盖章确认人ID
     */
    private Long sealConfirmerId;

    /**
     * 盖章确认时间
     */
    private Date sealConfirmTime;

    /**
     * 校验时间
     */
    private Date checkTime;
}
