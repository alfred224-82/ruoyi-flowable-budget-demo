package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 助教小时价格设置 staff_pricing（T21.7，小微版）
 * <p>Boss 可随时设置/修改助教小时单价，顾客在自助预约选择助教时可见。
 * <p>★房间价格复用 service_item.price（已有字段，无需新建表）。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。UNIQUE(tenant_id, user_id) 防重复。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_pricing")
public class StaffPricing extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 助教用户ID（FK→sys_user.user_id）
     */
    private Long userId;

    /**
     * ★小时单价（元/小时）—— 兼作基准价（预约计费兼容）
     */
    private BigDecimal hourlyRate;

    /**
     * 计价模式（duration计时/package套餐/times计量/fixed一口价）
     */
    private String pricingType;

    /**
     * 扩展配置JSON（套餐/一口价的含时长、超时费等）
     */
    private String extraConfig;

    /**
     * 是否启用（0停用 1启用）
     */
    private Integer enabled;

    /**
     * 备注（如：资深助教/特价等）
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
