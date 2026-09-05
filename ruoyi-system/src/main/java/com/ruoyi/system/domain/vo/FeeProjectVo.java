package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目信息视图对象 fee_project（T5.1）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目名称 */
    private String projectName;
    /** 客户名称 */
    private String clientName;
    /** 初始合同额 */
    private BigDecimal contractAmount;
    /** 当前合同额(含变更) */
    private BigDecimal currentContractAmount;
    /** 状态(DRAFT/PENDING_APPROVAL/ACTIVE/COMPLETED/CLOSED/LOST) */
    private String status;
    /** 开始日期 */
    private Date startDate;
    /** 结束日期 */
    private Date endDate;
    /** 项目经理ID */
    private Long projectManagerId;
    /** 项目经理姓名 */
    private String projectManagerName;
    /** 项目总预算 */
    private BigDecimal totalBudget;
    /** 已用预算 */
    private BigDecimal usedBudget;
    /** 已核验收入 */
    private BigDecimal verifiedIncome;
    /** 待核验收入 */
    private BigDecimal pendingIncome;
    /** 总支出(报销+付款) */
    private BigDecimal totalExpense;
    /** 烂账原因 */
    private String lossReason;
    /** 烂账类型 */
    private String lossType;
    /** 坏账计提金额 */
    private BigDecimal badDebtProvision;
    /** 变更次数 */
    private Integer changeCount;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
