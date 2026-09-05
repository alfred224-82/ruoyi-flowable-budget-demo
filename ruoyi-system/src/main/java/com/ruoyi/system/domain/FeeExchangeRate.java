package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 月度汇率对象 fee_exchange_rate
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_exchange_rate")
public class FeeExchangeRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 币种编码（USD/EUR/GBP/JPY）
     */
    private String currencyCode;

    /**
     * 币种名称
     */
    private String currencyName;

    /**
     * 汇率期间（YYYY-MM）
     */
    private String ratePeriod;

    /**
     * 兑人民币汇率
     */
    private BigDecimal rateToCny;

    /**
     * 来源（MANUAL/AUTO）
     */
    private String source;

    private Date createTime;
}
