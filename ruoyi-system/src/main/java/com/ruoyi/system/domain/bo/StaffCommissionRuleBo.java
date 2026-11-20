package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 员工分成规则业务对象 staff_commission_rule（M35 T35.2）
 * <p>老板 PC 端新增/编辑员工分成规则；三种类型校验：
 * <ul>
 *   <li>FIXED_AMOUNT: fixedAmount > 0</li>
 *   <li>PERCENTAGE:   percentageRate 0~100</li>
 *   <li>TIERED:       tieredConfig JSON 结构验证（本期预留）</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class StaffCommissionRuleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "分成规则ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 员工用户ID */
    @NotNull(message = "员工不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long staffId;

    /** 分成类型：FIXED_AMOUNT/PERCENTAGE/TIERED */
    @NotBlank(message = "分成类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String commissionType;

    /** 固定金额（元/场），仅 FIXED_AMOUNT 使用 */
    @DecimalMin(value = "0", message = "固定金额不能小于 0", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal fixedAmount;

    /** 百分比（0~100），仅 PERCENTAGE 使用 */
    @DecimalMin(value = "0", message = "百分比不能小于 0", groups = {AddGroup.class, EditGroup.class})
    @DecimalMax(value = "100", message = "百分比不能大于 100", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal percentageRate;

    /** 阶梯规则 JSON（TIERED 预留） */
    private String tieredConfig;

    /** 追加消费是否计提成：0=否 1=是（默认关） */
    private Integer includeAddonInCommission;

    /** 月底薪（进固定成本，不进单场） */
    @DecimalMin(value = "0", message = "月底薪不能小于 0", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal baseSalary;

    /** 是否启用：0=停用 1=启用 */
    private Integer isActive;

    /** 备注 */
    private String remark;

    /** 员工姓名（查询过滤用，非表字段） */
    private String staffName;
}
