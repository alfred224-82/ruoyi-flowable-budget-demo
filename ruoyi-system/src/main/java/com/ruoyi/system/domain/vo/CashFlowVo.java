package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收支流水视图对象 cash_flow（T15.5/T15.14）
 * <p>纯记账口径；支持收支明细导出（{@code @ExcelProperty} 标注导出列，micro:report:export）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@ExcelIgnoreUnannotated
public class CashFlowVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 流水类型：income(收入)/expense(支出) */
    @ExcelProperty(value = "流水类型")
    private String flowType;

    /** 收支分类 */
    @ExcelProperty(value = "收支分类")
    private String category;

    /** 金额 */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /** 关联单据ID */
    private Long relatedOrderId;

    /** 关联单据类型（booking/settlement/manual） */
    @ExcelProperty(value = "关联单据类型")
    private String relatedType;

    /** 发生日期 */
    @ExcelProperty(value = "发生日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date occurDate;

    /** 创建者（手动补录人） */
    @ExcelProperty(value = "补录人")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 备注 */
    @ExcelProperty(value = "备注")
    private String remark;
}
