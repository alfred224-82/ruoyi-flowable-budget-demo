package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工薪资视图对象 fee_staff_salary（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeStaffSalaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 用户姓名 */
    private String userName;
    /** 月薪基数 */
    private BigDecimal monthlyBase;
    /** 社保比例 */
    private BigDecimal socialRate;
    /** 社保公司部分 */
    private BigDecimal socialAmount;
    /** 公积金比例 */
    private BigDecimal providentRate;
    /** 公积金公司部分 */
    private BigDecimal providentAmount;
    /** 其他补贴 */
    private BigDecimal otherAllowance;
    /** 月度人力成本合计(计算字段) */
    private BigDecimal monthlyTotal;
    /** 状态(ACTIVE/RESIGNED) */
    private String status;
    /** 生效日期 */
    private Date effectiveDate;
    /** 创建时间 */
    private Date createTime;
}
