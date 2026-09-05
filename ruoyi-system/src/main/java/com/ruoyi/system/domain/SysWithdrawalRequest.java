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
 * 提现申请对象 sys_withdrawal_request（T14.3 提现申请与人工审核）
 * <p>表含 create_by/create_time/update_time（无 update_by），故不继承 BaseEntity。
 * <p>状态机：PENDING(申请,冻结余额) → APPROVED/PAID(通过,扣减余额) / REJECTED(驳回,解冻)。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_withdrawal_request")
public class SysWithdrawalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 提现单号 */
    private String withdrawNo;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 提现金额 */
    private BigDecimal amount;

    /** 收款账户信息(脱敏) */
    private String accountInfo;

    /** 状态：PENDING/APPROVED/REJECTED/PAID */
    private String status;

    /** 审核人 */
    private String auditBy;

    /** 审核时间 */
    private Date auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 申请人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 申请时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
