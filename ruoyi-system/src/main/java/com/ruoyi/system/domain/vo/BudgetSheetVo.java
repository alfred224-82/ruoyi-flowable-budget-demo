package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
public class BudgetSheetVo {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "预算单号")
    private String sheetNo;

    @ExcelProperty(value = "组织ID")
    private Long orgId;

    @ExcelProperty(value = "组织名称")
    private String orgName;

    @ExcelProperty(value = "预算月份")
    private String budgetMonth;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "驳回来源")
    private String rejectLevel;

    @ExcelProperty(value = "驳回理由")
    private String rejectReason;

    @ExcelProperty(value = "截止时间")
    private Date deadlineTime;

    @ExcelProperty(value = "当前处理人")
    private String currentHandler;

    @ExcelProperty(value = "预算总额")
    private BigDecimal totalBudget;

    @ExcelProperty(value = "实际总额")
    private BigDecimal totalActual;

    @ExcelProperty(value = "差异率")
    private BigDecimal varianceRate;

    @ExcelProperty(value = "流程实例ID")
    private String processInstanceId;

    @ExcelProperty(value = "备注")
    private String remark;
}
