package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 盈利提示配置对象 fee_profit_tip_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_profit_tip_config")
public class FeeProfitTipConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 科目类型（INCOME/EXPENSE/ASSET/LIABILITY）
     */
    private String subjectType;

    /**
     * 提示类型（INCREASE/DECREASE/NEUTRAL）
     */
    private String tipType;

    /**
     * 提示文案
     */
    private String tipText;

    /**
     * 文字颜色
     */
    private String tipColor;

    /**
     * 行业编码
     */
    private String industryCode;

    /**
     * 大额支出阈值（超则弹窗）
     */
    private BigDecimal largeAmountThreshold;

    /**
     * 是否强制弹窗（0否1是）
     */
    private Integer forcePopup;

    /**
     * 状态
     */
    private String status;

    private Date createTime;
    private Date updateTime;
}
