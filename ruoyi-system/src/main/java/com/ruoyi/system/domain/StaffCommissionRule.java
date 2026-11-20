package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 员工分成规则对象 staff_commission_rule（M35 T35.1，设计方案 §14.2）
 * <p>挂在员工档案上，三种类型：
 * <ul>
 *   <li>FIXED_AMOUNT 固定金额（元/场）</li>
 *   <li>PERCENTAGE   按比例（0~100%）</li>
 *   <li>TIERED       阶梯/按人头（结构预留，本期不做）</li>
 * </ul>
 * <p>追加消费是否计提成 include_addon_in_commission（默认关）；月底薪 base_salary
 * 进 M38 月分摊固定成本，不进单场。
 * <p>规则变更后历史活动仍用 {@link ActivityStaffSnapshot} 快照，历史账不可变。
 * <p>租户级表（含 tenant_id 由 {@link TenantEntity} 自动填充），
 * UNIQUE(tenant_id, staff_id) 保证每员工一条规则。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_commission_rule")
public class StaffCommissionRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 固定金额类型 */
    public static final String TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";
    /** 按比例类型 */
    public static final String TYPE_PERCENTAGE = "PERCENTAGE";
    /** 阶梯类型（预留） */
    public static final String TYPE_TIERED = "TIERED";

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工用户ID（FK→sys_user.user_id） */
    private Long staffId;

    /** 分成类型：FIXED_AMOUNT/PERCENTAGE/TIERED */
    private String commissionType;

    /** 固定金额（元/场），仅 FIXED_AMOUNT 使用 */
    private BigDecimal fixedAmount;

    /** 百分比（0~100），仅 PERCENTAGE 使用 */
    private BigDecimal percentageRate;

    /** 阶梯规则 JSON（TIERED 预留） */
    private String tieredConfig;

    /** 追加消费是否计提成：0=否 1=是（默认关） */
    private Integer includeAddonInCommission;

    /** 月底薪（进固定成本，不进单场） */
    private BigDecimal baseSalary;

    /** 是否启用：0=停用 1=启用 */
    private Integer isActive;

    /** 备注 */
    private String remark;
}
