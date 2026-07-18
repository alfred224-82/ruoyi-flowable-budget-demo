package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预留预算视图对象 fee_reserved_budget
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@ExcelIgnoreUnannotated
public class ReservedBudgetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 预算期间
     */
    @ExcelProperty("预算期间")
    private String budgetPeriod;

    /**
     * 层级类型：DEPT/BRANCH/HQ
     */
    @ExcelProperty("层级类型")
    private String levelType;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty("部门名称")
    private String deptName;

    /**
     * 预留预算总额度
     */
    @ExcelProperty("预留预算总额度")
    private BigDecimal totalAmount;

    /**
     * 已分摊金额
     */
    @ExcelProperty("已分摊金额")
    private BigDecimal allocatedAmount;

    /**
     * 剩余可用额度
     */
    @ExcelProperty("剩余可用额度")
    private BigDecimal availableAmount;

    /**
     * 预警阈值百分比
     */
    private BigDecimal warningThreshold;

    /**
     * 编制人ID
     */
    private Long creatorId;

    /**
     * 状态：ACTIVE/FROZEN/EXPIRED
     */
    @ExcelProperty("状态")
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;
}
