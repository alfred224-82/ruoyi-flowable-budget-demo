package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格调整记录视图对象 price_change_log（T15.4/T15.11 改价历史留痕）
 * <p>{@code itemName} 为服务层回填的服务项目名（来自 service_item.name），便于改价记录列表直接展示，非表字段。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class PriceChangeLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 服务项目ID */
    private Long itemId;

    /** 服务项目名称（回填，非表字段） */
    private String itemName;

    /** 调整前单价 */
    private BigDecimal oldPrice;

    /** 调整后单价 */
    private BigDecimal newPrice;

    /** 操作人（老板/平台管理员） */
    private String operator;

    /** 操作人用户ID */
    private Long operatorId;

    /** 改价时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    /** 备注 */
    private String remark;
}
