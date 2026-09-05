package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 补票记录视图对象 fee_invoice_supplement（T5.8）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeInvoiceSupplementVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 关联报销单 */
    private Long billId;
    /** 报销单号(关联展示) */
    private String billNo;
    /** 报销金额(关联展示) */
    private BigDecimal billAmount;
    /** 补传发票 */
    private String invoiceUrls;
    /** OCR识别结果 */
    private String ocrResult;
    /** OCR识别金额 */
    private BigDecimal ocrAmount;
    /** 发票验真结果 */
    private Integer verifyPassed;
    /** 补票时间 */
    private Date supplementDate;
    /** 预计补票日期 */
    private Date estimatedDate;
    /** 额度预占金额 */
    private BigDecimal reservedAmount;
    /** 财务审核理由 */
    private String auditReason;
    /** 状态(PENDING/COMPLETED/OVERDUE) */
    private String status;
    /** 创建时间 */
    private Date createTime;
}
