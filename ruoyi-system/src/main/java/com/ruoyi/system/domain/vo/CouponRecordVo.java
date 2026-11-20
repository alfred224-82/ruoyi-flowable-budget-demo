package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 顾客券记录视图对象 coupon_record（T16.3，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class CouponRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long userId;
    /** 持券人昵称（回填，非表字段） */
    private String userName;
    private Long couponId;
    /** 优惠券名称（回填，非表字段） */
    private String couponName;
    private String source;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date usedTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
