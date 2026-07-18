package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预留预算分摊记录对象 fee_reserved_allocation_log
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@TableName("fee_reserved_allocation_log")
public class ReservedAllocationLog {

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联预留预算ID
     */
    private Long reservedId;

    /**
     * 关联预算调整申请ID
     */
    private Long applicationId;

    /**
     * 关联申请单号
     */
    private String appNo;

    /**
     * 目标预算科目编码
     */
    private String targetSubject;

    /**
     * 目标预算科目名称
     */
    private String targetSubjectName;

    /**
     * 分摊金额
     */
    private BigDecimal allocateAmount;

    /**
     * 分摊前科目可用余额
     */
    private BigDecimal beforeAvailable;

    /**
     * 分摊后科目可用余额
     */
    private BigDecimal afterAvailable;

    /**
     * 分摊前预留剩余额度
     */
    private BigDecimal beforeReservedAvailable;

    /**
     * 分摊后预留剩余额度
     */
    private BigDecimal afterReservedAvailable;

    /**
     * 操作人ID（审批人）
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
