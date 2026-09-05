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
 * 收款单业务对象 fee_receipt（T5.3）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeReceiptBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（查询用） */
    private Long id;

    /** 收款单号（查询用） */
    private String receiptNo;

    /** 关联项目(必选) */
    @NotNull(message = "收款单必须关联项目", groups = {AddGroup.class})
    private Long projectId;

    /** 付款方(对方) */
    @NotBlank(message = "付款方不能为空", groups = {AddGroup.class})
    private String payer;

    /** 收款金额 */
    @NotNull(message = "收款金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "收款金额必须大于0", groups = {AddGroup.class})
    private BigDecimal amount;

    /** 收款类型(PROJECT_PAYMENT/REFUND/OTHER) */
    private String receiptType;

    /** 收款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date receivedDate;

    /** 回单附件 */
    private String proofUrls;

    /** 状态（查询用） */
    private String status;
}
