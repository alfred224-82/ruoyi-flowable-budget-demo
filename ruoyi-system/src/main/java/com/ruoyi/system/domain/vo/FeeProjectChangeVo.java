package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同变更视图对象 fee_project_change（T5.5）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectChangeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 变更类型 */
    private String changeType;
    /** 变更原因 */
    private String changeReason;
    /** 原合同额 */
    private BigDecimal oldContractAmount;
    /** 新合同额 */
    private BigDecimal newContractAmount;
    /** 合同额变更量 */
    private BigDecimal contractDelta;
    /** 按大类预算变更 JSON */
    private String budgetDeltas;
    /** 变更状态(PENDING/REJECTED/CONFIRMED/OVERDUE) */
    private String changeStatus;
    /** 依据附件 */
    private String basisUrls;
    /** 发起人 */
    private Long createdBy;
    /** 审批人 */
    private Long approvedBy;
    /** 审批时间 */
    private Date approvedAt;
    /** 收款确认时间 */
    private Date confirmedAt;
    /** 创建时间 */
    private Date createTime;
}
