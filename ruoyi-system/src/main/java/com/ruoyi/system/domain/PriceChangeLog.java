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
 * 价格调整记录对象 price_change_log（T15.4/T15.11 改价历史留痕）
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；改价时由服务层自动写入。
 * <p>表仅含 change_time（无 create_by/create_time/update_by/update_time/del_flag），故不继承 BaseEntity；
 * tenantId 标注 {@code @TableField(fill = INSERT)}，change_time 由服务层显式设置或 DB DEFAULT 兜底。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@TableName("price_change_log")
public class PriceChangeLog implements Serializable {

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
     * 服务项目ID（FK→service_item）
     */
    private Long itemId;

    /**
     * 调整前单价
     */
    private BigDecimal oldPrice;

    /**
     * 调整后单价
     */
    private BigDecimal newPrice;

    /**
     * 操作人（老板/平台管理员）
     */
    private String operator;

    /**
     * 操作人用户ID
     */
    private Long operatorId;

    /**
     * 改价时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date changeTime;

    /**
     * 备注
     */
    private String remark;
}
