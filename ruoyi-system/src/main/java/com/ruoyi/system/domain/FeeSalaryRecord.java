package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 薪资计提记录对象 fee_salary_record（T5.9）
 * 每月1号 salaryAccrualJob 计提
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_salary_record")
public class FeeSalaryRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 计提月份 yyyy-MM
     */
    private String accrualMonth;

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
    private BigDecimal baseAmount;

    /**
     * 社保公司部分
     */
    private BigDecimal socialAmount;

    /**
     * 公积金公司部分
     */
    private BigDecimal providentAmount;

    /**
     * 其他补贴
     */
    private BigDecimal allowanceAmount;

    /**
     * 计提总额
     */
    private BigDecimal totalAmount;
}
