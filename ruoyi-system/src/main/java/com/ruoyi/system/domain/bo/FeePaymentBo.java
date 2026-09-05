package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 付款单业务对象 fee_payment（T5.2）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeePaymentBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（查询用） */
    private Long id;

    /** 付款单号（查询用） */
    private String paymentNo;

    /** 关联项目(必选) */
    @NotNull(message = "付款单必须关联项目", groups = {AddGroup.class})
    private Long projectId;

    /** 收款方(对方) */
    @NotBlank(message = "收款方不能为空", groups = {AddGroup.class})
    private String payee;

    /** 付款方(我方) */
    private String payer;

    /** 付款金额 */
    @NotNull(message = "付款金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "付款金额必须大于0", groups = {AddGroup.class})
    private BigDecimal amount;

    /** 付款类型(ENGINEERING/MATERIAL/SERVICE/RENT/OTHER) */
    private String payType;

    /** 付款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date paymentDate;

    /** 发票状态(HAS/NONE/PENDING) */
    private String invoiceStatus;

    /** 发票附件 */
    private String invoiceUrls;

    /** 合同/验收单附件 */
    private String contractUrls;

    /** 说明 */
    private String description;

    /** 状态（查询用） */
    private String status;
}
