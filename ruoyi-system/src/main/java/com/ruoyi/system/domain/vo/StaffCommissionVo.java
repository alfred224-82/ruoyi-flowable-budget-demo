package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工提成比例配置视图对象 staff_commission（REQ-03）
 * <p>platform_admin PC 端列表 + 新增/编辑回显；含服务端实时回填的当月基数与当月提成（只读）。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@Data
@ExcelIgnoreUnannotated
public class StaffCommissionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 员工用户ID */
    private Long staffUserId;

    /** 员工姓名 */
    @ExcelProperty(value = "员工姓名")
    private String staffName;

    /** 部门名称（回填，非表字段） */
    @ExcelProperty(value = "部门")
    private String deptName;

    /** 提成比例（0~1 小数，如 0.1000=10%） */
    @ExcelProperty(value = "提成比例")
    private BigDecimal commissionRate;

    /** 基础工资（元/月） */
    @ExcelProperty(value = "基础工资")
    private BigDecimal baseSalary;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date effectiveDate;

    /** 状态（0-正常；1-停用） */
    private String status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ===== 服务端实时回填（只读，非表字段） =====

    /** 当月已完成订单金额（提成基数，复用 StaffSettlement 口径） */
    @ExcelProperty(value = "当月已完成订单金额")
    private BigDecimal monthlyCompletedAmount;

    /** 当月提成（= 当月基数 × 提成比例，2 位 HALF_UP） */
    @ExcelProperty(value = "当月提成")
    private BigDecimal monthlyCommission;

    /** 当月人力成本（= 基础工资 + 当月提成） */
    @ExcelProperty(value = "当月人力成本")
    private BigDecimal monthlyLaborCost;
}
