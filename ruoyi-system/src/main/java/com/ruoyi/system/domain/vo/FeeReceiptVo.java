package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收款单视图对象 fee_receipt（T5.3）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeReceiptVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 收款单号 */
    private String receiptNo;
    /** 关联项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 付款方(对方) */
    private String payer;
    /** 收款金额 */
    private BigDecimal amount;
    /** 收款类型(PROJECT_PAYMENT/REFUND/OTHER) */
    private String receiptType;
    /** 收款日期 */
    private Date receivedDate;
    /** 回单附件 */
    private String proofUrls;
    /** 状态(PENDING/APPROVED/REJECTED) */
    private String status;
    /** 审核人 */
    private Long approvedBy;
    /** 审核时间 */
    private Date approvedAt;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private Date createTime;
}
