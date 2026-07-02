package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * 预算编制明细表视图对象 budget_preparation_detail
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetPreparationDetailVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 预算单ID（外键，关联budget_preparation.id）
     */
    @ExcelProperty(value = "预算单ID")
    private Long sheetId;

    /**
     * 科目编码（关联budget_subject.subject_code）
     */
    @ExcelProperty(value = "科目编码")
    private String subjectCode;

    /**
     * 科目名称（冗余字段）
     */
    @ExcelProperty(value = "科目名称")
    private String subjectName;

    /**
     * 科目类型（INCOME-收入类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类，COST-成本类）
     */
    @ExcelProperty(value = "科目类型")
    private String subjectType;

    /**
     * 预算金额
     */
    @ExcelProperty(value = "预算金额")
    private BigDecimal budgetAmount;

    /**
     * 实际金额
     */
    @ExcelProperty(value = "实际金额")
    private BigDecimal actualAmount;

    /**
     * 差异金额（自动计算：预算-实际）
     */
    @ExcelProperty(value = "差异金额")
    private BigDecimal varianceAmount;

    /**
     * 差异率（百分比）
     */
    @ExcelProperty(value = "差异率")
    private BigDecimal varianceRate;

    /**
     * 部门ID（关联sys_dept.dept_id）
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 部门名称（冗余字段）
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
