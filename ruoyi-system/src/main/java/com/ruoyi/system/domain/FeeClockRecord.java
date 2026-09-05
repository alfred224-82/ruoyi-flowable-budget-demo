package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 打卡记录对象 fee_clock_record（T6.2 / FEAT_TIME_CLOCK）
 * 定位存疑仅标记不拦截（ADR-014 威慑非定罪），证据等级 medium
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_clock_record")
public class FeeClockRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联报销单
     */
    private Long billId;

    /**
     * 打卡用户
     */
    private Long userId;

    /**
     * 触发规则(TRAVEL_CATEGORY/AMOUNT_THRESHOLD/HISTORY_ABNORMAL)
     */
    private String triggerRule;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 定位地址
     */
    private String address;

    /**
     * 定位精度(米)
     */
    private BigDecimal accuracy;

    /**
     * 现场照片
     */
    private String photoUrls;

    /**
     * 打卡时间
     */
    private Date clockTime;

    /**
     * 定位存疑(精度>50米，仅标记不拦截)
     */
    private Integer isSuspicious;

    /**
     * 证据等级 strong/medium/weak
     */
    private String evidenceLevel;
}
