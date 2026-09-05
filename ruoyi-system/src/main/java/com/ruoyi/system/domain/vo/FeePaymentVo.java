package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 付款单视图对象 fee_payment（T5.2）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeePaymentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 付款单号 */
    private String paymentNo;
    /** 关联项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 收款方(对方) */
    private String payee;
    /** 付款方(我方) */
    private String payer;
    /** 付款金额 */
    private BigDecimal amount;
    /** 付款类型 */
    private String payType;
    /** 付款日期 */
    private Date paymentDate;
    /** 发票状态(HAS/NONE/PENDING) */
    private String invoiceStatus;
    /** 发票附件 */
    private String invoiceUrls;
    /** 合同/验收单附件 */
    private String contractUrls;
    /** 说明 */
    private String description;
    /** 状态(PENDING/APPROVED/REJECTED) */
    private String status;
    /** 审批人 */
    private Long approvedBy;
    /** 审批时间 */
    private Date approvedAt;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private Date createTime;
}
