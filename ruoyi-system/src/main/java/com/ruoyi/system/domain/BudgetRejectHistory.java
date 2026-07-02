package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算驳回历史记录对象 budget_reject_history
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_reject_history")
public class BudgetRejectHistory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 预算单ID
     */
    private Long sheetId;

    /**
     * 预算单号
     */
    private String sheetNo;

    /**
     * 驳回方层级(HQ/Branch)
     */
    private String rejectFromLevel;

    /**
     * 驳回人工号
     */
    private String rejectFromUser;

    /**
     * 驳回人姓名
     */
    private String rejectFromName;

    /**
     * 被驳回方层级(Branch/Dept)
     */
    private String rejectToLevel;

    /**
     * 被驳回人工号
     */
    private String rejectToUser;

    /**
     * 被驳回部门ID
     */
    private Long rejectToDeptId;

    /**
     * 被驳回部门名称
     */
    private String rejectToDeptName;

    /**
     * 驳回理由
     */
    private String rejectReason;

    /**
     * 截止时间
     */
    private Date deadlineTime;

    /**
     * 驳回时间
     */
    private Date rejectTime;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 处理耗时(小时)
     */
    private BigDecimal handleDurationHours;

    /**
     * 是否超时(0否1是)
     */
    private Integer isTimeout;
}
