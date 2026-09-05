package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 付款单对象 fee_payment（T5.2）
 * 必选项目，老板审批，通过后扣项目预算 + 记总支出
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_payment")
public class FeePayment extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 付款单号 PAY-YYYYMMDD-XXX
     */
    private String paymentNo;

    /**
     * 关联项目(必选)
     */
    private Long projectId;

    /**
     * 收款方(对方)
     */
    private String payee;

    /**
     * 付款方(我方)
     */
    private String payer;

    /**
     * 付款金额
     */
    private BigDecimal amount;

    /**
     * 付款类型(ENGINEERING/MATERIAL/SERVICE/RENT/OTHER)
     */
    private String payType;

    /**
     * 付款日期
     */
    private Date paymentDate;

    /**
     * 发票状态(HAS/NONE/PENDING)
     */
    private String invoiceStatus;

    /**
     * 发票附件
     */
    private String invoiceUrls;

    /**
     * 合同/验收单附件
     */
    private String contractUrls;

    /**
     * 说明
     */
    private String description;

    /**
     * 状态(PENDING/APPROVED/REJECTED)
     */
    private String status;

    /**
     * 审批人
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private Date approvedAt;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 部门ID(高级版预留)
     */
    private Long departmentId;

    /**
     * 组织ID(专业版预留)
     */
    private Long orgId;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
