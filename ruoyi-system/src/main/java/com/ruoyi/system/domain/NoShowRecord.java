package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 爽约明细对象 no_show_record（T15.31，小微版防爽约可追溯明细）
 * <p>每次判定爽约（no_show）写一行，记录订单编号/原预约时间/标记来源；
 * 租户级表（含 tenant_id），多租户插件自动隔离，跨租户互不影响。
 * <p>★不继承 BaseEntity：本表仅含 create_time（无 create_by/update_by/update_time），
 * tenant_id 与 create_time 由 {@code @TableField(fill = INSERT)} 自动填充。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
@TableName("no_show_record")
public class NoShowRecord implements Serializable {

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
     * 顾客用户ID
     */
    private Long userId;

    /**
     * 爽约订单编号（booking_order.order_no）
     */
    private String orderNo;

    /**
     * 原预约到店时间
     */
    private Date bookTime;

    /**
     * 标记爽约时间
     */
    private Date markedAt;

    /**
     * 标记来源：system(定时任务自动)/boss(老板手动)
     */
    private String markedBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 备注
     */
    private String remark;
}
