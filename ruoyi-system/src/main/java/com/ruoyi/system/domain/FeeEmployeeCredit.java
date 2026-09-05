package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 员工信用分对象 fee_employee_credit（T6.3 / FEAT_CREDIT_SCORE）
 * 信用等级：EXCELLENT优秀≥80 / NORMAL正常50-79 / RESTRICTED受限<50
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_employee_credit")
public class FeeEmployeeCredit extends TenantEntity {

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
     * 当前信用分(默认100)
     */
    private Integer creditScore;

    /**
     * 信用等级(EXCELLENT优秀≥80/NORMAL正常50-79/RESTRICTED受限<50)
     */
    private String creditLevel;

    /**
     * 按时补票次数
     */
    private Integer onTimeCount;

    /**
     * 逾期次数
     */
    private Integer overdueCount;

    /**
     * 连续按时次数
     */
    private Integer consecutiveOnTime;

    /**
     * 最近重算日期(creditScoreCalcJob每周一)
     */
    private Date lastCalcDate;
}
