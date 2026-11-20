package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动结算单主表 activity_settlement（M36 T36.1）
 * <p>状态机：DRAFT → SUBMITTED → LOCKED；提交后不可编辑，锁定后错误走调整单。
 * <p>代收类型（COLLECTED）不计入收入/利润/分账基数。
 * <p>员工成本由 activity_staff_snapshot 快照自动生成，不可手填。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_settlement")
public class ActivitySettlement extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** FK→promotion_activity.id */
    private Long activityId;

    /** 结算单号（提交时生成，如 ST202610190001） */
    private String settlementNo;

    /** 状态：DRAFT=草稿 / SUBMITTED=已提交 / LOCKED=已锁定 */
    private String status;

    /** 是否调整单（0=原单 1=调整单） */
    private Integer isAdjustment;

    /** 调整单原单ID（is_adjustment=1 时非空） */
    private Long originSettlementId;

    // ===== 收入汇总（后端自动计算） =====

    /** 票面收入合计（type=TICKET 汇总） */
    private BigDecimal ticketRevenue;

    /** 追加收入合计（type=ADDON 汇总） */
    private BigDecimal addonRevenue;

    /** 代收金额合计（type=COLLECTED 汇总，不计入利润） */
    private BigDecimal collectedAmount;

    /** 收入合计=ticket+addon（不含代收） */
    private BigDecimal totalRevenue;

    // ===== 成本汇总 =====

    /** 员工成本合计（快照自动生成，不可手填） */
    private BigDecimal employeeCost;

    /** 其他成本合计（可手填） */
    private BigDecimal otherCost;

    /** 成本合计=employee+other */
    private BigDecimal totalCost;

    // ===== 利润 =====

    /** 本场利润=total_revenue - total_cost */
    private BigDecimal profit;

    /** 预估利润（来自 M35.4 定价页快照） */
    private BigDecimal estimatedProfit;

    /** 利润差额=profit - estimated_profit */
    private BigDecimal profitDiff;

    // ===== 结算元数据 =====

    /** 结算操作人（sys_user.user_id） */
    private Long settledBy;

    /** 结算提交时间 */
    private Date settledTime;

    /** 结算锁定时间 */
    private Date lockedTime;

    /** 备注 */
    private String remark;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;
}
