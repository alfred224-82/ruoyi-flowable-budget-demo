package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同台账对象 fee_contract_ledger
 * 设计文档 Phase 6 Task 6.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_contract_ledger")
public class FeeContractLedger extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 合同编号 */
    private String contractCode;

    /** 合同名称 */
    private String contractName;

    /** 合同类型（RENT/SERVICE/ANNUAL_FEE） */
    private String contractType;

    /** 归属部门ID */
    private Long deptId;

    /** 关联预算科目 */
    private String budgetSubjectCode;

    /** 合同总金额 */
    private BigDecimal totalAmount;

    /** 摊销月数 */
    private Integer amortizeMonths;

    /** 月摊销额 */
    private BigDecimal monthlyAmount;

    /** 合同起始日 */
    private Date startDate;

    /** 合同到期日 */
    private Date endDate;

    /** 累计已摊销 */
    private BigDecimal accumulatedAmount;

    /** 行业编码 */
    private String industryCode;

    /** 状态（ACTIVE/COMPLETED/TERMINATED） */
    private String status;
}
