package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券视图对象 coupon（T16.3，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class CouponVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String name;
    private String type;
    private BigDecimal value;
    private Integer validDays;
    private Integer total;
    private Integer issuedCount;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
