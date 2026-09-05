package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工薪资对象 fee_staff_salary（T5.9 / ADR-024，公司级方案A）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_staff_salary")
public class FeeStaffSalary extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 月薪基数
     */
    private BigDecimal monthlyBase;

    /**
     * 社保比例
     */
    private BigDecimal socialRate;

    /**
     * 社保公司部分
     */
    private BigDecimal socialAmount;

    /**
     * 公积金比例
     */
    private BigDecimal providentRate;

    /**
     * 公积金公司部分
     */
    private BigDecimal providentAmount;

    /**
     * 其他补贴
     */
    private BigDecimal otherAllowance;

    /**
     * 状态(ACTIVE/RESIGNED)
     */
    private String status;

    /**
     * 生效日期
     */
    private Date effectiveDate;
}
