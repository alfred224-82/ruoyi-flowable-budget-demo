package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

/**
 * 预算编制明细表对象 budget_preparation_detail
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
@TableName("budget_preparation_detail")
public class BudgetPreparationDetail implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;
    
    /**
     * 预算单ID（外键，关联budget_preparation.id）
     */
    private Long sheetId;
    
    /**
     * 科目编码（关联budget_subject.subject_code）
     */
    private String subjectCode;
    
    /**
     * 科目名称（冗余字段）
     */
    private String subjectName;
    
    /**
     * 科目类型（INCOME-收入类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类，COST-成本类）
     */
    private String subjectType;
    
    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;
    
    /**
     * 实际金额
     */
    private BigDecimal actualAmount;
    
    /**
     * 差异金额（自动计算：预算-实际）
     */
    private BigDecimal varianceAmount;
    
    /**
     * 差异率（百分比）
     */
    private BigDecimal varianceRate;
    
    /**
     * 部门ID（关联sys_dept.dept_id）
     */
    private Long deptId;
    
    /**
     * 部门名称（冗余字段）
     */
    private String deptName;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;

}
