package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * 预算编制主表视图对象 budget_preparation
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetPreparationVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 预算单号（唯一，格式：BG-YYYYMM-XXX）
     */
    @ExcelProperty(value = "预算单号")
    private String sheetNo;

    /**
     * 组织ID（关联sys_dept表的dept_id）
     */
    @ExcelProperty(value = "组织ID")
    private Long orgId;

    /**
     * 组织名称（冗余字段，便于查询）
     */
    @ExcelProperty(value = "组织名称")
    private String orgName;

    /**
     * 预算年度
     */
    @ExcelProperty(value = "预算年度")
    private Integer budgetYear;

    /**
     * 预算月份（1-12）
     */
    @ExcelProperty(value = "预算月份")
    private Integer budgetMonth;

    /**
     * 预算期间（格式：YYYY-MM，自动生成）
     */
    @ExcelProperty(value = "预算期间")
    private String budgetPeriod;

    /**
     * 生命周期状态（Draft-草稿，Pending_Review-待审核，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 审批阶段（None-无，Dept-部门领导，Branch-分公司领导，HQ-总公司领导）
     */
    @ExcelProperty(value = "审批阶段")
    private String approvalStage;

    /**
     * 驳回来源（HQ-总部，Branch-分公司，None-无）
     */
    @ExcelProperty(value = "驳回来源")
    private String rejectLevel;

    /**
     * 驳回理由
     */
    @ExcelProperty(value = "驳回理由")
    private String rejectReason;

    /**
     * 截止时间
     */
    @ExcelProperty(value = "截止时间")
    private Date deadlineTime;

    /**
     * 当前处理人（用户账号）
     */
    @ExcelProperty(value = "当前处理人")
    private String currentHandler;

    /**
     * 预算总额（自动汇总明细表）
     */
    @ExcelProperty(value = "预算总额")
    private BigDecimal totalBudget;

    /**
     * 实际总额
     */
    @ExcelProperty(value = "实际总额")
    private BigDecimal totalActual;

    /**
     * 差异率（百分比）
     */
    @ExcelProperty(value = "差异率")
    private BigDecimal varianceRate;

    /**
     * Flowable流程实例ID
     */
    @ExcelProperty(value = "流程实例ID")
    private String processInstanceId;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

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
