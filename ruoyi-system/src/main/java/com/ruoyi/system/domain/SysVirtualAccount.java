package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 租户虚拟账户对象 sys_virtual_account（T14.3 平台代收分账入账/提现）
 * <p>每租户唯一（uk_tenant）；表仅含 create_time/update_time + 乐观锁 version，故不继承 BaseEntity。
 * <p>余额变更走乐观锁（@Version）防并发覆盖；提现审核中金额入 frozen_balance。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_virtual_account")
public class SysVirtualAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 可用余额(元) */
    private BigDecimal balance;

    /** 冻结余额(提现审核中) */
    private BigDecimal frozenBalance;

    /** 累计入账 */
    private BigDecimal totalIncome;

    /** 累计提现 */
    private BigDecimal totalWithdrawn;

    /** 状态（0正常 1冻结） */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
