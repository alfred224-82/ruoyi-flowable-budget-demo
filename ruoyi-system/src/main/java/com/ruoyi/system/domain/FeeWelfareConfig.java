package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 公司福利政策配置对象 fee_welfare_config
 * 设计文档 Phase 17 Task 17.1
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_welfare_config")
public class FeeWelfareConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 福利编码（租户内唯一）
     */
    private String welfareCode;

    /**
     * 福利名称
     */
    private String welfareName;

    /**
     * 福利大类：MEAL/TRAVEL/TRANSPORT/OTHER
     */
    private String category;

    /**
     * 福利子类：WORKDAY_MEAL/OVERTIME_MEAL/TRAVEL_MEAL/SHORT_TRIP/LONG_TRIP/CITY_TRANSPORT/INTER_CITY/COMMUNICATION/HOUSING/CUSTOM
     */
    private String subCategory;

    /**
     * 适用区域：DOMESTIC/INTERNATIONAL/ALL
     */
    private String region;

    /**
     * 出差时长类型：SHORT/LONG/ANY
     */
    private String tripDuration;

    /**
     * 城市等级：TIER1/TIER2/TIER3/ANY（仅国内）
     */
    private String cityLevel;

    /**
     * 国家分组：HK_MO_TW/ASIA/EUROPE_AMERICA/OTHER/ANY（仅国外）
     */
    private String countryGroup;

    /**
     * 补助标准金额
     */
    private BigDecimal standardAmount;

    /**
     * 计量单位：DAY/MONTH/TIME/MEAL
     */
    private String unit;

    /**
     * 最大享受天数/次数（0=不限）
     */
    private Integer maxLimit;

    /**
     * 适用职级：ALL/指定职级编码
     */
    private String applicableRank;

    /**
     * 生效日期
     */
    private Date effectiveDate;

    /**
     * 失效日期
     */
    private Date expiryDate;

    /**
     * 状态：ENABLED/DISABLED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;
}
