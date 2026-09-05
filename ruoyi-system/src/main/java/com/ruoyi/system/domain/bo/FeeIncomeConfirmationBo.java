package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收入确认业务对象 fee_income_confirmation（T5.4）
 * 项目经理创建 → 待核验
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeIncomeConfirmationBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（查询用） */
    private Long id;

    /** 项目ID */
    @NotNull(message = "收入确认必须关联项目", groups = {AddGroup.class})
    private Long projectId;

    /** 确认收入金额 */
    @NotNull(message = "确认收入金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "确认收入金额必须大于0", groups = {AddGroup.class})
    private BigDecimal confirmedAmount;

    /** 确认日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date confirmDate;

    /** 依据类型(CONTRACT/INVOICE/RECEIPT/OTHER) */
    private String basisType;

    /** 依据编号 */
    private String basisNo;

    /** 依据附件 */
    private String basisUrls;

    /** 核验状态（查询用） */
    private String verifyStatus;
}
