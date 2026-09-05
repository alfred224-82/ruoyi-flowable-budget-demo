package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 补票记录对象 fee_invoice_supplement（T5.8 / ADR-018）
 * 无发票后补通道：额度预占 → 补票转实扣 → 超时释放
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_invoice_supplement")
public class FeeInvoiceSupplement extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联报销单
     */
    private Long billId;

    /**
     * 补传发票
     */
    private String invoiceUrls;

    /**
     * OCR识别结果
     */
    private String ocrResult;

    /**
     * OCR识别金额
     */
    private BigDecimal ocrAmount;

    /**
     * 发票验真结果
     */
    private Integer verifyPassed;

    /**
     * 补票时间
     */
    private Date supplementDate;

    /**
     * 预计补票日期(默认提交日+7天)
     */
    private Date estimatedDate;

    /**
     * 额度预占金额(审批通过预占,补票转实扣,超时释放)
     */
    private BigDecimal reservedAmount;

    /**
     * 财务审核理由(后补单必填)
     */
    private String auditReason;

    /**
     * 状态(PENDING待补/COMPLETED已补/OVERDUE超时异常)
     */
    private String status;
}
