package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 收入确认对象 fee_income_confirmation（T5.4 / ADR-021）
 * 三态：PENDING待核验 / VERIFIED已核验 / REJECTED已驳回
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_income_confirmation")
public class FeeIncomeConfirmation extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 确认收入金额
     */
    private BigDecimal confirmedAmount;

    /**
     * 确认日期
     */
    private Date confirmDate;

    /**
     * 依据类型(CONTRACT/INVOICE/RECEIPT/OTHER)
     */
    private String basisType;

    /**
     * 依据编号
     */
    private String basisNo;

    /**
     * 依据附件
     */
    private String basisUrls;

    /**
     * 创建人(项目经理/财务)
     */
    private Long confirmedBy;

    /**
     * 创建人姓名
     */
    private String confirmedByName;

    /**
     * 核验状态(PENDING待核验/VERIFIED已核验/REJECTED已驳回)
     */
    private String verifyStatus;

    /**
     * 核验人(财务)
     */
    private Long verifyBy;

    /**
     * 核验时间
     */
    private Date verifyAt;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 组织ID(专业版预留)
     */
    private Long orgId;
}
