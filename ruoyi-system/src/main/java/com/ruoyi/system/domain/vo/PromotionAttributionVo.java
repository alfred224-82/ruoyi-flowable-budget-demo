package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 推广归因视图对象 promotion_attribution（T16.6，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionAttributionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long customerUserId;
    private String customerPhone;
    private Long promoterUserId;
    /** 推广人昵称（回填，非表字段） */
    private String promoterName;
    private String promoterRoleCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastScanTime;

    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
