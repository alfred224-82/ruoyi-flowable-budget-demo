package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 费用管控操作日志对象 fee_operation_log
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@TableName("fee_operation_log")
public class FeeOperationLog {

    /**
     * 主键ID
     */
    @TableId(value = "log_id")
    private Long logId;

    /**
     * 所属模块：EXPENSE/OVER_BUDGET/BUDGET_ADJUST
     */
    private String module;

    /**
     * 操作类型：SUBMIT/APPROVE/REJECT/RESUBMIT/DELETE/OB_APPROVE/OB_REJECT/OB_ADJUST/BA_APPLY/BA_ALLOCATE/BA_REJECT/BA_ESCALATE
     */
    private String operationType;

    /**
     * 关联报销单ID
     */
    private Long billId;

    /**
     * 报销单编号
     */
    private String billNo;

    /**
     * 关联预算调整申请ID
     */
    private Long relatedAppId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 操作前状态
     */
    private String beforeStatus;

    /**
     * 操作后状态
     */
    private String afterStatus;

    /**
     * 操作详情（JSON格式）
     */
    private String operationDetail;

    /**
     * 操作人IP地址
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    private Date operationTime;
}
