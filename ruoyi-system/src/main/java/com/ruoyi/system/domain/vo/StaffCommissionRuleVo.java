package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工分成规则视图对象 staff_commission_rule（M35 T35.2）
 * <p>老板 PC 端列表 + 新增/编辑回显；含员工姓名/部门回填。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@ExcelIgnoreUnannotated
public class StaffCommissionRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 员工用户ID */
    private Long staffId;

    /** 员工姓名（回填，非表字段） */
    @ExcelProperty(value = "员工姓名")
    private String staffName;

    /** 部门名称（回填，非表字段） */
    @ExcelProperty(value = "部门")
    private String deptName;

    /** 分成类型：FIXED_AMOUNT/PERCENTAGE/TIERED */
    @ExcelProperty(value = "分成类型")
    private String commissionType;

    /** 固定金额（元/场） */
    @ExcelProperty(value = "固定金额")
    private BigDecimal fixedAmount;

    /** 百分比（0~100） */
    @ExcelProperty(value = "百分比")
    private BigDecimal percentageRate;

    /** 阶梯规则 JSON（预留） */
    private String tieredConfig;

    /** 追加消费是否计提成：0=否 1=是 */
    @ExcelProperty(value = "追加消费计提成")
    private Integer includeAddonInCommission;

    /** 月底薪 */
    @ExcelProperty(value = "月底薪")
    private BigDecimal baseSalary;

    /** 是否启用：0=停用 1=启用 */
    @ExcelProperty(value = "启用")
    private Integer isActive;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
