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
 * 收款单对象 fee_receipt（T5.3）
 * 必选项目，财务审核，联动生成"已核验"收入确认
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_receipt")
public class FeeReceipt extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 收款单号 RCP-YYYYMMDD-XXX
     */
    private String receiptNo;

    /**
     * 关联项目(必选)
     */
    private Long projectId;

    /**
     * 付款方(对方)
     */
    private String payer;

    /**
     * 收款金额
     */
    private BigDecimal amount;

    /**
     * 收款类型(PROJECT_PAYMENT/REFUND/OTHER)
     */
    private String receiptType;

    /**
     * 收款日期
     */
    private Date receivedDate;

    /**
     * 回单附件
     */
    private String proofUrls;

    /**
     * 状态(PENDING/APPROVED/REJECTED)
     */
    private String status;

    /**
     * 审核人
     */
    private Long approvedBy;

    /**
     * 审核时间
     */
    private Date approvedAt;

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
