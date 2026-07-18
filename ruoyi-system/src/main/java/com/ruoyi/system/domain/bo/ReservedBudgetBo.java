package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 预留预算业务对象 fee_reserved_budget
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservedBudgetBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 预算期间
     */
    @NotBlank(message = "预算期间不能为空", groups = {AddGroup.class})
    private String budgetPeriod;

    /**
     * 层级类型：DEPT/BRANCH/HQ
     */
    @NotBlank(message = "层级类型不能为空", groups = {AddGroup.class})
    private String levelType;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 预留预算总额度
     */
    @NotNull(message = "预留预算总额度不能为空", groups = {AddGroup.class})
    private BigDecimal totalAmount;

    /**
     * 预警阈值百分比
     */
    private BigDecimal warningThreshold;

    /**
     * 状态：ACTIVE/FROZEN/EXPIRED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
