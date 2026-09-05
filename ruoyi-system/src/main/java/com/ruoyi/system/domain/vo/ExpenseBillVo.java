package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单视图对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@ExcelIgnoreUnannotated
public class ExpenseBillVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 报销单号
     */
    @ExcelProperty(value = "报销单号")
    private String billNo;

    /**
     * 部门ID
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    @ExcelProperty(value = "申请人")
    private String applicantName;

    /**
     * 物资小类编码
     */
    @ExcelProperty(value = "物资小类编码")
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @ExcelProperty(value = "用途标签编码")
    private String purposeTag;

    /**
     * 用途标签名称
     */
    @ExcelProperty(value = "用途标签名称")
    private String purposeTagName;

    /**
     * 项目编号
     */
    @ExcelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 关联资金管控项目ID（T5.7，0=公司级）
     */
    private Long projectId;

    /**
     * 关联项目名称（小程序展示用，关联查询填充）
     */
    private String projectName;

    /**
     * 是否发票后补(0否 1是，T5.8)
     */
    private Integer invoiceDefer;

    /**
     * 计算后预算科目编码
     */
    @ExcelProperty(value = "预算科目编码")
    private String calcBudgetSubject;

    /**
     * 预算科目名称
     */
    @ExcelProperty(value = "预算科目名称")
    private String calcBudgetSubjectName;

    /**
     * 计算后会计大类
     */
    @ExcelProperty(value = "会计大类")
    private String calcAccountingClass;

    /**
     * 费用大类编码（T5.17 大类管控维度）
     */
    @ExcelProperty(value = "费用大类")
    private String categoryCode;

    /**
     * 发票号码（T5.18）
     */
    private String invoiceNo;

    /**
     * 发票金额（T5.18）
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
    @ExcelProperty(value = "报销金额")
    private BigDecimal amount;

    /**
     * 币种编码
     */
    @ExcelProperty(value = "币种")
    private String currencyCode;

    /**
     * 原币金额
     */
    @ExcelProperty(value = "原币金额")
    private BigDecimal originalAmount;

    /**
     * 使用汇率
     */
    @ExcelProperty(value = "汇率")
    private BigDecimal exchangeRate;

    /**
     * 补助金额
     */
    @ExcelProperty(value = "补助金额")
    private BigDecimal subsidyAmount;

    /**
     * 出差天数
     */
    @ExcelProperty(value = "出差天数")
    private Integer travelDays;

    /**
     * 费用发生日期
     */
    @ExcelProperty(value = "费用发生日期")
    private Date expenseDate;

    /**
     * 报销年月（格式：YYYY-MM，对应预算编制期间）
     */
    @ExcelProperty(value = "报销年月")
    private String expensePeriod;

    /**
     * 是否固定资产(0否 1是)
     */
    @ExcelProperty(value = "是否固定资产")
    private Integer isFixedAsset;

    /**
     * 使用年限(年)，仅固定资产时填写
     */
    @ExcelProperty(value = "使用年限(年)")
    private Integer usefulLife;

    /**
     * 支出类型(CAPITAL/OPERATING)
     */
    @ExcelProperty(value = "支出类型")
    private String expenditureType;

    /**
     * 费用确认方式(DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE)
     */
    @ExcelProperty(value = "费用确认方式")
    private String costMethod;

    /**
     * 是否超预算(0否 1是)
     */
    @ExcelProperty(value = "是否超预算")
    private Integer isOverBudget;

    /**
     * 关联超预算审批单ID
     */
    @ExcelProperty(value = "超预算审批单ID")
    private Long overBudgetApprovalId;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 分摊状态(NONE/PENDING/ALLOCATED)
     */
    @ExcelProperty(value = "分摊状态")
    private String allocationStatus;

    /**
     * 合规校验状态（PASS/FAIL/PENDING_SEAL）
     */
    @ExcelProperty(value = "合规状态")
    private String complianceStatus;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 证据等级 strong/medium/weak（T6.7 / FEAT_EVIDENCE_CHAIN）
     */
    private String evidenceLevel;

    /**
     * 映射规则匹配链路JSON（T6.4）
     */
    private String matchTrace;

    /**
     * 提交时信用等级快照（T6.3）
     */
    private String creditLevel;

    /**
     * 驳回原因（小程序展示驳回待办用）
     */
    private String rejectReason;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
