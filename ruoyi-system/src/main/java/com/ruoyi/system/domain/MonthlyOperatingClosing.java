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
 * 月度经营月结对象 monthly_operating_closing（M38 T38.4）
 * <p>承载每月税费（手填或按率估算）+ 月结锁定状态；锁定后禁止改税费。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；UNIQUE(tenant_id, month)。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("monthly_operating_closing")
public class MonthlyOperatingClosing extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 月份（yyyy-MM，如 2026-10） */
    private String month;

    /** 税费金额（元） */
    private BigDecimal taxAmount;

    /** 税率（%，如 3 表示 3%；手填税费时可为空） */
    private BigDecimal taxRate;

    /** 计税方式：MANUAL=手填 / RATE=按率估算 */
    private String taxMode;

    /** 月结状态：OPEN=未月结 / LOCKED=已锁定 */
    private String status;

    /** 锁定时间 */
    private Date lockTime;

    /** 锁定操作人（sys_user.user_id） */
    private Long lockBy;

    /** 备注 */
    private String remark;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;
}
