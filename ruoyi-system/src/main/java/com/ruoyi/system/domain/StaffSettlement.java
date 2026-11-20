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
 * 人员结算对象 staff_settlement（T15.5/T15.13，小微版）
 * <p>★V1 零支付合规：只记账不打款；pay_status 仅 PENDING(待发放)/PAID_OFFLINE(已线下发放)，
 * 老板线下转账后点「标记已线下发放」→改状态 + 写 cash_flow 支出流水，不调任何支付/转账接口。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_settlement")
public class StaffSettlement extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 结算单号
     */
    private String settleNo;

    /**
     * 被结算员工/助教用户ID
     */
    private Long staffUserId;

    /**
     * 员工姓名快照
     */
    private String staffName;

    /**
     * 结算周期起
     */
    private Date periodStart;

    /**
     * 结算周期止
     */
    private Date periodEnd;

    /**
     * 周期内完成单量
     */
    private Integer totalOrders;

    /**
     * 应发金额（自动汇总算出）
     */
    private BigDecimal totalAmount;

    /**
     * 发放状态：PENDING(待发放)/PAID_OFFLINE(已线下发放)；★仅标记不调支付接口
     */
    private String payStatus;

    /**
     * 标记人（老板线下转账后点此）
     */
    private String markedBy;

    /**
     * 标记人用户ID
     */
    private Long markedById;

    /**
     * 标记已线下发放时间
     */
    private Date markedTime;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
