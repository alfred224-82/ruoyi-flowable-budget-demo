package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 推广码视图对象 promotion_code（T16.1/T16.5，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionCodeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long userId;
    /** 推广人昵称（回填，非表字段） */
    private String userName;
    private String roleCode;
    private String codeType;
    private String sceneParam;
    private String qrCodeUrl;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
