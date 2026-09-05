package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收入确认视图对象 fee_income_confirmation（T5.4）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeIncomeConfirmationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 确认收入金额 */
    private BigDecimal confirmedAmount;
    /** 确认日期 */
    private Date confirmDate;
    /** 依据类型(CONTRACT/INVOICE/RECEIPT/OTHER) */
    private String basisType;
    /** 依据编号 */
    private String basisNo;
    /** 依据附件 */
    private String basisUrls;
    /** 创建人 */
    private Long confirmedBy;
    /** 创建人姓名 */
    private String confirmedByName;
    /** 核验状态(PENDING/VERIFIED/REJECTED) */
    private String verifyStatus;
    /** 核验人 */
    private Long verifyBy;
    /** 核验时间 */
    private Date verifyAt;
    /** 驳回原因 */
    private String rejectReason;
    /** 创建时间 */
    private Date createTime;
}
