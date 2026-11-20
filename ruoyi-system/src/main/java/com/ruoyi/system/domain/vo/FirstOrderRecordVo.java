package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 首单识别记录视图对象 first_order_record（T16.4/T16.8，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class FirstOrderRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long activityId;
    /** 活动名称（回填，非表字段） */
    private String activityName;
    private String customerPhone;
    private Long customerUserId;
    private Long firstOrderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date consumeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
