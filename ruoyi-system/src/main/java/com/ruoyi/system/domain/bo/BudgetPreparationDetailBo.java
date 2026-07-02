package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.Date;
import java.math.BigDecimal;

/**
 * 预算编制明细表业务对象 budget_preparation_detail
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Data
public class BudgetPreparationDetailBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 预算单ID（外键，关联budget_preparation.id）
     */
    @NotNull(message = "预算单ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long sheetId;

    /**
     * 科目编码（关联budget_subject.subject_code）
     */
    @NotBlank(message = "科目编码不能为空", groups = { AddGroup.class, EditGroup.class })
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
    @DecimalMin(value = "0", message = "预算金额不能为负数", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal budgetAmount;

    /**
     * 实际金额
     */
    @DecimalMin(value = "0", message = "实际金额不能为负数", groups = { AddGroup.class, EditGroup.class })
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
