package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 每日营收快照对象 micro_daily_revenue（T15.5/T15.14/T15.22，microDailyRevenueJob 每日生成）
 * <p>租户级表（含 tenant_id），uk_tenant_date 唯一保证按日期幂等 upsert。
 * <p>表【无 del_flag/create_by/update_by】，仅 create_time/update_time/snapshot_time，故不继承 BaseEntity；
 * tenantId 标注 {@code @TableField(fill = INSERT)}（跨租户 Job 场景由服务层显式 setTenantId），时间列依赖 DB DEFAULT 兜底。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@TableName("micro_daily_revenue")
public class MicroDailyRevenue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 营收日期
     */
    private Date revenueDate;

    /**
     * 当日总收入
     */
    private BigDecimal grossIncome;

    /**
     * 当日总支出
     */
    private BigDecimal totalExpense;

    /**
     * 当日净利润（收入-支出）
     */
    private BigDecimal netProfit;

    /**
     * 当日订单数
     */
    private Integer orderCount;

    /**
     * 快照生成时间
     */
    private Date snapshotTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
