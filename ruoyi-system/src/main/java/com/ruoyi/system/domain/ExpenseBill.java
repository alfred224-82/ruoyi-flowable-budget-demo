package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_expense_bill")
public class ExpenseBill extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 报销单号
     */
    private String billNo;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 物资小类编码
     */
    private String itemCategory;

    /**
     * 用途标签编码
     */
    private String purposeTag;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 关联资金管控项目ID（T5.7，0=公司级，未选默认公司级）
     */
    private Long projectId;

    /**
     * 是否发票后补(0否 1是，T5.8 无发票后补通道)
     */
    private Integer invoiceDefer;

    /**
     * 计算后预算科目编码
     */
    private String calcBudgetSubject;

    /**
     * 计算后会计大类
     */
    private String calcAccountingClass;

    /**
     * 费用大类编码（映射B定位，T5.17 大类管控维度）
     */
    private String categoryCode;

    /**
     * 发票号码（T5.18 验真/查重）
     */
    private String invoiceNo;

    /**
     * 发票金额（与报销金额比对，T5.18）
     */
    private BigDecimal invoiceAmount;

    /**
     * 发票验真状态（UNVERIFIED/VERIFIED/MISMATCH，T5.18）
     */
    private String invoiceVerifyStatus;

    /**
     * 科目匹配阶段依据（EXACT/KEYWORD/FALLBACK/MANUAL，T5.18）
     */
    private String matchStage;

    /**
     * 报销金额（人民币）
     */
    private BigDecimal amount;

    /**
     * 币种编码（CNY/USD/EUR/GBP/JPY）
     */
    private String currencyCode;

    /**
     * 原币金额（外币时填写）
     */
    private BigDecimal originalAmount;

    /**
     * 使用汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 补助金额
     */
    private BigDecimal subsidyAmount;

    /**
     * 出差天数（用于补助核算）
     */
    private Integer travelDays;

    /**
     * 费用发生日期
     */
    private Date expenseDate;

    /**
     * 报销年月（格式：YYYY-MM，对应预算编制期间）
     */
    private String expensePeriod;

    /**
     * 是否固定资产(0否 1是)
     */
    private Integer isFixedAsset;

    /**
     * 使用年限(年)，仅固定资产时填写
     */
    private Integer usefulLife;

    /**
     * 支出类型(CAPITAL/OPERATING)
     */
    private String expenditureType;

    /**
     * 费用确认方式(DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE)
     */
    private String costMethod;

    /**
     * 是否超预算(0否 1是)
     */
    private Integer isOverBudget;

    /**
     * 关联超预算审批单ID
     */
    private Long overBudgetApprovalId;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    private String status;

    /**
     * 分摊状态(NONE/PENDING/ALLOCATED)
     */
    private String allocationStatus;

    /**
     * 合规校验状态（PASS/FAIL/PENDING_SEAL）
     */
    private String complianceStatus;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 驳回时间
     */
    private Date rejectTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 证据等级 strong/medium/weak（T6.7 / FEAT_EVIDENCE_CHAIN）
     */
    private String evidenceLevel;

    /**
     * 映射规则匹配链路JSON（T6.4 / FEAT_RULE_ENGINE_CUSTOM）
     */
    private String matchTrace;

    /**
     * 提交时信用等级快照（T6.3 / FEAT_CREDIT_SCORE）
     */
    private String creditLevel;

    /**
     * 是否已扣部门额度(0否 1是，T6.1)
     */
    private Integer deptBudgetDeducted;
}
