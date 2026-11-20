package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 助教价格视图对象 staff_pricing（T21.7，支持 4 种计价模式）
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
public class StaffPricingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 助教用户ID */
    private Long userId;

    /** 助教昵称（回填，非表字段） */
    private String nickName;

    /** 助教头像（回填，非表字段） */
    private String avatar;

    /** ★基准价（元）—— 计时=时薪，套餐=基础价，计量=单价，一口价=固定价 */
    private BigDecimal hourlyRate;

    /** 计价模式（duration/package/times/fixed） */
    private String pricingType;

    /** 扩展配置JSON（套餐/一口价的含时长、超时费等） */
    private String extraConfig;

    /** 是否启用（0停用 1启用） */
    private Integer enabled;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
