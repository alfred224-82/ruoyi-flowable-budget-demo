package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算驳回历史记录视图对象 budget_reject_history
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetRejectHistoryVo {

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 预算单ID
     */
    @ExcelProperty(value = "预算单ID")
    private Long sheetId;

    /**
     * 预算单号
     */
    @ExcelProperty(value = "预算单号")
    private String sheetNo;

    /**
     * 驳回方层级
     */
    @ExcelProperty(value = "驳回方层级")
    private String rejectFromLevel;

    /**
     * 驳回人
     */
    @ExcelProperty(value = "驳回人")
    private String rejectFromName;

    /**
     * 被驳回方层级
     */
    @ExcelProperty(value = "被驳回方层级")
    private String rejectToLevel;

    /**
     * 被驳回部门
     */
    @ExcelProperty(value = "被驳回部门")
    private String rejectToDeptName;

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
     * 驳回时间
     */
    @ExcelProperty(value = "驳回时间")
    private Date rejectTime;

    /**
     * 处理时间
     */
    @ExcelProperty(value = "处理时间")
    private Date handleTime;

    /**
     * 处理耗时(小时)
     */
    @ExcelProperty(value = "处理耗时(小时)")
    private BigDecimal handleDurationHours;

    /**
     * 是否超时
     */
    @ExcelProperty(value = "是否超时")
    private Integer isTimeout;
}
