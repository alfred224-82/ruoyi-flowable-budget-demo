package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 跨部门分摊记录对象 fee_cross_dept_allocation_log
 * 设计文档 Phase 11 Task 11.1
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_cross_dept_allocation_log")
public class FeeCrossDeptAllocationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 分摊期间（YYYY-MM） */
    private String allocationPeriod;

    /** 来源部门（虚拟待分摊部门） */
    private Long sourceDeptId;

    /** 目标部门 */
    private Long targetDeptId;

    /** 预算科目 */
    private String budgetSubjectCode;

    /** 待分摊总额 */
    private BigDecimal totalAmount;

    /** 分摊比例 */
    private BigDecimal allocRatio;

    /** 分摊金额 */
    private BigDecimal allocAmount;

    /** 分摊依据（HEADCOUNT/WORKHOURS） */
    private String allocBasis;

    /** 调整凭证号 */
    private String voucherNo;

    /** 创建时间 */
    private Date createTime;
}
