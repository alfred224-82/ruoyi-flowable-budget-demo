package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算科目调剂流水视图对象
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
public class BudgetAdjustmentLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 责任中心编码
     */
    private String costCenterGroup;

    /**
     * 调出科目编码
     */
    private String fromSubject;

    /**
     * 调入科目编码
     */
    private String toSubject;

    /**
     * 调剂金额
     */
    private BigDecimal adjustAmount;

    /**
     * 关联审批单ID
     */
    private Long relatedApprovalId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作时间
     */
    private Date operateTime;
}
