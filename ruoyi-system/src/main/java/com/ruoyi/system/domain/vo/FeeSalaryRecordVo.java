package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资计提记录视图对象 fee_salary_record（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeSalaryRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 计提月份 yyyy-MM */
    private String accrualMonth;
    /** 用户ID */
    private Long userId;
    /** 用户姓名 */
    private String userName;
    /** 月薪基数 */
    private BigDecimal baseAmount;
    /** 社保公司部分 */
    private BigDecimal socialAmount;
    /** 公积金公司部分 */
    private BigDecimal providentAmount;
    /** 其他补贴 */
    private BigDecimal allowanceAmount;
    /** 计提总额 */
    private BigDecimal totalAmount;
    /** 创建时间 */
    private Date createTime;
}
