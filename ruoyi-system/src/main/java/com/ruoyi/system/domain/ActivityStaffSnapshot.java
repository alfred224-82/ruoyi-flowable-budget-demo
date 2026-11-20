package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 活动员工分成快照 activity_staff_snapshot（M35 T35.1，历史账不可变）
 * <p>活动创建/编辑选中员工时按当前规则生成快照；规则变更后历史快照不变。
 * <p>M36 T36.1 结算单员工成本行由本表 calculated_amount 自动生成，不可手填。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_staff_snapshot")
public class ActivityStaffSnapshot extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 员工用户ID（sys_user.user_id） */
    private Long staffId;

    /** 快照时的规则类型：FIXED_AMOUNT/PERCENTAGE/TIERED */
    private String ruleType;

    /** 快照时的规则值 JSON（含 fixedAmount/percentageRate/includeAddon 等） */
    private String snapshotValue;

    /** 按实际人数/收入计算的应发金额（结算时回写） */
    private BigDecimal calculatedAmount;
}
