package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收支流水对象 cash_flow（T15.5/T15.14，纯记账口径，非支付流水）
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；flow_type：income(收入)/expense(支出)。
 * <p>表含 create_by/create_time/update_time 但【无 update_by】，故不继承 BaseEntity（避免 insertFill 写入不存在的 update_by 列）；
 * tenantId 标注 {@code @TableField(fill = INSERT)} 由登录上下文自动填充，create_time/update_time 依赖 DB DEFAULT/ON UPDATE 兜底，
 * create_by（手动补录人）由服务层显式设置。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@TableName("cash_flow")
public class CashFlow implements Serializable {

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
     * 流水类型：income(收入)/expense(支出)
     */
    private String flowType;

    /**
     * 收支分类（订单收入/工资结算/房租/水电/采购...）
     */
    private String category;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 关联单据ID（booking_order/staff_settlement）
     */
    private Long relatedOrderId;

    /**
     * 关联单据类型（booking/settlement/manual）
     */
    private String relatedType;

    /**
     * 发生日期
     */
    private Date occurDate;

    /**
     * 创建者（手动补录人）
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

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
