package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算科目调剂流水对象 fee_budget_adjustment_log
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@TableName("fee_budget_adjustment_log")
public class BudgetAdjustmentLog {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 调出科目编码
     */
    private String fromSubject;

    /**
     * 调出科目名称
     */
    private String fromSubjectName;

    /**
     * 调入科目编码
     */
    private String toSubject;

    /**
     * 调入科目名称
     */
    private String toSubjectName;

    /**
     * 调剂金额
     */
    private BigDecimal adjustAmount;

    /**
     * 关联审批单ID
     */
    private Long relatedApprovalId;

    /**
     * 关联报销单号
     */
    private String relatedBillNo;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private Date operateTime;
}
